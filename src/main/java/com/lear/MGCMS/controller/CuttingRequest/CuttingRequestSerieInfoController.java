package com.lear.MGCMS.controller.CuttingRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.validation.Valid;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlan;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieRouleauInfo;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import com.lear.MGCMS.domain.CuttingSpeed;
import com.lear.MGCMS.domain.PartNumberMaterialConfigData;
import com.lear.MGCMS.payload.RapportOverlap;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanRepository;
import com.lear.MGCMS.security.Constants;
import com.lear.MGCMS.services.*;
import com.lear.MGCMS.services.CuttingRequest.data.CuttingRequestDataService;
import com.lear.MGCMS.services.CuttingRequest.data.CuttingRequestSerieDataService;
import com.lear.MGCMS.services.splice.MarkerService;
import com.lear.MGCMS.utils.ExcelHelper;
import com.lear.MGCMS.utils.StatusQualite;
import com.lear.MGCMS.utils.UtilFunctions;
import com.lear.cms.domain.Coupe;
import com.lear.cms.domain.Matlassage;
import com.lear.cms.domain.SuiviCoupe;
import com.lear.cms.domain.SuiviMatelassage;
import com.lear.cms.repositories.*;
import com.lear.ctc.domain.SequenceDetails;
import com.lear.ctc.repositories.SequenceDetailsRepository;
import com.lear.learpokayoke.domain.PokaYokeMain;
import com.lear.learpokayoke.repositories.PokaYokeMainRepository;
import com.lear.splice.domain.Marker;
import com.lear.splice.repositories.MarkersOnlyCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.services.CuttingRequest.CuttingRequestSerieInfoService;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieInfo;
import com.lear.MGCMS.payload.LectraStats;

@RestController
@RequestMapping("/api/cuttingRequestSerieInfo")
public class CuttingRequestSerieInfoController {

    @Autowired
    private CuttingRequestSerieInfoService service;

    @Autowired
    private CuttingRequestSerieDataService cuttingRequestSerieDataService;
    @Autowired
    private CuttingRequestDataService cuttingRequestDataService;
    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @Autowired
    private OptitimeService optitimeService;

