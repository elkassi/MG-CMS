#!/usr/bin/env python3
"""
Scheduling Optimizer for MG-CMS
Advanced scheduling optimization with continuous iteration cycles.

Features:
- Optimizes to minimize Max(sequence_duration / sequence_boxes) 
- Iterates over SERIES (not sequences) to find optimal machine assignment
- Fixed series (statusCoupe='In progress' with dateDebutCoupe) keep their position
- End dates calculated by adding cutting duration from timing placement
- No gaps, no overlaps on machines - series are placed sequentially
- Runs continuously finding better solutions over millions of iterations
- Can be stopped/started via signals

Usage:
    python optimizer.py --input input.json [--timeout 30] [--continuous]

Input JSON format:
{
    "sequences": [{"id": "SEQ001", "status": "IN_PROGRESS", "boxCount": 12, "series": [...]}],
    "machines": ["AA1", "AA2", "AA3", "AA4"],
    "machineTypes": {"AA1": "Lectra", "AA2": "Lectra IP6", ...},
    "lockedSeries": ["SERIE001", "SERIE002"],
    "maxBoxes": 64,
    "params": {
        "tolerancePercent": 10,
        "maxIterations": 1000000,
        "timeoutSeconds": 30,
        "prioritizeSequenceCompletion": true,
        "groupByMaterial": true,
        "priority": "balanced",
        "continuous": true,
        "cycleSeconds": 30
    }
}
"""

import json
import sys
import argparse
import random
import time
import signal
import os
from datetime import datetime, timedelta
from typing import List, Dict, Tuple, Optional, Set
from dataclasses import dataclass, field
from copy import deepcopy
from threading import Event


# Global stop event for graceful shutdown
stop_event = Event()


def signal_handler(signum, frame):
    """Handle stop signal"""
    stop_event.set()


signal.signal(signal.SIGTERM, signal_handler)
signal.signal(signal.SIGINT, signal_handler)


@dataclass
class Serie:
    id: str
    sequence_id: str
    placement: str
    part_number_material: str
    machine_type: str
    cutting_time_minutes: float
    is_locked: bool = False
    is_fixed: bool = False  # True if serie is in progress with fixed start date
    status: str = "WAITING"  # WAITING, IN_PROGRESS, COMPLETE
    assigned_machine: Optional[str] = None
    scheduled_start: Optional[datetime] = None
    scheduled_end: Optional[datetime] = None
    original_start: Optional[datetime] = None  # For fixed series


@dataclass
class Sequence:
    id: str
    status: str = "NOT_STARTED"  # NOT_STARTED, IN_PROGRESS, FINISHED
    box_count: int = 0
    series: List[Serie] = field(default_factory=list)
    
    @property
    def min_start(self) -> Optional[datetime]:
        starts = [s.scheduled_start for s in self.series if s.scheduled_start]
        return min(starts) if starts else None
    
    @property
    def max_end(self) -> Optional[datetime]:
        ends = [s.scheduled_end for s in self.series if s.scheduled_end]
        return max(ends) if ends else None
    
    @property
    def duration_minutes(self) -> float:
        if self.min_start and self.max_end:
            return (self.max_end - self.min_start).total_seconds() / 60
        return 0
    
    @property
    def duration_per_box(self) -> float:
        """Key metric: duration / number of boxes - lower is better"""
        if self.box_count > 0 and self.duration_minutes > 0:
            return self.duration_minutes / self.box_count
        return float('inf')


@dataclass
class Machine:
    name: str
    machine_type: str
    slots: List[Tuple[datetime, datetime, str]] = field(default_factory=list)
    
    def get_next_available_slot(self, now: datetime) -> datetime:
        """Get the next available time after all current slots"""
        if not self.slots:
            return now
        # Sort slots by end time
        sorted_slots = sorted(self.slots, key=lambda x: x[1])
        return max(now, sorted_slots[-1][1])
    
    def get_available_after(self, after_time: datetime) -> datetime:
        """Get available time that starts after a given time (no overlap)"""
        if not self.slots:
            return after_time
        # Find the latest end that might conflict
        for start, end, _ in sorted(self.slots, key=lambda x: x[0]):
            if after_time < end and after_time >= start:
                after_time = end
        return after_time
    
    def has_conflict(self, start: datetime, end: datetime) -> bool:
        """Check if a time range conflicts with existing slots"""
        for slot_start, slot_end, _ in self.slots:
            # Check for overlap
            if start < slot_end and end > slot_start:
                return True
        return False
    
    def reset(self):
        self.slots = []
    
    def add_slot(self, start: datetime, end: datetime, serie_id: str):
        self.slots.append((start, end, serie_id))


