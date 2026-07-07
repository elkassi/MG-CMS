import React from 'react';
import { Line } from 'react-chartjs-2';
import moment from 'moment';
import { Chart, LineElement, PointElement, LineController, CategoryScale, LinearScale, Title, Tooltip, Legend } from 'chart.js';
import annotationPlugin from 'chartjs-plugin-annotation';

Chart.register(
  LineElement,
  PointElement,
  LineController,
  CategoryScale,
  LinearScale,
  Title,
  Tooltip,
  Legend,
  annotationPlugin
);

const BoxTimelineGraph = ({ series, sequences, statBox, machineArr }) => {
    if (!series.length || !sequences.length) return null;
    // Build events: only sequence min start and max end
    let events = [];
    sequences.forEach(seq => {
        const relatedSeries = series.filter(s => s.sequence === seq.sequence);
        const starts = relatedSeries.map(s => s.dateDebutCoupe || s.EDdateDebutCoupe).filter(Boolean).map(d => moment(d, 'YYYY-MM-DD HH:mm:ss'));
        const ends = relatedSeries.map(s => s.dateFinCoupe || s.EDdateFinCoupe).filter(Boolean).map(d => moment(d, 'YYYY-MM-DD HH:mm:ss'));
        if (starts.length && ends.length) {
            const seqStart = moment.min(starts);
            const seqEnd = moment.max(ends);
            const stat = statBox.find(st => st.sequence === seq.sequence);
            const boxCount = stat ? stat.countBoxes : 0;
            events.push({ time: seqStart, delta: boxCount, label: `Début ${seq.sequence}` });
            events.push({ time: seqEnd, delta: -boxCount, label: `Fin ${seq.sequence}` });
        }
    });
    // Sort events by time
    events.sort((a, b) => a.time.valueOf() - b.time.valueOf());
    if (!events.length) return null;
    // Build a simple step function: walk through events, record value before and after each event
    let timeLabels = [];
    let dataPoints = [];
    let currentBox = 0;
    // Flat before first event
    const minTime = events[0].time.clone().startOf('minute');
    const maxTime = events[events.length - 1].time.clone().endOf('minute');
    let cursor = minTime.clone();
    if (cursor.isBefore(events[0].time)) {
        while (cursor.isBefore(events[0].time)) {
            timeLabels.push(cursor.format('YYYY-MM-DD HH:mm'));
            dataPoints.push(0);
            cursor.add(1, 'minute');
        }
    }
    // Step through events
    for (let i = 0; i < events.length; i++) {
        const e = events[i];
        // Fill up to this event (flat)
        while (cursor.isBefore(e.time)) {
            timeLabels.push(cursor.format('YYYY-MM-DD HH:mm'));
            dataPoints.push(currentBox);
            cursor.add(1, 'minute');
        }
        // At the event: record value before, then after
        timeLabels.push(e.time.format('YYYY-MM-DD HH:mm'));
        dataPoints.push(currentBox);
        currentBox += e.delta;
        timeLabels.push(e.time.format('YYYY-MM-DD HH:mm'));
        dataPoints.push(currentBox);
        cursor = e.time.clone().add(1, 'minute');
    }
    // Fill after last event (flat)
    while (cursor.isSameOrBefore(maxTime)) {
        timeLabels.push(cursor.format('YYYY-MM-DD HH:mm'));
        dataPoints.push(currentBox);
        cursor.add(1, 'minute');
    }
    // Build a map of event info for tooltips
    const eventInfoMap = {};
    events.forEach(e => {
        const key = e.time.format('YYYY-MM-DD HH:mm');
        if (!eventInfoMap[key]) eventInfoMap[key] = [];
        eventInfoMap[key].push(e.label);
    });
    // Calculate max boxes (number of machines * 16)
    const maxBoxes = Array.isArray(machineArr) ? machineArr.length * 16 : 16;
    // Chart.js data
    const data = {
        labels: timeLabels,
        datasets: [
            {
                label: 'Nombre de boxes',
                data: dataPoints,
                fill: {
                    target: 'origin',
                    above: 'rgba(30, 144, 255, 0.12)',
                },
                borderColor: 'rgba(30, 144, 255, 1)',
                backgroundColor: 'rgba(30, 144, 255, 0.12)',
                pointBackgroundColor: 'rgba(30, 144, 255, 1)',
                pointRadius: 1,
                pointHoverRadius: 4,
                pointBorderWidth: 2,
                borderWidth: 1,
                tension: 0,
                stepped: true
            }
        ]
    };
    // Chart.js options with custom tooltip
    const options = {
        responsive: true,
        plugins: {
            legend: { display: true, position: 'top', labels: { font: { size: 16 } } },
            title: { display: true, text: 'Nombre de boxes dans le temps', font: { size: 22 } },
            tooltip: {
                callbacks: {
                    title: (items) => items[0].label,
                    label: (item) => {
                        const time = item.label;
                        const value = item.parsed.y;
                        const eventsAtTime = eventInfoMap[time];
                        let lines = [`Nombre de boxes: ${value}`];
                        if (eventsAtTime && eventsAtTime.length) {
                            lines = lines.concat(eventsAtTime.map(ev => `\u2022 ${ev}`));
                        }
                        return lines;
                    }
                }
            },
            annotation: {
                annotations: [
                    // Horizontal max line
                    {
                        type: 'line',
                        scaleID: 'y',
                        value: maxBoxes,
                        borderColor: 'rgba(255,0,0,0.5)',
                        borderWidth: 2,
                        borderDash: [8, 6],
                        label: {
                            content: `Max (${maxBoxes})`,
                            enabled: true,
                            position: 'end',
                            color: 'rgba(255,0,0,0.7)',
                            font: { weight: 'bold', size: 14 }
                        }
                    },
                    // Vertical lines for each sequence start/end
                    ...events.map(e => ({
                        type: 'line',
                        scaleID: 'x',
                        value: e.time.format('YYYY-MM-DD HH:mm'),
                        borderColor: e.delta > 0 ? 'rgba(0,200,0,0.3)' : 'rgba(255,0,0,0.3)',
                        borderWidth: 3,
                        label: {
                            content: e.label,
                            enabled: true,
                            position: e.delta > 0 ? 'start' : 'end',
                            color: e.delta > 0 ? 'rgba(0,200,0,0.5)' : 'rgba(255,0,0,0.5)',
                            font: { size: 12 }
                        }
                    }))
                ]
            }
        },
        layout: {
            padding: { left: 30, right: 30, top: 30, bottom: 30 }
        },
        elements: {
            point: {
                radius: 0,
                borderWidth: 0,
                backgroundColor: 'rgba(30, 144, 255, 1)'
            }
        },
        scales: {
            x: {
                title: { display: true, text: 'Temps', font: { size: 16 } },
                type: 'category',
                ticks: { autoSkip: true, maxTicksLimit: 20, font: { size: 13 } },
                grid: { color: 'rgba(180,180,180,0.15)' }
            },
            y: {
                title: { display: true, text: 'Nombre de boxes', font: { size: 16 } },
                beginAtZero: true,
                grid: { color: 'rgba(180,180,180,0.15)' },
                ticks: { font: { size: 13 } }
            } 
        }
    };
    return (
        <div style={{ margin: '32px 0', background: '#f8fbff', borderRadius: 16, boxShadow: '0 2px 16px #0001', padding: 24 }}>
            <Line data={data} options={options} />
        </div>
    );
};

export default BoxTimelineGraph;