    @GetMapping
    public List<CuttingRequestSerieInfo> findAll(
            @RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "shift", required = false) String shift
    ) {
        return service.findAll(date, shift);
    }

    @GetMapping("/historique")
    public List<CuttingRequestSerieInfo> historique(
            @RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return service.historique(date);
    }

    @GetMapping("/seriesArr")
    public List<CuttingRequestSerieInfo> getAll(
            @RequestParam(value = "series", required = true) List<String> series
    ) {
        return service.findSeries(series);
    }

    @GetMapping("/rapportOverlap")
    public List<RapportOverlap> rapportOverlap(
            @RequestParam(value = "date1", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date1,
            @RequestParam(value = "date2", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date2
    ) {
        List<String> prodList = optitimeService.getListNames("05");
        return service.rapportOverlap(date1, date2, prodList);
    }

    @GetMapping("/notYet")
    public List<CuttingRequestSerieInfo> findAllNotYet() {
        return service.findAllNotYet();
    }

    @GetMapping("/inProgress")
    public List<CuttingRequestSerieInfo> findAllInProgress() {
        return service.findAllInProgress();
    }

    @GetMapping("/machines")
    public List<String> findMachines(
            @RequestParam(value = "sequence", required = false) String sequence,
            @RequestParam(value = "reftissuList", required = false) List<String> reftissuList
    ) {
        return service.findMachines(sequence, reftissuList);
    }

    @GetMapping("/filtre")
    public List<CuttingRequestSerieInfo> findStats(
            @RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "shift", required = false) String shift,
            @RequestParam(value = "machine", required = false) String machine,
            @RequestParam(value = "type", defaultValue = "coupe", required = false) String type
    ) {

        LocalDateTime startDate = null, endDate = null;
        if (shift.equals("2")) {
            startDate = date.atTime(05, 55);
            endDate = date.atTime(13, 45);
        } else if (shift.equals("3")) {
            startDate = date.atTime(13, 55);
            endDate = date.atTime(21, 45);
        } else {
            startDate = date.atTime(21, 55).minusDays(1);
            endDate = date.atTime(05, 45);
        }
        if (endDate.compareTo(LocalDateTime.now()) > 0) {
            endDate = LocalDateTime.now();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        System.out.println(startDate.format(formatter) + " => " + endDate.format(formatter) + " : " + machine);

        if (type.equals("matelassage")) {
            return service.findBetween2(startDate, endDate, machine);
        }

        return service.findBetween(startDate, endDate, machine);
    }

    @GetMapping("/statusQualite")
    public List<StatusQualite> statusQualite(
            @RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "date2", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date2,
            @RequestParam(value = "machine", required = false) String machine,
            @RequestParam(value = "valide", required = false) Boolean valide
    ) {
        return service.statusQualite(date, date2, machine, valide);
    }

    @GetMapping("/{serie}")
    public ResponseEntity<?> findBySerie(@PathVariable String serie) {
        CuttingRequestSerieInfo obj = service.findBySerie(serie);

        if (obj != null) {
            return new ResponseEntity<CuttingRequestSerieInfo>(obj, HttpStatus.OK);
        }
        return new ResponseEntity<String>(serie + " not found", HttpStatus.BAD_REQUEST);
    }

    @Autowired
    private MatlassageRepository matlassageRepository;
    @Autowired
    private SuiviMatelassageRepository suiviMatelassageRepository;
    @Autowired
    private SuiviPlanningRepository suiviPlanningRepository;
    @Autowired
    private OrderScheduleRepository orderScheduleRepository;
    @Autowired
    private ProduitFinitRepository produitFinitRepository;
    @Autowired
    private QueryService queryService;

    @Autowired
    private SerieRouleauTempService serieRouleauTempService;

    @Autowired
    private SuiviCoupeRepository suiviCoupeRepository;
    @Autowired
    private CoupeRepository coupeRepository;

    @Autowired
    private PokaYokeMainRepository pokaYokeMainRepository;
    @Autowired
    private SequenceDetailsRepository sequenceDetailsRepository;

    @PostMapping
//    @PreAuthorize("hasRole('CHEF_DE_ZONE') or hasRole('CHEF_EQUIPE') or hasRole('ADMIN')")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> save(@RequestBody CuttingRequestSerieInfo obj) {
        try {
            for (CuttingRequestSerieRouleauInfo crsr : obj.getCuttingRequestSerieRouleaus()) {
                if (crsr.getConfirmReftissu() != null && crsr.getConfirmReftissu().startsWith("P")) {
                    crsr.setConfirmReftissu(crsr.getConfirmReftissu().replaceFirst("P", ""));
                }
                if (crsr.getLotFrs() != null && crsr.getLotFrs().startsWith("H")) {
                    crsr.setLotFrs(crsr.getLotFrs().replaceFirst("H", ""));
                }
                if (crsr.getIdRouleau() != null && crsr.getIdRouleau().startsWith("S")) {
                    crsr.setIdRouleau(crsr.getIdRouleau().replaceFirst("S", ""));
                }
                crsr.setLongueurPremierCouche(obj.getLongueur());
                crsr.setCuttingRequestSerie(obj);
            }
            CuttingRequestSerieInfo crsi = service.findBySerie(obj.getSerie());
            if (crsi.getMatelasseur1() != null) obj.setMatelasseur1(crsi.getMatelasseur1());
            if (crsi.getMatelasseur2() != null) obj.setMatelasseur2(crsi.getMatelasseur2());
            if (crsi.getMatelasseur3() != null) obj.setMatelasseur3(crsi.getMatelasseur2());
            if (crsi.getMatelasseur4() != null) obj.setMatelasseur4(crsi.getMatelasseur2());

            if (crsi.getTableMatelassage() != null) obj.setTableMatelassage(crsi.getTableMatelassage());
            if (crsi.getDateDebutMatelassage() != null) obj.setDateDebutMatelassage(crsi.getDateDebutMatelassage());

            if (obj.getStatusMatelassage().equalsIgnoreCase("Incomplete")) {
                try {
                    SuiviMatelassage sm = suiviMatelassageRepository.findByNserie(obj.getSerie());
                    Matlassage mt = matlassageRepository.findByNserie(Long.parseLong(obj.getSerie()));

                    sm.setStatu("Incomplet");
                    mt.setStatu("Incomplet");

                    matlassageRepository.save(mt);
                    suiviMatelassageRepository.save(sm);
                } catch (Exception e) {
                    Constants.writeLogs("Error : " + e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                        Constants.writeLogs(element.toString());
                    }

                }
            }
            if (obj.getStatusMatelassage().equals("In progress") && !crsi.getStatusMatelassage().equals("In progress")) {
                obj.setDateDebutMatelassage(LocalDateTime.now());
                try {
                    SuiviMatelassage sm = suiviMatelassageRepository.findByNserie(obj.getSerie());
                    Matlassage mt = matlassageRepository.findByNserie(Long.parseLong(obj.getSerie()));
                    mt.setMatMatlasseur1(obj.getMatelasseur1());
                    mt.setMatMatlasseur2(obj.getMatelasseur2());
                    mt.setStatu("Non demarre");
                    mt.setTablee(obj.getTableMatelassage());
                    sm.setTablee(obj.getTableMatelassage());
                    sm.setDateDebut(obj.getDateDebutMatelassage().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    sm.setDateFin("  ----  ");
                    sm.setDebutIncomplet("  ----  ");
                    sm.setFinIncomplet("  ----  ");
                    sm.setStatu("En cours");


//                orderScheduleRepository.updateStatus(produitFinitRepository.findWOBySequence(obj.getCuttingRequest().getSequence()), "S");
                    orderScheduleRepository.updateStatusNew(obj.getCuttingRequest().getSequence(), "S");
                    matlassageRepository.save(mt);
                    suiviMatelassageRepository.save(sm);
                    suiviPlanningRepository.updateStatu1(obj.getCuttingRequest().getSequence());

                } catch (Exception e) {
                    Constants.writeLogs("Error : " + e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                        Constants.writeLogs(element.toString());
                    }

                }
                CuttingRequestSerieInfo newObj = service.save(obj);
                serieRouleauTempService.deleteByid(obj.getTableMatelassage());
                return new ResponseEntity<CuttingRequestSerieInfo>(newObj, HttpStatus.CREATED);
            }
            if (obj.getStatusMatelassage().equals("Complete") && !crsi.getStatusMatelassage().equals("Complete")) {
                obj.setDateFinMatelassage(LocalDateTime.now());
                CuttingRequestSerieInfo newObj = service.save(obj);
                serieRouleauTempService.deleteByid(obj.getTableMatelassage());

                try {

                    SuiviMatelassage sm = suiviMatelassageRepository.findByNserie(obj.getSerie());
                    Matlassage mt = matlassageRepository.findByNserie(Long.parseLong(obj.getSerie()));
                    mt.setMatMatlasseur1(obj.getMatelasseur1());
                    mt.setMatMatlasseur2(obj.getMatelasseur2());
                    mt.setReturnMagasin(obj.getRetourMagasin() + "");
                    mt.setStatu("Complet");
                    mt.setTablee(obj.getTableMatelassage());
                    sm.setTablee(obj.getTableMatelassage());
                    sm.setDateDebut(obj.getDateDebutMatelassage().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    sm.setDateFin(obj.getDateFinMatelassage().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    sm.setDebutIncomplet("  ----  ");
                    sm.setFinIncomplet("  ----  ");
                    sm.setStatu("Complet");
                    matlassageRepository.save(mt);
                    suiviMatelassageRepository.save(sm);
                    boolean finished = true;
                    for (String statu : service.getStatusMatelassageBySequence(obj.getCuttingRequest().getSequence())) {
                        if (!statu.equalsIgnoreCase("Complete")) {
                            finished = false;
                            break;
                        }
                    }
                    if (finished) {
                        suiviPlanningRepository.updateStatu2(obj.getCuttingRequest().getSequence());
                    } else {
                        suiviPlanningRepository.updateStatu1(obj.getCuttingRequest().getSequence());
                    }
                    orderScheduleRepository.updateStatus(produitFinitRepository.findWOBySequence(obj.getCuttingRequest().getSequence()), "S");
                    orderScheduleRepository.updateStatusNew(obj.getCuttingRequest().getSequence(), "S");

                } catch (Exception e) {
                    Constants.writeLogs("Error : " + e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                        Constants.writeLogs(element.toString());
                    }

                }

                return new ResponseEntity<CuttingRequestSerieInfo>(newObj, HttpStatus.CREATED);
            }

            if (obj.getStatusCoupe().equals("In progress") && !crsi.getStatusCoupe().equals("In progress")) {
                if (obj.getDateDebutCoupe() == null) {
                    obj.setDateDebutCoupe(LocalDateTime.now());
                }

                CuttingRequestSerieInfo newObj = service.save(obj);

                try {
                    SuiviCoupe sm = suiviCoupeRepository.findByNserie(obj.getSerie());
                    Coupe mt = coupeRepository.findFirstByNserie(Long.parseLong(obj.getSerie()));

                    mt.setMatricule(obj.getMatelasseur1());
                    mt.setMatricule2(obj.getMatelasseur2());
                    mt.setStatut("Non demarre");
                    mt.setMachine(obj.getTableCoupe());
                    sm.setTablee(obj.getTableCoupe());
                    sm.setDateDebut(obj.getDateDebutCoupe().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    sm.setDateFin("");
                    sm.setDebutIncomplet("  ----  ");
                    sm.setFinIncomplet("  ----  ");
                    sm.setStatu("En cours");
                    coupeRepository.save(mt);
                    suiviCoupeRepository.save(sm);
                    suiviPlanningRepository.updateStatuC1(obj.getCuttingRequest().getSequence());
                    orderScheduleRepository.updateStatus(produitFinitRepository.findWOBySequence(obj.getCuttingRequest().getSequence()), "S");
                    orderScheduleRepository.updateStatusNew(obj.getCuttingRequest().getSequence(), "S");
                    List<PokaYokeMain> pkArr = pokaYokeMainRepository.findByMarker(obj.getPlacement());
                    try{
                        PokaYokeMain pkObj = new PokaYokeMain();
                        if(pkArr.size() == 1) {
                            pkObj = pkArr.get(0);
                            if(!obj.getPartNumberMaterial().equalsIgnoreCase(pkObj.getMaterial())) {
                                pkObj.setMaterial(obj.getPartNumberMaterial());
                                pokaYokeMainRepository.save(pkObj);
                            }
                        } else {
                            if(pkArr.size() > 1) {
                                pokaYokeMainRepository.deleteByMarker(obj.getPlacement());
                            }
                            pkObj.setDate(LocalDateTime.now());
                            pkObj.setMaterial(obj.getPartNumberMaterial());
                            pkObj.setMarker(obj.getPlacement());
                            pokaYokeMainRepository.save(pkObj);
                        }
                    }catch (Exception e) {
                        System.out.println("Modify serie problem : " + e.getMessage());
                    }
                } catch (Exception e) {
                    Constants.writeLogs("Error : " + e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                        Constants.writeLogs(element.toString());
                    }

                }

                return new ResponseEntity<CuttingRequestSerieInfo>(newObj, HttpStatus.CREATED);
            }
            if (obj.getStatusCoupe().equals("Complete") && !crsi.getStatusCoupe().equals("Complete")) {
                if (obj.getDateFinCoupe() == null) {
                    obj.setDateDebutCoupe(LocalDateTime.now());
                }
                CuttingRequestSerieInfo newObj = service.save(obj);

                try {
                    SuiviCoupe sm = suiviCoupeRepository.findByNserie(obj.getSerie());
                    Coupe mt = coupeRepository.findFirstByNserie(Long.parseLong(obj.getSerie()));

                    mt.setMatricule(obj.getMatelasseur1());
                    mt.setMatricule2(obj.getMatelasseur2());
                    mt.setStatut("complet");
                    mt.setMachine(obj.getTableCoupe());
                    sm.setTablee(obj.getTableCoupe());
                    sm.setDateDebut(obj.getDateDebutCoupe().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    sm.setDateFin(obj.getDateFinCoupe().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    sm.setDebutIncomplet("  ----  ");
                    sm.setFinIncomplet("  ----  ");
                    sm.setStatu("Complet");
                    coupeRepository.save(mt);
                    suiviCoupeRepository.save(sm);
                    try{
                    List<PokaYokeMain> pkArr = pokaYokeMainRepository.findByMarker(obj.getPlacement());
                    PokaYokeMain pkObj = new PokaYokeMain();
                    if(pkArr.size() == 1) {
                        pkObj = pkArr.get(0);
                        if(!obj.getPartNumberMaterial().equalsIgnoreCase(pkObj.getMaterial())) {
                            pkObj.setMaterial(obj.getPartNumberMaterial());
                            pokaYokeMainRepository.save(pkObj);
                        }
                    } else {
                        if(pkArr.size() > 1) {
                            pokaYokeMainRepository.deleteByMarker(obj.getPlacement());
                        }
                        pkObj.setDate(LocalDateTime.now());
                        pkObj.setMaterial(obj.getPartNumberMaterial());
                        pkObj.setMarker(obj.getPlacement());
                        pokaYokeMainRepository.save(pkObj);
                    }
                    }catch (Exception e) {
                        System.out.println("Modify serie problem : " + e.getMessage());
                    }
                    boolean finished = true;
                    for (String statu : service.getStatusCoupeBySequence(obj.getCuttingRequest().getSequence())) {
                        if (!statu.equalsIgnoreCase("Complete")) {
                            finished = false;
                            break;
                        }
                    }
                    if (finished) {
                        orderScheduleRepository.updateStatus(produitFinitRepository.findWOBySequence(obj.getCuttingRequest().getSequence()), "E");
                        orderScheduleRepository.updateStatusNew(obj.getCuttingRequest().getSequence(), "E");
                        suiviPlanningRepository.updateStatuC2(obj.getCuttingRequest().getSequence());
                    } else {
                        orderScheduleRepository.updateStatus(produitFinitRepository.findWOBySequence(obj.getCuttingRequest().getSequence()), "S");
                        orderScheduleRepository.updateStatusNew(obj.getCuttingRequest().getSequence(), "S");
                        suiviPlanningRepository.updateStatuC1(obj.getCuttingRequest().getSequence());
                    }

                } catch (Exception e) {
                    Constants.writeLogs("Error : " + e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                        Constants.writeLogs(element.toString());
                    }

                }

                return new ResponseEntity<CuttingRequestSerieInfo>(newObj, HttpStatus.CREATED);
            }

            CuttingRequestSerieInfo newObj = service.save(obj);
            return new ResponseEntity<CuttingRequestSerieInfo>(newObj, HttpStatus.CREATED);
        } catch (Exception e) {
            Constants.writeLogs("Error : " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                Constants.writeLogs(element.toString());
            }
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Autowired
    private CuttingPlanRepository cuttingPlanRepository;
    @Autowired
    private CuttingSpeedService cuttingSpeedService;

    @PostMapping("/add")
    @PreAuthorize("hasRole('IMPORTER') or hasRole('ADMIN')")
    public ResponseEntity<?> add(@Valid @RequestBody CuttingRequestSerieData obj, BindingResult result) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;
        String sequence = obj.getSequence();
        CuttingRequestData crd = cuttingRequestDataService.findBySequence(sequence);
        Long nserie = matlassageRepository.getMaxNserie();
        if (nserie == null) {
            return new ResponseEntity<String>("Error getting nserie", HttpStatus.BAD_REQUEST);
        } else {
            nserie++;
        }

        if (crd != null) {
            obj.setPlanningDate(crd.getPlanningDate());
            obj.setShift(crd.getShift());
            obj.setStatusMatelassage("Waiting");
            obj.setStatusCoupe("Waiting");
            obj.setRetourMagasin(0.0);
            obj.setPerimetre(ExcelHelper.getPerimetre(obj.getPlacement()));
            Integer countNbrPiece = ExcelHelper.getTotalEmp(obj.getPlacement());
            obj.setNbrPiece(countNbrPiece);
            if (obj.getNbrCouche() != null && obj.getNbrPiece() != null) {
                obj.setNbrPieceTotal((double) (obj.getNbrPiece() * obj.getNbrCouche()));
            }
            CuttingSpeed speed = cuttingSpeedService.findById(obj.getConfig());
            if (obj.getPerimetre() != null) {
                if (speed != null) {
                    obj.setTempsDeCoupe(UtilFunctions
                            .convertTwoDigit(obj.getPerimetre() / (speed.getVitesse() * 100), 5));
                } else {
                    obj.setTempsDeCoupe(UtilFunctions.convertTwoDigit(obj.getPerimetre() / 300, 5));
                }
            }
            obj.setAutoCoupe(true);
            obj.setPartNumbers(crd.getModele() + " (" + crd.getCuttingPlanId() + ")");
            System.out.println("nserie : " + nserie);
            obj.setSerie(nserie + "");
            CuttingRequestSerieData newObj = cuttingRequestSerieDataService.save(obj);
        }
        // now by viewing the structure of Matlassage, SuiviMatelassage, Coupe, SuiviCoupe : we need to add in each one his own informatio depending on obj
        Matlassage mt = new Matlassage();
        mt.setNserie(nserie);
        mt.setNof(obj.getSequence());
        mt.setQuantite(obj.getQuantite());
        mt.setModele(obj.getDescription());
        mt.setDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        mt.setTablee("");
        mt.setLongueur(obj.getLongueur() + "");
        mt.setPlacement(obj.getPlacement());
        mt.setnCouches(obj.getNbrCouche() + "");
        mt.setLaLaizeDemande(obj.getLaize() + "");
        mt.setReftissu(obj.getPartNumberMaterial());
        mt.setDescription(obj.getDescription());
        mt.setReturnMagasin("0");
        mt.setMatMatlasseur1("");
        mt.setMatMatlasseur2("");
        mt.setMatMatlasseur3("");
        mt.setMatMatlasseur4("");
        mt.setHeure(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        int heure = LocalDateTime.now().getHour();
        if (heure >= 6 && heure < 14) {
            mt.setEquipe("B");
        } else if (heure >= 14 && heure < 22) {
            mt.setEquipe("C");
        } else {
            mt.setEquipe("A");
        }
        mt.setStatu("Non demarre");
        mt.setSens(obj.getMatelassageEndroit());
        mt.setMachine(obj.getMachine());
        if (crd != null) {
            mt.setDefinition(crd.getDefinition());
        } else {
            mt.setDefinition("");
        }
        if (crd != null && crd.getZone() != null) {
            mt.setAreaMatelassage(crd.getZone().getCode());
        } else {
            mt.setAreaMatelassage("1A");
        }
        matlassageRepository.save(mt);

        SuiviMatelassage sm = new SuiviMatelassage();
        //get the max id
        Long id = suiviMatelassageRepository.getMaxId();
        if (id == null) {
            id = 1L;
        } else {
            id++;
        }
        sm.setId(Long.valueOf(id));
        sm.setNserie(nserie + "");
        sm.setNof(obj.getSequence());
        sm.setModele(obj.getDescription());
        sm.setRefTissu(obj.getPartNumberMaterial());
        sm.setDesignation(obj.getDescription());
        sm.setTablee("");
        sm.setDateDebut("");
        sm.setFinIncomplet("");
        sm.setDebutIncomplet("");
        sm.setStatu("Non demarre");
        sm.setMachine(obj.getMachine());
        sm.setShift(mt.getEquipe());
        sm.setDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        sm.setTempsM("0");
        if (crd != null) {
            sm.setDefinition(crd.getDefinition());
        } else {
            sm.setDefinition("");
        }
        if (crd != null && crd.getZone() != null) {
            sm.setAreaSuiviMatelassage(crd.getZone().getCode());
        } else {
            sm.setAreaSuiviMatelassage("1A");
        }
        suiviMatelassageRepository.save(sm);

        Coupe cp = new Coupe();
        Long idCoupeMAx = coupeRepository.getMaxId();
        if (idCoupeMAx == null) {
            idCoupeMAx = 1L;
        } else {
            idCoupeMAx++;
        }
        cp.setId(idCoupeMAx);
        cp.setNof(obj.getSequence());
        cp.setNserie(nserie);
        cp.setDatedebut("");
//      ,[DateFin]
        cp.setDateFin("");

//      ,[drill1]
        String[] drills = obj.getDrill().split(",");
        if (drills.length < 1 || drills[0].isEmpty()) {
            cp.setDrill1("0");
        } else {
            cp.setDrill1(obj.getDrill().split(",")[0]);
        }
//      ,[drill2]
        if (drills.length < 2 || drills[1].isEmpty()) {
            cp.setDrill2("0");
        } else {
            cp.setDrill2(obj.getDrill().split(",")[1]);
        }
        cp.setOrigineX("");
        cp.setOrigineY("");
        cp.setPlacement(obj.getPlacement());
        cp.setConfiguration(obj.getConfig());
        cp.setMatricule("");
        cp.setMachine("");
        cp.setTempsCoupe("");
        cp.setStatut("Non demarre");
        cp.setDrill(cp.getDrill1() + "-" + cp.getDrill2());
        cp.setMatricule2("--");
        cp.setMachine2("--");
        coupeRepository.save(cp);

        SuiviCoupe sc = new SuiviCoupe();
        Long idSuiviCoupe = suiviCoupeRepository.getMaxId();
        if (idSuiviCoupe == null) {
            idSuiviCoupe = 1L;
        } else {
            idSuiviCoupe++;
        }

        sc.setId(idSuiviCoupe);
        sc.setNof(obj.getSequence());
        sc.setNserie(nserie + "");
        sc.setModele(obj.getDescription());
        sc.setRefTissu(obj.getPartNumberMaterial());
        sc.setDesignation(obj.getDescription());
        sc.setTablee("");
        sc.setDateDebut("");
        sc.setFinIncomplet("");
        sc.setDebutIncomplet("");
        sc.setDateFin("");
        sc.setStatu("Non demarre");
        sc.setMachine(obj.getMachine());
        sc.setTempsCoupe("");
        sc.setShift("");
        sc.setTempsArret("");
        sc.setPlacement(obj.getPlacement());
        sc.setLongueur(obj.getLongueur() + "");
        sc.setNbrCouches(obj.getNbrCouche() + "");
        sc.setDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        sc.setTempsCoupeBrute("0");
        sc.setTempsReactivite("0");
        sc.setType("Cycle");
        sc.setTablee2("--");
        sc.setShift2("--");
        sc.setDate2("");
        if (crd != null && crd.getZone() != null) {
            sc.setAreaSuiviCoupe(crd.getZone().getCode());
        } else {
            sc.setAreaSuiviCoupe("1A");
        }
        suiviCoupeRepository.save(sc);
        try{
        List<PokaYokeMain> pkArr = pokaYokeMainRepository.findByMarker(obj.getPlacement());
        PokaYokeMain pkObj = new PokaYokeMain();
        if(pkArr.size() == 1) {
            pkObj = pkArr.get(0);
            if(!obj.getPartNumberMaterial().equalsIgnoreCase(pkObj.getMaterial())) {
                pkObj.setMaterial(obj.getPartNumberMaterial());
                pokaYokeMainRepository.save(pkObj);
            }
        } else {
            if(pkArr.size() > 1) {
                pokaYokeMainRepository.deleteByMarker(obj.getPlacement());
            }
            pkObj.setDate(LocalDateTime.now());
            pkObj.setMaterial(obj.getPartNumberMaterial());
            pkObj.setMarker(obj.getPlacement());
            pokaYokeMainRepository.save(pkObj);
        }
        }catch (Exception e) {
            System.out.println("Modify serie problem : " + e.getMessage());
        }

        return new ResponseEntity<CuttingRequestSerieData>(obj, HttpStatus.CREATED);
    }

    @PostMapping("/modifierNbrCouche")
    @PreAuthorize("hasRole('QUALITE') or hasRole('ADMIN')")
    public ResponseEntity<?> modifierNbrCouche(
            @RequestParam(value = "serie", required = true) String serie,
            @RequestParam(value = "nbrCouche", required = true) Integer nbrCouche
    ) throws CloneNotSupportedException {
        CuttingRequestSerieData oldObj = cuttingRequestSerieDataService.findById(serie);
        String sequence = oldObj.getSequence();

        CuttingRequestSerieData newObj = cuttingRequestSerieDataService.save(oldObj);
        oldObj.setNbrCouche(nbrCouche);
        cuttingRequestSerieDataService.save(oldObj);

        // now by viewing the structure of Matlassage, SuiviMatelassage, Coupe, SuiviCoupe : we need to add in each one his own informatio depending on obj
        Matlassage oldmt = matlassageRepository.findByNserie(Long.parseLong(serie));
        oldmt.setnCouches(oldObj.getNbrCouche() + "");
        matlassageRepository.save(oldmt);

        SuiviCoupe oldsc = suiviCoupeRepository.findByNserie(serie);
        oldsc.setNbrCouches(nbrCouche + "");
        suiviCoupeRepository.save(oldsc);


        return new ResponseEntity<CuttingRequestSerieData>(oldObj, HttpStatus.CREATED);


    }

    @PostMapping("/diviser")
    @PreAuthorize("hasRole('QUALITE') or hasRole('ADMIN')")
    public ResponseEntity<?> diviser(
            @RequestParam(value = "serie", required = true) String serie,
            @RequestParam(value = "nbrCouche", required = true) Integer nbrCouche
    ) throws CloneNotSupportedException {
        CuttingRequestSerieData oldObj = cuttingRequestSerieDataService.findById(serie);
        CuttingRequestSerieData obj = (CuttingRequestSerieData) oldObj.clone();
        String sequence = oldObj.getSequence();
        CuttingRequestData crd = cuttingRequestDataService.findBySequence(sequence);
        Long nserie = matlassageRepository.getMaxNserie();
        if (nserie == null) {
            return new ResponseEntity<String>("Error getting nserie", HttpStatus.BAD_REQUEST);
        } else {
            nserie++;
        }

        if (crd != null) {
            obj.setStatusMatelassage("Waiting");
            obj.setDateDebutMatelassage(null);
            obj.setDateFinMatelassage(null);
            obj.setDateDebutCoupe(null);
            obj.setDateFinCoupe(null);
            obj.setStatusCoupe("Waiting");
            obj.setRetourMagasin(0.0);
            obj.setSerie(nserie + "");
            obj.setNbrCouche(obj.getNbrCouche() - nbrCouche);
            Integer countNbrPiece = ExcelHelper.getTotalEmp(obj.getPlacement());
            obj.setNbrPiece(countNbrPiece);
            if (obj.getNbrCouche() != null && obj.getNbrPiece() != null) {
                obj.setNbrPieceTotal((double) (obj.getNbrPiece() * obj.getNbrCouche()));
            }
            CuttingRequestSerieData newObj = cuttingRequestSerieDataService.save(obj);
            oldObj.setNbrCouche(nbrCouche);
            cuttingRequestSerieDataService.save(oldObj);
        }
        // now by viewing the structure of Matlassage, SuiviMatelassage, Coupe, SuiviCoupe : we need to add in each one his own informatio depending on obj
        Matlassage oldmt = matlassageRepository.findByNserie(Long.parseLong(serie));
        Matlassage mt = (Matlassage) oldmt.clone();
        mt.setNserie(nserie);
        mt.setDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        mt.setTablee("");
        mt.setnCouches(obj.getNbrCouche() + "");
        mt.setLaLaizeDemande(obj.getLaize() + "");
        mt.setReturnMagasin("0");
        mt.setMatMatlasseur1("");
        mt.setMatMatlasseur2("");
        mt.setMatMatlasseur3("");
        mt.setMatMatlasseur4("");
        mt.setHeure(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        mt.setStatu("Non demarre");
        matlassageRepository.save(mt);
        oldmt.setnCouches(oldObj.getNbrCouche() + "");
        matlassageRepository.save(oldmt);

        SuiviMatelassage sm = new SuiviMatelassage();
        //get the max id
        Long id = suiviMatelassageRepository.getMaxId();
        if (id == null) {
            id = 1L;
        } else {
            id++;
        }
        sm.setId(Long.valueOf(id));
        sm.setNserie(nserie + "");
        sm.setNof(obj.getSequence());
        sm.setModele(obj.getDescription());
        sm.setRefTissu(obj.getPartNumberMaterial());
        sm.setDesignation(obj.getDescription());
        sm.setTablee("");
        sm.setDateDebut("");
        sm.setFinIncomplet("");
        sm.setDebutIncomplet("");
        sm.setStatu("Non demarre");
        sm.setMachine(obj.getMachine());
        sm.setShift(mt.getEquipe());
        sm.setDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        sm.setTempsM("0");
        if (crd != null) {
            sm.setDefinition(crd.getDefinition());
        } else {
            sm.setDefinition("");
        }
        if (crd != null && crd.getZone() != null) {
            sm.setAreaSuiviMatelassage(crd.getZone().getCode());
        } else {
            sm.setAreaSuiviMatelassage("1A");
        }
        suiviMatelassageRepository.save(sm);

        Coupe cp = new Coupe();
        Long idCoupeMAx = coupeRepository.getMaxId();
        if (idCoupeMAx == null) {
            idCoupeMAx = 1L;
        } else {
            idCoupeMAx++;
        }
        cp.setId(idCoupeMAx);
        cp.setNof(obj.getSequence());
        cp.setNserie(nserie);
        cp.setDatedebut("");
//      ,[DateFin]
        cp.setDateFin("");

//      ,[drill1]
        String[] drills = obj.getDrill().split(",");
        if (drills.length < 1 || drills[0].isEmpty()) {
            cp.setDrill1("0");
        } else {
            cp.setDrill1(obj.getDrill().split(",")[0]);
        }
//      ,[drill2]
        if (drills.length < 2 || drills[1].isEmpty()) {
            cp.setDrill2("0");
        } else {
            cp.setDrill2(obj.getDrill().split(",")[1]);
        }
        cp.setOrigineX("");
        cp.setOrigineY("");
        cp.setPlacement(obj.getPlacement());
        cp.setConfiguration(obj.getConfig());
        cp.setMatricule("");
        cp.setMachine("");
        cp.setTempsCoupe("");
        cp.setStatut("Non demarre");
        cp.setDrill(cp.getDrill1() + "-" + cp.getDrill2());
        cp.setMatricule2("--");
        cp.setMachine2("--");
        coupeRepository.save(cp);

        SuiviCoupe sc = new SuiviCoupe();
        Long idSuiviCoupe = suiviCoupeRepository.getMaxId();
        if (idSuiviCoupe == null) {
            idSuiviCoupe = 1L;
        } else {
            idSuiviCoupe++;
        }

        sc.setId(idSuiviCoupe);
        sc.setNof(obj.getSequence());
        sc.setNserie(nserie + "");
        sc.setModele(obj.getDescription());
        sc.setRefTissu(obj.getPartNumberMaterial());
        sc.setDesignation(obj.getDescription());
        sc.setTablee("");
        sc.setDateDebut("");
        sc.setFinIncomplet("");
        sc.setDebutIncomplet("");
        sc.setDateFin("");
        sc.setStatu("Non demarre");
        sc.setMachine(obj.getMachine());
        sc.setTempsCoupe("");
        sc.setShift("");
        sc.setTempsArret("");
        sc.setPlacement(obj.getPlacement());
        sc.setLongueur(obj.getLongueur() + "");
        sc.setNbrCouches(obj.getNbrCouche() + "");
        sc.setDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        sc.setTempsCoupeBrute("0");
        sc.setTempsReactivite("0");
        sc.setType("Cycle");
        sc.setTablee2("--");
        sc.setShift2("--");
        sc.setDate2("");
        if (crd != null && crd.getZone() != null) {
            sc.setAreaSuiviCoupe(crd.getZone().getCode());
        } else {
            sc.setAreaSuiviCoupe("1A");
        }
        suiviCoupeRepository.save(sc);
        SuiviCoupe oldsc = suiviCoupeRepository.findByNserie(serie);
        oldsc.setNbrCouches(nbrCouche + "");
        suiviCoupeRepository.save(oldsc);

        return new ResponseEntity<CuttingRequestSerieData>(obj, HttpStatus.CREATED);


    }

    @Autowired
    private MarkerService markerService;

    @Autowired
    private MarkersOnlyCodeRepository markersOnlyCodeRepository;

    @Autowired
    private PartNumberMaterialConfigDataService partNumberMaterialConfigDataService;

    @PostMapping("/modify")
    @PreAuthorize("hasRole('CAD') or hasRole('ADMIN') or hasRole('CHEF_DE_ZONE') or hasRole('CHEF_EQUIPE') or hasRole('QUALITE')")
    public ResponseEntity<?> modify(
            @RequestBody CuttingRequestSerieData obj,
            @RequestParam(name = "maxNbrCouche", required = false) Integer maxNbrCouche,
            @RequestParam(name = "maxPlie", required = false) Integer maxPlie
    ) throws CloneNotSupportedException {
//        List<String> arr = partNumberMaterialConfigDataService.getPartNumberMaterial();
//        if(!arr.isEmpty() && arr.contains(obj.getPartNumberMaterial())) {
//            return new ResponseEntity<String>("Partnumber not found", HttpStatus.BAD_REQUEST);
//        }

        CuttingRequestSerieData oldObj = cuttingRequestSerieDataService.findById(obj.getSerie());


        oldObj.setPartNumberMaterial(obj.getPartNumberMaterial());
        oldObj.setDescription(obj.getDescription());
        oldObj.setMatelassageEndroit(obj.getMatelassageEndroit());
        oldObj.setLongueur(obj.getLongueur());
        oldObj.setNbrCouche(obj.getNbrCouche());
        oldObj.setLaize(obj.getLaize());
        oldObj.setPlacement(obj.getPlacement());
        oldObj.setConfig(obj.getConfig());
        oldObj.setDrill(obj.getDrill());
        oldObj.setMachine(obj.getMachine());
        CuttingRequestSerieData newObj = cuttingRequestSerieDataService.save(oldObj);
        Matlassage mt = matlassageRepository.findByNserie(Long.parseLong(obj.getSerie()));
        if (mt != null) {
            mt.setReftissu(obj.getPartNumberMaterial());
            mt.setDescription(obj.getDescription());
            mt.setPlacement(obj.getPlacement());
            mt.setMachine(obj.getMachine());
            mt.setnCouches(obj.getNbrCouche() + "");
            mt.setSens(obj.getMatelassageEndroit());
            mt.setLongueur(obj.getLongueur() + "");
            mt.setLaLaizeDemande(obj.getLaize() + "");
            mt = matlassageRepository.save(mt);
        }
        SuiviMatelassage sm = suiviMatelassageRepository.findByNserie(obj.getSerie());
        if (sm != null) {
            sm.setRefTissu(obj.getPartNumberMaterial());
            sm.setDesignation(obj.getDescription());
            sm.setMachine(obj.getMachine());
            sm = suiviMatelassageRepository.save(sm);
        }
        Coupe cp = coupeRepository.findFirstByNserie(Long.parseLong(obj.getSerie()));
        if (cp != null) {
            cp.setPlacement(obj.getPlacement());
            if (obj.getDrill().contains(",")) {
                String[] drills = obj.getDrill().split(",");
                if(drills.length > 0 && !drills[0].isEmpty()) {
                    cp.setDrill1(drills[0]);
                } else {
                    cp.setDrill1("0");
                }

                if(drills.length > 1 && !drills[1].isEmpty()) {
                    cp.setDrill2(drills[1]);
                } else {
                    cp.setDrill2("0");
                }

                cp.setDrill(cp.getDrill1()+"-"+cp.getDrill2());
            }
            cp.setConfiguration(obj.getConfig());
            coupeRepository.save(cp);
        }
        SuiviCoupe sc = suiviCoupeRepository.findByNserie(obj.getSerie());
        if (sc != null) {
            sc.setRefTissu(obj.getPartNumberMaterial());
            sc.setDesignation(obj.getDescription());
            sc.setMachine(obj.getMachine());
            sc.setPlacement(obj.getPlacement());
            sc.setLongueur(obj.getLongueur() + "");
            sc.setNbrCouches(obj.getNbrCouche() + "");
            sc = suiviCoupeRepository.save(sc);
        }
        markerService.updateMarker(obj.getPlacement(), obj.getLongueur(), obj);
        markersOnlyCodeRepository.deleteByCode(obj.getSerie());
        try {
            List<PokaYokeMain> pkArr = pokaYokeMainRepository.findByMarker(obj.getPlacement());
            PokaYokeMain pkObj = new PokaYokeMain();
            if (pkArr.size() == 1) {
                pkObj = pkArr.get(0);
                if (!obj.getPartNumberMaterial().equalsIgnoreCase(pkObj.getMaterial())) {
                    pkObj.setMaterial(obj.getPartNumberMaterial());
                    pokaYokeMainRepository.save(pkObj);
                }
            } else {
                if (pkArr.size() > 1) {
                    pokaYokeMainRepository.deleteByMarker(obj.getPlacement());
                }
                pkObj.setDate(LocalDateTime.now());
                pkObj.setMaterial(obj.getPartNumberMaterial());
                pkObj.setMarker(obj.getPlacement());
                pokaYokeMainRepository.save(pkObj);
            }
        }catch (Exception e) {
            System.out.println("Modify serie problem : " + e.getMessage());
        }

//        try{
            List<SequenceDetails> arrSequenceDetails = sequenceDetailsRepository.findBySerialNumber(newObj.getSerie());
            SequenceDetails sd = new SequenceDetails();
            if(arrSequenceDetails.size() > 1) {
                sequenceDetailsRepository.deleteAll(arrSequenceDetails);
            } else if(arrSequenceDetails.size() == 1) {
                sd = arrSequenceDetails.get(0);
                sd.setSequence(newObj.getSequence());
                sd.setMarker(newObj.getPlacement());
                sd.setMaterialPartNumber(obj.getPartNumberMaterial());
                if(sd.getCreatedAt() == null) {
                    sd.setCreatedAt(LocalDateTime.now());
                }
                sd.setUpdatedAt(LocalDateTime.now());
                sequenceDetailsRepository.save(sd);
            } else {
                sd.setSerialNumber(newObj.getSerie());
                sd.setSequence(newObj.getSequence());
                sd.setMarker(newObj.getPlacement());
                sd.setMaterialPartNumber(newObj.getPartNumberMaterial());
                if(sd.getCreatedAt() == null) {
                    sd.setCreatedAt(LocalDateTime.now());
                }
                sequenceDetailsRepository.save(sd);
            }
//        } catch (Exception  e) {
//            System.out.println("Modif Serie  sequenceDetailsRepository problem : " + e.getMessage());
//        }
        String serie = newObj.getSerie();
        if (maxNbrCouche != null && maxPlie != null) {
            maxNbrCouche -= Math.min(maxPlie, maxNbrCouche);
            while (maxNbrCouche > 0) {
                CuttingRequestSerieData obj2 = (CuttingRequestSerieData) newObj.clone();
                String sequence = newObj.getSequence();
                CuttingRequestData crd = cuttingRequestDataService.findBySequence(sequence);
                Long nserie = matlassageRepository.getMaxNserie();
                if (nserie == null) {
                    nserie = cuttingRequestSerieDataService.getMaxNserie();
                }
                if (nserie == null) {
                    nserie = (long) LocalDateTime.now().getYear() * 1000000;
                }
                nserie++;
                int nbrCouche = Math.min(maxPlie, maxNbrCouche);
                if (crd != null) {

                    obj2.setStatusMatelassage("Waiting");
                    obj2.setDateDebutMatelassage(null);
                    obj2.setDateFinMatelassage(null);
                    obj2.setDateDebutCoupe(null);
                    obj2.setDateFinCoupe(null);
                    obj2.setStatusCoupe("Waiting");
                    obj2.setRetourMagasin(0.0);
                    obj2.setSerie(nserie + "");
                    obj2.setNbrCouche(nbrCouche);
                    Integer countNbrPiece = ExcelHelper.getTotalEmp(obj2.getPlacement());
                    obj2.setNbrPiece(countNbrPiece);
                    if (obj2.getNbrCouche() != null && obj2.getNbrPiece() != null) {
                        obj2.setNbrPieceTotal((double) (obj2.getNbrPiece() * obj2.getNbrCouche()));
                    }
                    cuttingRequestSerieDataService.save(obj2);
                }

                Matlassage mtNew = (Matlassage) mt.clone();
                mtNew.setNserie(nserie);
                mtNew.setDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                mtNew.setTablee("");
                mtNew.setnCouches(nbrCouche + "");
                mtNew.setLaLaizeDemande(newObj.getLaize() + "");
                mtNew.setReturnMagasin("0");
                mtNew.setMatMatlasseur1("");
                mtNew.setMatMatlasseur2("");
                mtNew.setMatMatlasseur3("");
                mtNew.setMatMatlasseur4("");
                mtNew.setHeure(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
                mtNew.setStatu("Non demarre");
                matlassageRepository.save(mtNew);

                SuiviMatelassage smNew = new SuiviMatelassage();
                //get the max id
                Long id = suiviMatelassageRepository.getMaxId();
                if (id == null) {
                    id = 1L;
                } else {
                    id++;
                }
                smNew.setId(Long.valueOf(id));
                smNew.setNserie(nserie + "");
                smNew.setNof(newObj.getSequence());
                smNew.setModele(newObj.getDescription());
                smNew.setRefTissu(newObj.getPartNumberMaterial());
                smNew.setDesignation(newObj.getDescription());
                smNew.setTablee("");
                smNew.setDateDebut("");
                smNew.setFinIncomplet("");
                smNew.setDebutIncomplet("");
                smNew.setStatu("Non demarre");
                smNew.setMachine(newObj.getMachine());
                smNew.setShift(mt.getEquipe());
                smNew.setDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                smNew.setTempsM("0");
                if (crd != null) {
                    smNew.setDefinition(crd.getDefinition());
                } else {
                    smNew.setDefinition("");
                }
                if (crd != null && crd.getZone() != null) {
                    smNew.setAreaSuiviMatelassage(crd.getZone().getCode());
                } else {
                    smNew.setAreaSuiviMatelassage("1A");
                }
                suiviMatelassageRepository.save(smNew);

                Coupe cpNew = new Coupe();
                Long idCoupeMAx = coupeRepository.getMaxId();
                if (idCoupeMAx == null) {
                    idCoupeMAx = 1L;
                } else {
                    idCoupeMAx++;
                }
                cpNew.setId(idCoupeMAx);
                cpNew.setNof(newObj.getSequence());
                cpNew.setNserie(nserie);
                cpNew.setDatedebut("");
//      ,[DateFin]
                cpNew.setDateFin("");

//      ,[drill1]
                String[] drills = newObj.getDrill().split(",");
                if (drills.length < 1 || drills[0].isEmpty()) {
                    cpNew.setDrill1("0");
                } else {
                    cpNew.setDrill1(newObj.getDrill().split(",")[0]);
                }
//      ,[drill2]
                if (drills.length < 2 || drills[1].isEmpty()) {
                    cpNew.setDrill2("0");
                } else {
                    cpNew.setDrill2(newObj.getDrill().split(",")[1]);
                }
                cpNew.setOrigineX("");
                cpNew.setOrigineY("");
                cpNew.setPlacement(newObj.getPlacement());
                cpNew.setConfiguration(newObj.getConfig());
                cpNew.setMatricule("");
                cpNew.setMachine("");
                cpNew.setTempsCoupe("");
                cpNew.setStatut("Non demarre");
                cpNew.setDrill(cpNew.getDrill1() + "-" + cpNew.getDrill2());
                cpNew.setMatricule2("--");
                cpNew.setMachine2("--");
                coupeRepository.save(cpNew);

                SuiviCoupe scNew = new SuiviCoupe();
                Long idSuiviCoupe = suiviCoupeRepository.getMaxId();
                if (idSuiviCoupe == null) {
                    idSuiviCoupe = 1L;
                } else {
                    idSuiviCoupe++;
                }

                scNew.setId(idSuiviCoupe);
                scNew.setNof(newObj.getSequence());
                scNew.setNserie(nserie + "");
                scNew.setModele(newObj.getDescription());
                scNew.setRefTissu(newObj.getPartNumberMaterial());
                scNew.setDesignation(newObj.getDescription());
                scNew.setTablee("");
                scNew.setDateDebut("");
                scNew.setFinIncomplet("");
                scNew.setDebutIncomplet("");
                scNew.setDateFin("");
                scNew.setStatu("Non demarre");
                scNew.setMachine(newObj.getMachine());
                scNew.setTempsCoupe("");
                scNew.setShift("");
                scNew.setTempsArret("");
                scNew.setPlacement(newObj.getPlacement());
                scNew.setLongueur(newObj.getLongueur() + "");
                scNew.setNbrCouches(newObj.getNbrCouche() + "");
                scNew.setDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                scNew.setTempsCoupeBrute("0");
                scNew.setTempsReactivite("0");
                scNew.setType("Cycle");
                scNew.setTablee2("--");
                scNew.setShift2("--");
                scNew.setDate2("");
                if (crd != null && crd.getZone() != null) {
                    scNew.setAreaSuiviCoupe(crd.getZone().getCode());
                } else {
                    scNew.setAreaSuiviCoupe("1A");
                }
                suiviCoupeRepository.save(sc);

                maxNbrCouche -= Math.min(maxPlie, maxNbrCouche);
            }

        }


        return new ResponseEntity<CuttingRequestSerieData>(newObj, HttpStatus.CREATED);
    }
}
