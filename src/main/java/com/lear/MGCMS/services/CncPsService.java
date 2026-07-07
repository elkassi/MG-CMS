package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.*;
import com.lear.MGCMS.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class CncPsService {

    @Autowired
    private CncPsSessionRepository sessionRepository;

    @Autowired
    private CncPsLeatherConsumptionRepository consumptionRepository;

    @Autowired
    private CncControlRepository controlRepository;

    @Autowired
    private MachineCncRepository machineCncRepository;

    @Autowired
    private ProgramCNCRepository programCNCRepository;

    @Autowired
    private CodeDefautRepository codeDefautRepository;

    @Autowired
    private CodeScrapRepository codeScrapRepository;

    @Autowired
    private CncProductionRepository productionRepository;

    // Session methods
    public CncPsSession saveSession(CncPsSession session) {
        return sessionRepository.save(session);
    }

    public Optional<CncPsSession> findSessionById(Long id) {
        return sessionRepository.findById(id);
    }

    public List<CncPsSession> findAllSessions() {
        return sessionRepository.findAllByOrderByCreatedAtDesc();
    }

    public Page<CncPsSession> findAllSessions(Pageable pageable) {
        return sessionRepository.findAll(pageable);
    }

    public List<CncPsSession> findSessionsByOperator(String operator) {
        return sessionRepository.findByOperatorOrderByCreatedAtDesc(operator);
    }

    public List<CncPsSession> findRecentSessionsByOperator(String operator, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return sessionRepository.findByOperatorAndCreatedAtAfterOrderByCreatedAtDesc(operator, since);
    }

    public List<CncPsSession> findByBoxId(String boxId) {
        return sessionRepository.findByBoxId(boxId);
    }

    public boolean existsByBoxId(String boxId) {
        List<CncPsSession> sessions = sessionRepository.findByBoxId(boxId);
        return sessions != null && !sessions.isEmpty();
    }

    public CncPsSession completeSession(Long sessionId) {
        Optional<CncPsSession> opt = sessionRepository.findById(sessionId);
        if (opt.isPresent()) {
            CncPsSession session = opt.get();
            session.setCompleted(true);
            return sessionRepository.save(session);
        }
        return null;
    }

    public CncPsSession reopenSession(Long sessionId) {
        Optional<CncPsSession> opt = sessionRepository.findById(sessionId);
        if (opt.isPresent()) {
            CncPsSession session = opt.get();
            session.setCompleted(false);
            session.setProductionStatus(null);
            session.setMachineCnc(null);
            session.setStartProductionDate(null);
            session.setEndProductionDate(null);
            session.setProductionOperator(null);
            return sessionRepository.save(session);
        }
        return null;
    }

    public void deleteSession(Long sessionId) {
        sessionRepository.deleteById(sessionId);
    }

    public CncPsSession markLabelPrinted(Long sessionId) {
        Optional<CncPsSession> opt = sessionRepository.findById(sessionId);
        if (opt.isPresent()) {
            CncPsSession session = opt.get();
            session.setLabelPrinted(true);
            return sessionRepository.save(session);
        }
        return null;
    }

    // Consumption methods
    public CncPsLeatherConsumption addConsumption(Long sessionId, String leatherPartNumber, String serial, String lot,
                                                   Double quantiteInitial, Double quantiteConsumed, Double quantiteRetour) {
        Optional<CncPsSession> opt = sessionRepository.findById(sessionId);
        if (!opt.isPresent()) {
            return null;
        }
        CncPsLeatherConsumption consumption = new CncPsLeatherConsumption();
        consumption.setSession(opt.get());
        consumption.setLeatherPartNumber(leatherPartNumber);
        consumption.setSerial(serial);
        consumption.setLot(lot);
        consumption.setQuantiteInitial(quantiteInitial);
        consumption.setQuantiteConsumed(quantiteConsumed);
        consumption.setQuantiteRetour(quantiteRetour);
        consumption.setCreatedAt(LocalDateTime.now());
        return consumptionRepository.save(consumption);
    }

    public List<CncPsLeatherConsumption> findConsumptionsBySession(Long sessionId) {
        return consumptionRepository.findBySessionId(sessionId);
    }

    public boolean deleteConsumption(Long consumptionId) {
        if (consumptionRepository.existsById(consumptionId)) {
            consumptionRepository.deleteById(consumptionId);
            return true;
        }
        return false;
    }

    public List<CncPsLeatherConsumption> findConsumptionsBySerial(String serial) {
        return consumptionRepository.findBySerialOrderByCreatedAtDesc(serial);
    }

    public double getTotalConsumedForSession(Long sessionId) {
        List<CncPsLeatherConsumption> consumptions = consumptionRepository.findBySessionId(sessionId);
        return consumptions.stream()
                .mapToDouble(c -> c.getQuantiteConsumed() != null ? c.getQuantiteConsumed() : 0.0)
                .sum();
    }

    // CncControl methods
    public CncControl addControl(Long sessionId, Integer quantite, String result, String codeDefaut, String codeScrap,
                                  String matricule, String stage, String programNumber, String panelNumber, String pattern,
                                  String numBonScrap, String scrapStatus, Long machineId) {
        Optional<CncPsSession> opt = sessionRepository.findById(sessionId);
        if (!opt.isPresent()) return null;
        CncControl control = new CncControl();
        control.setSession(opt.get());
        control.setQuantite(quantite);
        control.setResult(result);
        control.setCodeDefaut(codeDefaut);
        control.setCodeScrap(codeScrap);
        control.setMatricule(matricule);
        control.setStage(stage);
        control.setProgramNumber(programNumber);
        control.setPanelNumber(panelNumber);
        control.setPattern(pattern);
        control.setNumBonScrap(numBonScrap);
        control.setScrapStatus(scrapStatus);
        if (machineId != null) {
            machineCncRepository.findById(machineId).ifPresent(control::setMachine);
        }
        control.setCreatedAt(LocalDateTime.now());
        return controlRepository.save(control);
    }

    /** Update the optional scrap voucher number + replacement status on a control (rectification). */
    public CncControl updateControlScrap(Long controlId, String numBonScrap, String scrapStatus) {
        Optional<CncControl> opt = controlRepository.findById(controlId);
        if (!opt.isPresent()) return null;
        CncControl control = opt.get();
        control.setNumBonScrap(numBonScrap);
        control.setScrapStatus(scrapStatus);
        return controlRepository.save(control);
    }

    /** Total pieces controlled at a given stage (CNC / PRESS / BLIND). */
    public int getControlledQtyForStage(Long sessionId, String stage) {
        return controlRepository.findBySessionId(sessionId).stream()
                .filter(c -> stage.equals(c.getStage() != null ? c.getStage() : "CNC"))
                .mapToInt(c -> c.getQuantite() != null ? c.getQuantite() : 0)
                .sum();
    }

    /** Pieces scrapped at a stage (a row is scrapped when it carries a codeScrap). */
    public int getScrappedQtyForStage(Long sessionId, String stage) {
        return controlRepository.findBySessionId(sessionId).stream()
                .filter(c -> stage.equals(c.getStage() != null ? c.getStage() : "CNC"))
                .filter(c -> c.getCodeScrap() != null && !c.getCodeScrap().trim().isEmpty())
                .mapToInt(c -> c.getQuantite() != null ? c.getQuantite() : 0)
                .sum();
    }

    /** Pieces passing a stage (controlled minus scrapped) — i.e. the target for the next stage. */
    public int getPassingQtyForStage(Long sessionId, String stage) {
        return getControlledQtyForStage(sessionId, stage) - getScrappedQtyForStage(sessionId, stage);
    }

    // ----- per-programme variants (each CNC programme is controlled separately) -----

    private static String nz(String s) { return s == null ? "" : s.trim(); }

    private static boolean sameProg(CncControl c, String programNumber, String panelNumber, String pattern) {
        return nz(c.getProgramNumber()).equals(nz(programNumber))
                && nz(c.getPanelNumber()).equals(nz(panelNumber))
                && nz(c.getPattern()).equals(nz(pattern));
    }

    /** Pieces controlled at a stage for one programme (matched on programNumber+panelNumber+pattern). */
    public int getControlledQtyForStageAndProg(Long sessionId, String stage, String programNumber, String panelNumber, String pattern) {
        return controlRepository.findBySessionId(sessionId).stream()
                .filter(c -> stage.equals(c.getStage() != null ? c.getStage() : "CNC"))
                .filter(c -> sameProg(c, programNumber, panelNumber, pattern))
                .mapToInt(c -> c.getQuantite() != null ? c.getQuantite() : 0)
                .sum();
    }

    /** Pieces scrapped at a stage for one programme. */
    public int getScrappedQtyForStageAndProg(Long sessionId, String stage, String programNumber, String panelNumber, String pattern) {
        return controlRepository.findBySessionId(sessionId).stream()
                .filter(c -> stage.equals(c.getStage() != null ? c.getStage() : "CNC"))
                .filter(c -> sameProg(c, programNumber, panelNumber, pattern))
                .filter(c -> c.getCodeScrap() != null && !c.getCodeScrap().trim().isEmpty())
                .mapToInt(c -> c.getQuantite() != null ? c.getQuantite() : 0)
                .sum();
    }

    /** Scrapped pieces marked REMPLACE at a stage for one programme (their replacements re-enter the next stage). */
    public int getReplacedQtyForStageAndProg(Long sessionId, String stage, String programNumber, String panelNumber, String pattern) {
        return controlRepository.findBySessionId(sessionId).stream()
                .filter(c -> stage.equals(c.getStage() != null ? c.getStage() : "CNC"))
                .filter(c -> sameProg(c, programNumber, panelNumber, pattern))
                .filter(c -> c.getCodeScrap() != null && !c.getCodeScrap().trim().isEmpty())
                .filter(c -> "REMPLACE".equals(c.getScrapStatus() != null ? c.getScrapStatus().trim() : null))
                .mapToInt(c -> c.getQuantite() != null ? c.getQuantite() : 0)
                .sum();
    }

    /**
     * Pieces passing a stage for one programme = controlled - scrapped + replaced. A scrap marked
     * REMPLACE is replaced, so its piece re-enters the next stage's target.
     */
    public int getPassingQtyForStageAndProg(Long sessionId, String stage, String programNumber, String panelNumber, String pattern) {
        return getControlledQtyForStageAndProg(sessionId, stage, programNumber, panelNumber, pattern)
                - getScrappedQtyForStageAndProg(sessionId, stage, programNumber, panelNumber, pattern)
                + getReplacedQtyForStageAndProg(sessionId, stage, programNumber, panelNumber, pattern);
    }

    public List<CncControl> findControlsBySession(Long sessionId) {
        return controlRepository.findBySessionId(sessionId);
    }

    public boolean deleteControl(Long controlId) {
        if (controlRepository.existsById(controlId)) {
            controlRepository.deleteById(controlId);
            return true;
        }
        return false;
    }

    public Optional<CncControl> findControlById(Long id) {
        return controlRepository.findById(id);
    }

    public int getTotalControlQuantityForSession(Long sessionId) {
        List<CncControl> controls = controlRepository.findBySessionId(sessionId);
        return controls.stream().mapToInt(c -> c.getQuantite() != null ? c.getQuantite() : 0).sum();
    }

    public int getTotalControlQuantityForSessionAndPanel(Long sessionId, String panelNumber) {
        List<CncControl> controls = controlRepository.findBySessionId(sessionId);
        return controls.stream()
                .filter(c -> Objects.equals(c.getPanelNumber(), panelNumber))
                .mapToInt(c -> c.getQuantite() != null ? c.getQuantite() : 0)
                .sum();
    }

    // MachineCnc methods
    public List<MachineCnc> findAllMachines() {
        return machineCncRepository.findAll();
    }

    public MachineCnc saveMachine(MachineCnc machine) {
        return machineCncRepository.save(machine);
    }

    public Optional<MachineCnc> findMachineById(Long id) {
        return machineCncRepository.findById(id);
    }

    // CodeDefaut / CodeScrap CNC filtered
    public List<CodeDefaut> findCodeDefautCNC() {
        return codeDefautRepository.findAllCNC();
    }

    public List<CodeScrap> findCodeScrapCNC() {
        return codeScrapRepository.findAllCNC();
    }

    // Session queries for shifts/KPI
    public List<CncPsSession> findSessionsBetween(LocalDateTime start, LocalDateTime end) {
        return sessionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
    }

    public List<CncPsSession> findCompletedSessionsBetween(LocalDateTime start, LocalDateTime end) {
        return sessionRepository.findByCompletedAndCreatedAtBetweenOrderByCreatedAtDesc(true, start, end);
    }

    // ProgramCNC methods (single source of truth for CNC panels/patterns)
    public List<ProgramCNC> findProgramCNCByPartNumber(String partNumber) {
        return programCNCRepository.findByPartNumber(partNumber);
    }

    public ProgramCNC saveProgramCNC(ProgramCNC programCNC) {
        return programCNCRepository.save(programCNC);
    }

    public Page<ProgramCNC> findAllProgramCNC(Pageable pageable) {
        return programCNCRepository.findAll(pageable);
    }

    // CncProduction methods
    public CncProduction saveProduction(CncProduction production) {
        return productionRepository.save(production);
    }

    public List<CncProduction> findProductionsBySession(Long sessionId) {
        return productionRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    public Optional<CncProduction> findProductionById(Long id) {
        return productionRepository.findById(id);
    }

    public List<CncProduction> findActiveProductionsBySession(Long sessionId) {
        return productionRepository.findBySessionIdAndStatus(sessionId, "In progress");
    }
}