class SchedulingOptimizer:
    def __init__(self, data: dict, continuous: bool = False, cycle_seconds: int = 30):
        self.data = data
        self.continuous = continuous
        self.cycle_seconds = cycle_seconds
        self.start_time = time.time()
        
        # Best solution tracking
        self.best_solution = None
        self.best_score = float('inf')
        self.best_score_detail = {}
        self.iteration_count = 0
        self.cycle_count = 0
        self.last_improvement_cycle = 0
        self.total_series_count = 0
        
        # Parse input
        self.sequences = self._parse_sequences()
        self.machines = self._parse_machines()
        self.locked_series_ids = set(data.get('lockedSeries', []))
        self.max_boxes = data.get('maxBoxes', 64)
        self.params = data.get('params', {})
        
        # Separate series by status
        self.fixed_series = []  # In progress with dateDebutCoupe
        self.schedulable_series = []  # Waiting series to be scheduled
        self.completed_series = []  # Already complete
        
        for seq in self.sequences:
            for serie in seq.series:
                if serie.is_fixed:
                    self.fixed_series.append(serie)
                elif serie.status == "COMPLETE":
                    self.completed_series.append(serie)
                elif not serie.is_locked:
                    self.schedulable_series.append(serie)
        
        self.total_series_count = len(self.fixed_series) + len(self.schedulable_series)
        
        # Calculate current box usage
        in_progress_sequences = [s for s in self.sequences if s.status == "IN_PROGRESS"]
        self.current_boxes_used = sum(seq.box_count for seq in in_progress_sequences)

    def _parse_sequences(self) -> List[Sequence]:
        sequences = []
        for seq_data in self.data.get('sequences', []):
            seq = Sequence(
                id=seq_data['id'],
                status=seq_data.get('status', 'NOT_STARTED'),
                box_count=seq_data.get('boxCount', len(seq_data.get('series', [])))
            )
            for s_data in seq_data.get('series', []):
                # Check if this serie is fixed (In progress with start date)
                is_fixed = False
                original_start = None
                status = s_data.get('status', 'WAITING')
                date_debut = s_data.get('dateDebutCoupe')
                
                if status == 'In progress' and date_debut:
                    is_fixed = True
                    try:
                        original_start = datetime.fromisoformat(date_debut.replace(' ', 'T'))
                    except:
                        try:
                            original_start = datetime.strptime(date_debut, '%Y-%m-%d %H:%M:%S')
                        except:
                            is_fixed = False
                
                serie = Serie(
                    id=s_data['id'],
                    sequence_id=seq_data['id'],
                    placement=s_data.get('placement', ''),
                    part_number_material=s_data.get('partNumberMaterial', ''),
                    machine_type=s_data.get('machineType', 'Lectra'),
                    cutting_time_minutes=s_data.get('cuttingTimeMinutes', 0),
                    is_locked=s_data.get('isLocked', False),
                    is_fixed=is_fixed,
                    status=status,
                    assigned_machine=s_data.get('tableCoupe') or s_data.get('machineAssigned'),
                    original_start=original_start
                )
                
                # Override locked status if in locked list
                if serie.id in self.data.get('lockedSeries', []):
                    serie.is_locked = True
                    
                seq.series.append(serie)
            sequences.append(seq)
        return sequences

    def _parse_machines(self) -> Dict[str, Machine]:
        machines = {}
        machine_types = self.data.get('machineTypes', {})
        for machine_name in self.data.get('machines', []):
            machines[machine_name] = Machine(
                name=machine_name,
                machine_type=machine_types.get(machine_name, 'Lectra')
            )
        return machines

    def _get_candidate_machines(self, serie: Serie) -> List[Machine]:
        """Get machines compatible with the serie's requirements"""
        candidates = []
        placement = serie.placement.upper() if serie.placement else ''
        
        # Special handling for -0BF placements
        if '-0BF' in placement:
            if serie.part_number_material == '100132940':
                target_type = 'Lectra IP6'
            else:
                target_type = 'Lectra'
        else:
            target_type = serie.machine_type or 'Lectra'
        
        for machine in self.machines.values():
            if machine.machine_type == target_type:
                candidates.append(machine)
        
        # Fallback to all machines if no candidates found
        if not candidates:
            candidates = list(self.machines.values())
        
        return candidates

    def _reset_machines(self):
        """Reset all machine slots"""
        for machine in self.machines.values():
            machine.reset()

    def _setup_fixed_series(self):
        """Setup machine slots for fixed series (in progress with dates)"""
        for serie in self.fixed_series:
            if serie.original_start and serie.assigned_machine:
                machine = self.machines.get(serie.assigned_machine)
                if machine:
                    start = serie.original_start
                    end = start + timedelta(minutes=serie.cutting_time_minutes)
                    machine.add_slot(start, end, serie.id)
                    serie.scheduled_start = start
                    serie.scheduled_end = end

    def _evaluate_solution(self, assignments: List[Tuple[Serie, Machine, datetime, datetime]]) -> Tuple[float, Dict]:
        """
        Evaluate a solution using: Max(sequence_duration / sequence_boxes)
        Lower score is better - prioritizes finishing boxes faster.
        """
        # Build sequence summaries
        seq_data = {}
        
        # Include fixed series
        for serie in self.fixed_series:
            if serie.scheduled_start and serie.scheduled_end:
                if serie.sequence_id not in seq_data:
                    seq = next((s for s in self.sequences if s.id == serie.sequence_id), None)
                    seq_data[serie.sequence_id] = {
                        'starts': [],
                        'ends': [],
                        'box_count': seq.box_count if seq else 1
                    }
                seq_data[serie.sequence_id]['starts'].append(serie.scheduled_start)
                seq_data[serie.sequence_id]['ends'].append(serie.scheduled_end)
        
        # Include new assignments
        for serie, machine, start, end in assignments:
            if serie.sequence_id not in seq_data:
                seq = next((s for s in self.sequences if s.id == serie.sequence_id), None)
                seq_data[serie.sequence_id] = {
                    'starts': [],
                    'ends': [],
                    'box_count': seq.box_count if seq else 1
                }
            seq_data[serie.sequence_id]['starts'].append(start)
            seq_data[serie.sequence_id]['ends'].append(end)
        
        # Calculate duration/boxes for each sequence
        max_ratio = 0
        max_duration = 0
        total_waiting_time = 0
        ratios = {}
        
        for seq_id, data in seq_data.items():
            if data['starts'] and data['ends']:
                duration = (max(data['ends']) - min(data['starts'])).total_seconds() / 60
                box_count = max(1, data['box_count'])
                ratio = duration / box_count
                ratios[seq_id] = {
                    'duration_min': duration,
                    'boxes': box_count,
                    'ratio': ratio
                }
                max_ratio = max(max_ratio, ratio)
                max_duration = max(max_duration, duration)
        
        detail = {
            'max_duration_per_box': max_ratio,
            'max_duration_minutes': max_duration,
            'sequence_ratios': ratios,
            'series_count': len(assignments) + len(self.fixed_series)
        }
        
        return max_ratio, detail

    def _schedule_series(self, series_order: List[Serie]) -> List[Tuple[Serie, Machine, datetime, datetime]]:
        """
        Schedule series in order, placing each serie on the earliest available machine.
        No gaps, no overlaps - series are placed sequentially on each machine.
        """
        assignments = []
        now = datetime.now()
        
        # Reset and setup fixed series
        self._reset_machines()
        self._setup_fixed_series()
        
        for serie in series_order:
            if serie.is_fixed or serie.is_locked or serie.id in self.locked_series_ids:
                continue
            if serie.status == 'COMPLETE':
                continue
            
            candidates = self._get_candidate_machines(serie)
            if not candidates:
                continue
            
            # Find best machine (earliest available with no conflict)
            best_machine = None
            best_start = None
            best_end = None
            
            for machine in candidates:
                # Get next available time for this machine
                start = machine.get_next_available_slot(now)
                end = start + timedelta(minutes=serie.cutting_time_minutes)
                
                # Ensure no conflict
                while machine.has_conflict(start, end):
                    start = machine.get_available_after(start)
                    end = start + timedelta(minutes=serie.cutting_time_minutes)
                
                if best_end is None or end < best_end:
                    best_machine = machine
                    best_start = start
                    best_end = end
            
            if best_machine:
                best_machine.add_slot(best_start, best_end, serie.id)
                assignments.append((serie, best_machine, best_start, best_end))
                serie.assigned_machine = best_machine.name
                serie.scheduled_start = best_start
                serie.scheduled_end = best_end
        
        return assignments

    def _genetic_optimize_cycle(self, max_iterations: int = 100000) -> Tuple[List[Tuple[Serie, Machine, datetime, datetime]], float, Dict]:
        """Run one optimization cycle using genetic algorithm"""
        
        if not self.schedulable_series:
            # Only fixed series, just evaluate them
            assignments = []
            self._reset_machines()
            self._setup_fixed_series()
            score, detail = self._evaluate_solution(assignments)
            return assignments, score, detail
        
        # Initialize population with different orderings
        population_size = 50
        population = []
        
        base_series = list(self.schedulable_series)
        
        # Add priority ordering (by sequence, then by placement)
        priority_ordered = sorted(base_series, key=lambda s: (s.sequence_id, s.placement or ''))
        population.append(priority_ordered)
        
        # Add material-grouped ordering
        material_grouped = sorted(base_series, key=lambda s: (s.part_number_material or '', s.sequence_id))
        population.append(material_grouped)
        
        # Add sequence-grouped ordering
        seq_grouped = sorted(base_series, key=lambda s: s.sequence_id)
        population.append(seq_grouped)
        
        # Add random orderings
        for _ in range(population_size - 3):
            order = base_series.copy()
            random.shuffle(order)
            population.append(order)
        
        best_solution = None
        best_score = float('inf')
        best_detail = {}
        
        cycle_start = time.time()
        gen = 0
        
        while gen < max_iterations:
            if stop_event.is_set():
                break
            
            # Check cycle timeout
            if time.time() - cycle_start > self.cycle_seconds:
                break
            
            gen += 1
            
            # Evaluate all solutions
            scored = []
            for order in population:
                assignments = self._schedule_series(order)
                score, detail = self._evaluate_solution(assignments)
                scored.append((score, order, assignments, detail))
                self.iteration_count += 1
                
                if score < best_score:
                    best_score = score
                    best_solution = assignments
                    best_detail = detail
            
            # Sort by score (lower is better)
            scored.sort(key=lambda x: x[0])
            
            # Keep top 50%
            survivors = [s[1] for s in scored[:len(scored)//2]]
            
            # Create new population through crossover and mutation
            new_population = survivors.copy()
            
            while len(new_population) < population_size:
                parent1 = random.choice(survivors)
                parent2 = random.choice(survivors)
                child = self._crossover(parent1, parent2)
                
                # Higher mutation rate for exploration
                if random.random() < 0.3:
                    child = self._mutate(child)
                
                new_population.append(child)
            
            population = new_population
        
        return best_solution or [], best_score, best_detail

    def _crossover(self, parent1: List[Serie], parent2: List[Serie]) -> List[Serie]:
        """Order crossover (OX1)"""
        size = len(parent1)
        if size < 2:
            return parent1.copy()
        
        start = random.randint(0, size - 2)
        end = random.randint(start + 1, size - 1)
        
        child = [None] * size
        child[start:end] = parent1[start:end]
        
        remaining = [s for s in parent2 if s not in child[start:end]]
        idx = 0
        for i in range(size):
            if child[i] is None:
                child[i] = remaining[idx]
                idx += 1
        
        return child

    def _mutate(self, order: List[Serie]) -> List[Serie]:
        """Multiple mutation strategies"""
        if len(order) < 2:
            return order
        
        mutated = order.copy()
        mutation_type = random.choice(['swap', 'insert', 'reverse'])
        
        if mutation_type == 'swap':
            # Swap two random elements
            i = random.randint(0, len(mutated) - 1)
            j = random.randint(0, len(mutated) - 1)
            mutated[i], mutated[j] = mutated[j], mutated[i]
        elif mutation_type == 'insert':
            # Remove and insert at random position
            i = random.randint(0, len(mutated) - 1)
            elem = mutated.pop(i)
            j = random.randint(0, len(mutated))
            mutated.insert(j, elem)
        elif mutation_type == 'reverse':
            # Reverse a segment
            i = random.randint(0, len(mutated) - 2)
            j = random.randint(i + 1, len(mutated) - 1)
            mutated[i:j+1] = reversed(mutated[i:j+1])
        
        return mutated

    def _build_result(self, assignments: List[Tuple[Serie, Machine, datetime, datetime]], 
                      score: float, detail: Dict, status: str = 'COMPLETED') -> dict:
        """Build the output result dictionary"""
        result = {
            'status': status,
            'iterationCount': self.iteration_count,
            'cycleCount': self.cycle_count,
            'totalSeriesCount': self.total_series_count,
            'fixedSeriesCount': len(self.fixed_series),
            'schedulableSeriesCount': len(self.schedulable_series),
            'score': score,
            'scoreMetric': 'max_duration_per_box',
            'scoreDetail': detail,
            'lastImprovementCycle': self.last_improvement_cycle,
            'assignments': [],
            'sequenceSummaries': []
        }
        
        # Add fixed series to assignments
        for serie in self.fixed_series:
            if serie.scheduled_start and serie.scheduled_end and serie.assigned_machine:
                result['assignments'].append({
                    'serieId': serie.id,
                    'sequenceId': serie.sequence_id,
                    'machineName': serie.assigned_machine,
                    'scheduledStart': serie.scheduled_start.isoformat(),
                    'scheduledEnd': serie.scheduled_end.isoformat(),
                    'cuttingDurationMinutes': serie.cutting_time_minutes,
                    'partNumberMaterial': serie.part_number_material,
                    'placement': serie.placement,
                    'isFixed': True
                })
        
        # Build assignments list
        seq_times = {}
        
        # Process fixed series for summaries
        for serie in self.fixed_series:
            if serie.scheduled_start and serie.scheduled_end:
                if serie.sequence_id not in seq_times:
                    seq = next((s for s in self.sequences if s.id == serie.sequence_id), None)
                    seq_times[serie.sequence_id] = {
                        'starts': [], 
                        'ends': [], 
                        'cutting': 0,
                        'box_count': seq.box_count if seq else 1
                    }
                seq_times[serie.sequence_id]['starts'].append(serie.scheduled_start)
                seq_times[serie.sequence_id]['ends'].append(serie.scheduled_end)
                seq_times[serie.sequence_id]['cutting'] += serie.cutting_time_minutes
        
        # Process new assignments
        for serie, machine, start, end in assignments:
            result['assignments'].append({
                'serieId': serie.id,
                'sequenceId': serie.sequence_id,
                'machineName': machine.name,
                'scheduledStart': start.isoformat(),
                'scheduledEnd': end.isoformat(),
                'cuttingDurationMinutes': serie.cutting_time_minutes,
                'partNumberMaterial': serie.part_number_material,
                'placement': serie.placement,
                'isFixed': False
            })
            
            if serie.sequence_id not in seq_times:
                seq = next((s for s in self.sequences if s.id == serie.sequence_id), None)
                seq_times[serie.sequence_id] = {
                    'starts': [], 
                    'ends': [], 
                    'cutting': 0,
                    'box_count': seq.box_count if seq else 1
                }
            seq_times[serie.sequence_id]['starts'].append(start)
            seq_times[serie.sequence_id]['ends'].append(end)
            seq_times[serie.sequence_id]['cutting'] += serie.cutting_time_minutes
        
        # Build sequence summaries with duration/box ratio
        for seq_id, times in seq_times.items():
            if times['starts'] and times['ends']:
                min_start = min(times['starts'])
                max_end = max(times['ends'])
                duration_minutes = (max_end - min_start).total_seconds() / 60
                duration_hours = duration_minutes / 60
                box_count = times['box_count']
                ratio = duration_minutes / box_count if box_count > 0 else 0
                
                result['sequenceSummaries'].append({
                    'sequenceId': seq_id,
                    'minStartDate': min_start.isoformat(),
                    'maxEndDate': max_end.isoformat(),
                    'durationMinutes': round(duration_minutes, 2),
                    'durationHours': round(duration_hours, 2),
                    'totalCuttingMinutes': times['cutting'],
                    'boxCount': box_count,
                    'durationPerBox': round(ratio, 2)
                })
        
        # Overall metrics
        if result['sequenceSummaries']:
            all_starts = [datetime.fromisoformat(s['minStartDate']) for s in result['sequenceSummaries']]
            all_ends = [datetime.fromisoformat(s['maxEndDate']) for s in result['sequenceSummaries']]
            result['minStartDate'] = min(all_starts).isoformat()
            result['maxEndDate'] = max(all_ends).isoformat()
            result['maxDurationMinutes'] = max(s['durationMinutes'] for s in result['sequenceSummaries'])
            result['maxDurationPerBox'] = max(s['durationPerBox'] for s in result['sequenceSummaries'])
        
        return result

    def optimize(self) -> dict:
        """Run optimization - single cycle"""
        assignments, score, detail = self._genetic_optimize_cycle()
        self.cycle_count = 1
        self.best_solution = assignments
        self.best_score = score
        self.best_score_detail = detail
        
        return self._build_result(assignments, score, detail)

    def optimize_continuous(self):
        """
        Run continuous optimization in cycles.
        Keeps running until stopped, constantly improving.
        Outputs results to stdout after each cycle if improvement found.
        """
        print(json.dumps({
            'status': 'STARTED', 
            'message': 'Starting continuous optimization',
            'totalSeries': self.total_series_count,
            'fixedSeries': len(self.fixed_series),
            'schedulableSeries': len(self.schedulable_series),
            'machines': len(self.machines)
        }), flush=True)
        
        iterations_per_output = 100000
        last_output_time = time.time()
        output_interval = 5  # Output progress every 5 seconds
        
        while not stop_event.is_set():
            cycle_start = time.time()
            self.cycle_count += 1
            
            # Run one optimization cycle with high iterations
            assignments, score, detail = self._genetic_optimize_cycle(max_iterations=1000000)
            
            # Check if we found a better solution
            improved = False
            if score < self.best_score:
                self.best_score = score
                self.best_solution = assignments
                self.best_score_detail = detail
                self.last_improvement_cycle = self.cycle_count
                improved = True
            
            # Output progress periodically
            if improved or (time.time() - last_output_time > output_interval):
                last_output_time = time.time()
                
                if improved:
                    # Output the new best solution
                    result = self._build_result(self.best_solution, self.best_score, self.best_score_detail, 'IMPROVED')
                    print(json.dumps(result, default=str), flush=True)
                else:
                    # Just output progress
                    progress = {
                        'status': 'RUNNING',
                        'cycleCount': self.cycle_count,
                        'iterationCount': self.iteration_count,
                        'currentScore': score,
                        'bestScore': self.best_score,
                        'improved': False,
                        'lastImprovementCycle': self.last_improvement_cycle,
                        'elapsedSeconds': time.time() - self.start_time,
                        'totalSeries': self.total_series_count
                    }
                    print(json.dumps(progress), flush=True)
            
            # Wait for remaining cycle time (if any)
            elapsed = time.time() - cycle_start
            if elapsed < self.cycle_seconds:
                # Instead of sleeping, keep iterating
                remaining = self.cycle_seconds - elapsed
                if remaining > 0.5:
                    # Run more iterations in remaining time
                    extra_start = time.time()
                    while time.time() - extra_start < remaining and not stop_event.is_set():
                        assignments, score, detail = self._genetic_optimize_cycle(max_iterations=10000)
                        if score < self.best_score:
                            self.best_score = score
                            self.best_solution = assignments
                            self.best_score_detail = detail
                            self.last_improvement_cycle = self.cycle_count
        
        # Output final result when stopped
        if self.best_solution is not None:
            result = self._build_result(self.best_solution, self.best_score, self.best_score_detail, 'STOPPED')
            print(json.dumps(result, default=str), flush=True)
        else:
            print(json.dumps({'status': 'STOPPED', 'message': 'No solution found'}), flush=True)


def main():
    parser = argparse.ArgumentParser(description='Scheduling Optimizer for MG-CMS')
    parser.add_argument('--input', required=True, help='Input JSON file or JSON string')
    parser.add_argument('--timeout', type=int, default=30, help='Timeout per cycle in seconds')
    parser.add_argument('--continuous', action='store_true', help='Run in continuous mode')
    parser.add_argument('--output', help='Output file (default: stdout)')
    
    args = parser.parse_args()
    
    # Load input
    try:
        if args.input.startswith('{'):
            data = json.loads(args.input)
        else:
            with open(args.input, 'r') as f:
                data = json.load(f)
    except Exception as e:
        print(json.dumps({'status': 'FAILED', 'error': str(e)}))
        sys.exit(1)
    
    # Check for continuous mode in params
    continuous = args.continuous or data.get('params', {}).get('continuous', False)
    cycle_seconds = args.timeout or data.get('params', {}).get('cycleSeconds', 30)
    
    # Run optimization
    try:
        optimizer = SchedulingOptimizer(data, continuous=continuous, cycle_seconds=cycle_seconds)
        
        if continuous:
            optimizer.optimize_continuous()
        else:
            result = optimizer.optimize()
            output = json.dumps(result, indent=2, default=str)
            if args.output:
                with open(args.output, 'w') as f:
                    f.write(output)
            else:
                print(output)
            
    except Exception as e:
        print(json.dumps({'status': 'FAILED', 'error': str(e)}))
        sys.exit(1)


if __name__ == '__main__':
    main()
