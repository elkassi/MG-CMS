package com.lear.MGCMS.controller.CuttingRequest;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.lear.MGCMS.services.*;
import org.apache.commons.collections4.map.HashedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.CuttingSpeed;
import com.lear.MGCMS.domain.PartNumberInfo;
import com.lear.MGCMS.domain.Projet;
import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlan;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight2;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterial;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialPlacement;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestBox;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestBoxV2;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestPartNumber;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestPartNumberV2;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerie;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieInfo;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieV2;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestV2;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanLight2Repository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanRepository;
import com.lear.MGCMS.services.CuttingRequest.CuttingRequestSerieInfoService;
import com.lear.MGCMS.services.CuttingRequest.CuttingRequestService;
import com.lear.MGCMS.services.CuttingRequest.CuttingRequestV2Service;
import com.lear.MGCMS.services.ctc.GammeTechniqueImprimerService;
import com.lear.MGCMS.utils.ExcelHelper;
import com.lear.MGCMS.utils.UtilFunctions;
import com.lear.cms.domain.Coupe;
import com.lear.cms.domain.GammeTechniqueImprimer;
import com.lear.cms.domain.Matlassage;
import com.lear.cms.domain.ProduitFinit;
import com.lear.cms.repositories.CoupeRepository;
import com.lear.cms.repositories.MatlassageRepository;
import com.lear.cms.repositories.ProduitFinitRepository;

@RestController
@RequestMapping("/api/cuttingRequestV2")
public class CuttingRequestV2Controller {

    @Autowired
    private CuttingRequestV2Service service;
    @Autowired
    private CuttingRequestSerieInfoService cuttingRequestSerieInfoService;
    @Autowired
    private ProjetService projetService;

    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @Autowired
    private PartNumberInfoService partNumberInfoService;

    @Autowired
    private UserService userService;

    @Autowired
    private GammeTechniqueImprimerService gammeTechniqueImprimerService;

    @Autowired
    private MatlassageRepository matlassageRepository;
    @Autowired
    private CoupeRepository coupeRepository;
    @Autowired
    private CuttingSpeedService cuttingSpeedService;

    @Autowired
    private CuttingPlanLight2Repository cuttingPlanLight2Repository;

//	@GetMapping("/{date}/{shift}")
//	public List<CuttingRequest> findAll(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
//			@PathVariable String shift){
//		return service.findAll(date, shift);
//	}

    @GetMapping("/{sequence}")
    public ResponseEntity<?> findBySequence(@PathVariable String sequence) {
        CuttingRequestV2 cr = service.findBySequence(sequence);
        if (cr == null) {
            return new ResponseEntity<String>("sequence " + sequence + " not found", HttpStatus.BAD_REQUEST);
        }
        cr.setCmsId(cuttingPlanLight2Repository.findCmsIdById(cr.getCuttingPlanId()));
        return new ResponseEntity<CuttingRequestV2>(cr, HttpStatus.OK);
    }

    @GetMapping
    public List<CuttingRequestV2> findAll2(
            @RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "shift", required = false) String shift) {
        List<CuttingRequestV2> arr = service.findAll(date, shift);
        for (CuttingRequestV2 cr : arr) {
            cr.setCmsId(cuttingPlanLight2Repository.findCmsIdById(cr.getCuttingPlanId()));
        }
        return arr;
    }

    @PostMapping("/printSerie")
    public ResponseEntity<?> printTicket(@RequestBody List<CuttingRequestSerieInfo> arr, BindingResult result,
                                         Authentication authentication) throws IOException {

        String content = new String(Files.readAllBytes(Paths.get("C:\\cmsFolder\\tickets\\ticketSerie.prn")));
        User user = userService.findByUsername(authentication.getName());
        if (user == null || user.getIpPrinter() == null || user.getIpPrinter().trim().isEmpty()) {
            return new ResponseEntity<String>("Il faut demander au administrateur de vous ajouter l'ip d'imprimante ",
                    HttpStatus.BAD_REQUEST);
        }
        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_IMPORTER") || role.getName().equals("ROLE_ADMIN")) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        if (arr.size() > 0) {
            CuttingRequestSerieInfo crsi = arr.get(0);
            CuttingRequestV2 cr = service.findBySequence(crsi.getCuttingRequest().getSequence());
            Socket clientSocket;
            try {
                clientSocket = new Socket(user.getIpPrinter(), 9100);
                DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                for (CuttingRequestSerieInfo obj : arr) {
                    String contentTicket = content;
                    contentTicket = contentTicket.replaceAll("@modele", cr.getModele());
                    contentTicket = contentTicket.replaceAll("@sequence", cr.getSequence());
                    contentTicket = contentTicket.replaceAll("@reftissu", obj.getPartNumberMaterial());
                    contentTicket = contentTicket.replaceAll("@description", obj.getDescription());
                    contentTicket = contentTicket.replaceAll("@serie", obj.getSerie());
                    contentTicket = contentTicket.replaceAll("@placement", obj.getPlacement());
                    contentTicket = contentTicket.replaceAll("@kit", obj.getPartNumbers());
                    contentTicket = contentTicket.replaceAll("@longueur", obj.getLongueur() + "");
                    contentTicket = contentTicket.replaceAll("@nbrCouche", obj.getNbrCouche() + "");
                    contentTicket = contentTicket.replaceAll("@laize", obj.getLaize() + "");
                    contentTicket = contentTicket.replaceAll("@placement", obj.getPlacement());
                    contentTicket = contentTicket.replaceAll("@sens", obj.getMatelassageEndroit());
                    contentTicket = contentTicket.replaceAll("@config", obj.getConfig());
                    if (obj.getDrill() != null && obj.getDrill().length() > 0) {
                        String[] drill = obj.getDrill().split(",");
                        if (drill.length > 0 && drill[0] != null) {
                            contentTicket = contentTicket.replaceAll("@drill1", drill[0]);
                        } else {
                            contentTicket = contentTicket.replaceAll("@drill1", "");
                        }
                        if (drill.length > 1 && drill[1] != null) {
                            contentTicket = contentTicket.replaceAll("@drill2", drill[1]);
                        } else {
                            contentTicket = contentTicket.replaceAll("@drill2", "");
                        }
                    }
                    contentTicket = contentTicket.replaceAll("@machine", obj.getMachine());
                    contentTicket = contentTicket.replaceAll("@date",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    contentTicket = contentTicket.replaceAll("@pageNumb", obj.getInd() + "");
                    contentTicket = contentTicket.replaceAll("@totalPages", cr.getCuttingRequestSeries().size() + "");
                    System.out.println(contentTicket);
                    outToServer.writeBytes(contentTicket);
                }
                clientSocket.close();
                return new ResponseEntity<String>("Printed : " + arr.size(), HttpStatus.OK);
            } catch (IOException e) {
                return new ResponseEntity<String>("Printer problem", HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity<String>("list empty", HttpStatus.BAD_REQUEST);

    }

    @Autowired
    private QueryService queryService;


    @PostMapping
    public ResponseEntity<?> save(@RequestBody CuttingRequestV2 obj, BindingResult result,
                                  Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null)
            return errorMap;

        User user = userService.findByUsername(authentication.getName());

        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_IMPORTER")) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        boolean inCMS = true;
        List<String> woList = new ArrayList<String>();
        Map<String, Integer> woQty = new HashedMap<String, Integer>();
        for (CuttingRequestPartNumberV2 crpn : obj.getCuttingRequestPartNumbers()) {
            System.out.println("WO : " + crpn.getWo());
            woList.add(crpn.getWo());
            woQty.put(crpn.getWo(), crpn.getQuantity());
        }
        String sequenceCMS = null;
        if (obj.getSequence() == null) {
            List<String> arrSqPassed = new ArrayList<String>();
            List<String> gtArr = gammeTechniqueImprimerService.findSequenceByWOs(woList);
            System.out.println(woList.size() + " => " + gtArr.size());
            for (String sq : gtArr) {
                if (arrSqPassed.contains(sq)) {
                    continue;
                }
                CuttingRequestV2 crTest = service.findBySequence(sq);
                if (crTest != null) {
                    arrSqPassed.add(sq);
                    continue;
                }
                Map<String, Integer> woQty2 = new HashedMap<String, Integer>();
                for (GammeTechniqueImprimer gt : gammeTechniqueImprimerService.findBySequence(sq)) {
                    if (gt.getNbrImprissionImp().equals("1")) {
                        if (woQty2.get(gt.getNofImp()) != null) {
                            woQty2.put(gt.getNofImp(),
                                    woQty2.get(gt.getNofImp()) + Integer.parseInt(gt.getQuantiteImp()));
                        } else {
                            woQty2.put(gt.getNofImp(), Integer.parseInt(gt.getQuantiteImp()));
                        }
                    }
                }
                boolean equal = true;
                for (String key : woQty2.keySet()) {
                    if (!woQty2.get(key).equals(woQty.get(key))) {
                        equal = false;
                    }
                }
                if (!equal) {
                    arrSqPassed.add(sq);
                    continue;
                }
                sequenceCMS = sq;
                break;
            }
        } else {
            sequenceCMS = obj.getSequence();
        }

        if (sequenceCMS == null) {
            return new ResponseEntity<String>("CMS not found", HttpStatus.BAD_REQUEST);
        }
        System.out.println(sequenceCMS);
        List<GammeTechniqueImprimer> arrCMS = gammeTechniqueImprimerService.findBySequence(sequenceCMS);
        if (inCMS && sequenceCMS != null) {
            obj.setSequence(sequenceCMS);
        }

        List<CuttingRequestBoxV2> crbArr = new ArrayList<CuttingRequestBoxV2>();
        for (CuttingRequestPartNumberV2 crpn : obj.getCuttingRequestPartNumbers()) {
            PartNumberInfo pni = partNumberInfoService.findByPartNumber(crpn.getPartNumber());
            if (pni != null && pni.getPackageQty() != null) {
                crpn.setPackageQty(pni.getPackageQty());
            } else {
                Integer packaging = queryService.getPackaginFromCMS(crpn.getPartNumber());
                if (packaging != null) {
                    crpn.setPackageQty(packaging);
                } else {
                    crpn.setPackageQty(10);
                }
            }
            crpn.setCuttingRequest(obj);
        }

        if (inCMS) {
            for (GammeTechniqueImprimer gamme : arrCMS) {
                CuttingRequestBoxV2 crb = new CuttingRequestBoxV2();
                crb.setCuttingRequest(obj);
                crb.setId(gamme.getnSerieGammeImp() + "");
                crb.setPartNumber(gamme.getPartNumberImp());
                crb.setDescription(gamme.getDescriptionImp());
                crb.setItem(gamme.getCode3Imp());
                crb.setQtyBox(Integer.parseInt(gamme.getQuantiteImp()));
                crb.setWo(gamme.getNofImp());
                crb.setWoid(gamme.getWoidImp());
                crb.setNbrImpression(gamme.getNbrImprissionImp());
                crbArr.add(crb);
                System.out.println(gamme.getnSerieGammeImp());
            }
        }

//		if(obj.getCuttingRequestBoxs().size() == 0) {
        obj.setCuttingRequestBoxs(crbArr);
//		}

        LocalDateTime date = LocalDateTime.now();
        Integer ind = 1;
        List<CuttingRequestSerieV2> arrCrs = new ArrayList<CuttingRequestSerieV2>();
        for (Matlassage mt : matlassageRepository.findByNofOrderByNserie(sequenceCMS)) {
            CuttingRequestSerieV2 foundedCrs = new CuttingRequestSerieV2();
            for (CuttingRequestSerieV2 crsTest : obj.getCuttingRequestSeries()) {
                if (crsTest.getPlacement().equalsIgnoreCase(mt.getPlacement())) {
                    foundedCrs = crsTest;
                    break;
                }
            }
            CuttingRequestSerieV2 crs = new CuttingRequestSerieV2();
            crs.setPlacement(mt.getPlacement());
            crs.setSerie(mt.getNserie() + "");
            crs.setPartNumberMaterial(mt.getReftissu());
            crs.setDescription(mt.getDescription());
            crs.setLongueur(Double.parseDouble(mt.getLongueur()));
            crs.setQuantite(mt.getQuantite());
            crs.setPartNumbers(mt.getModele());
            crs.setGroupPlacement(1);
            crs.setActivated(true);
            crs.setMachine(mt.getMachine());

            Coupe coupe = coupeRepository.findFirstByNserie(Long.parseLong(crs.getSerie()));
            if (coupe != null) {
                crs.setDrill(coupe.getDrill1() + "," + coupe.getDrill2());
                crs.setConfig(coupe.getConfiguration());
            }
            if (foundedCrs != null) {
                crs.setMaxPlie(foundedCrs.getMaxPlie());
                crs.setMaxDrill(foundedCrs.getMaxDrill());
                crs.setMaxPlieDrill(foundedCrs.getMaxPlieDrill());
                crs.setPerimetre(foundedCrs.getPerimetre());
                crs.setTempsDeCoupe(foundedCrs.getTempsDeCoupe());
            }
            if (crs.getPerimetre() == null || crs.getPerimetre() == 0.0) {
                crs.setPerimetre(ExcelHelper.getPerimetre(crs.getPlacement()));
            }
            if (crs.getPerimetre() != null && (crs.getTempsDeCoupe() == null || crs.getTempsDeCoupe() == 0.0)) {
                CuttingSpeed speed = cuttingSpeedService.findById(crs.getConfig());
                System.out.println(mt.getPlacement() + " speed : " + speed);
                if (speed != null) {
                    crs.setTempsDeCoupe(
                            UtilFunctions.convertTwoDigit(crs.getPerimetre() / (speed.getVitesse() * 100), 5));
                } else {
                    crs.setTempsDeCoupe(UtilFunctions.convertTwoDigit(crs.getPerimetre() / 300, 5));
                }
            }
            crs.setMatelassageEndroit(mt.getSens());
            crs.setNbrCouche(Integer.parseInt(mt.getnCouches()));
            crs.setPlacement(mt.getPlacement());
            crs.setLaize(Double.parseDouble(mt.getLaLaizeDemande()));
            crs.setCreatedAt(date);
            crs.setPlanningDate(obj.getPlanningDate());
            crs.setShift(obj.getShift());
            crs.setInd(ind);

            arrCrs.add(crs);
            crs.setCuttingRequest(obj);
            ind += 1;
        }
        obj.setCuttingRequestSeries(arrCrs);
        if (user != null && obj.getCreatedBy() == null) {
            obj.setCreatedAt(date);
            obj.setCreatedBy(user);
        } else {

        }

        CuttingRequestV2 newObj = service.save(obj);
        return new ResponseEntity<CuttingRequestV2>(newObj, HttpStatus.CREATED);
    }

    @PostMapping("/add-box")
    public ResponseEntity<?> addBox(@RequestParam(value = "sequence", required = false) String sequence,
                                    @RequestParam(value = "pn", required = false) String pn,
                                    Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        CuttingRequestV2 obj = service.findBySequence(sequence);
        CuttingRequestBoxV2 box = null;
        for (CuttingRequestBoxV2 crb : obj.getCuttingRequestBoxs()) {
            if (crb.getPartNumber().equalsIgnoreCase(pn)) {
                box = crb;
            }
        }
        CuttingRequestBoxV2 newBox = new CuttingRequestBoxV2();
        newBox.setPartNumber(box.getPartNumber());
        newBox.setDescription(box.getDescription());
        newBox.setItem(box.getItem());
        newBox.setWo(box.getWo());
        newBox.setWoid(box.getWoid());
        newBox.setQtyBox(box.getQtyBox());
        newBox.setGammePrinted(box.getGammePrinted());
        newBox.setNbrImpression("2");
        GammeTechniqueImprimer gti = null;
        for (GammeTechniqueImprimer gamme : gammeTechniqueImprimerService.findBySequence(sequence)) {
            if (gamme.getPartNumberImp().equalsIgnoreCase(pn)) {
                gti = new GammeTechniqueImprimer(gammeTechniqueImprimerService.findMaxId() + 1, gamme.getTitreImp(),
                        gamme.getCode1Imp(), gamme.getCode3Imp(), gamme.getCode5Imp(), gamme.getPartNumberImp(),
                        gamme.getDescriptionImp(), gamme.getElaborerparImp(), gamme.getDateElaborationImp(),
                        gamme.getValiderparImp(), gamme.getModifierParImp(), gamme.getDateModificationImp(),
                        gamme.getValiderModParImp(), gamme.getShiftImp(), gamme.getDateImprissionImp(),
                        gamme.getDateRechercheImp(), gamme.getUserNameImp(), gamme.getIdGamme1(), "Duplicata",
                        gamme.getSupplierKitImp(), gamme.getSiteImp(), gamme.getIndiceLabelImp(),
                        gamme.getColorLabelImp(), gamme.getCustomerPN_LabelImp(), gamme.getJlr_PNLabelImp(),
                        gamme.getqLEvelLabelImp(), gamme.getXatnLabelImp(), gamme.getDescriptionLabelImp(),
                        gamme.getSoulignerLabelImp(), gamme.getMatriculeLavelImp(), gamme.getnSerieGammeImp(),
                        gamme.getnSequenceImp(), gamme.getNofImp(), gamme.getWoidImp(), gamme.getEcnImp(),
                        gamme.getPackagingImp(), gamme.getQuantiteImp(), "2");
                break;
            }
        }

        Integer serie = null;
        Integer max = gammeTechniqueImprimerService.findMaxSerie();
        if (max != null && gti != null) {
            serie = (max) + 1;
            gti.setnSerieGammeImp(serie);
            gammeTechniqueImprimerService.save(gti, user);
            newBox.setId(serie + "");
            newBox.setCuttingRequest(obj);
            obj.getCuttingRequestBoxs().add(newBox);
            CuttingRequestV2 newObj = service.save(obj);
            return new ResponseEntity<CuttingRequestV2>(newObj, HttpStatus.CREATED);

        }
        return new ResponseEntity<String>("bad", HttpStatus.BAD_REQUEST);

    }

    @PostMapping("/add-box2")
    public ResponseEntity<?> addBoxV2(@RequestParam(value = "sequence", required = false) String sequence,
                                      @RequestParam(value = "pn", required = false) String pn,
                                      Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        GammeTechniqueImprimer gti = null;
        for (GammeTechniqueImprimer gamme : gammeTechniqueImprimerService.findBySequence(sequence)) {
            if (gamme.getPartNumberImp().equalsIgnoreCase(pn)) {
                gti = new GammeTechniqueImprimer(gammeTechniqueImprimerService.findMaxId() + 1, gamme.getTitreImp(),
                        gamme.getCode1Imp(), gamme.getCode3Imp(), gamme.getCode5Imp(), gamme.getPartNumberImp(),
                        gamme.getDescriptionImp(), gamme.getElaborerparImp(), gamme.getDateElaborationImp(),
                        gamme.getValiderparImp(), gamme.getModifierParImp(), gamme.getDateModificationImp(),
                        gamme.getValiderModParImp(), gamme.getShiftImp(), gamme.getDateImprissionImp(),
                        gamme.getDateRechercheImp(), gamme.getUserNameImp(), gamme.getIdGamme1(), "Duplicata",
                        gamme.getSupplierKitImp(), gamme.getSiteImp(), gamme.getIndiceLabelImp(),
                        gamme.getColorLabelImp(), gamme.getCustomerPN_LabelImp(), gamme.getJlr_PNLabelImp(),
                        gamme.getqLEvelLabelImp(), gamme.getXatnLabelImp(), gamme.getDescriptionLabelImp(),
                        gamme.getSoulignerLabelImp(), gamme.getMatriculeLavelImp(), gamme.getnSerieGammeImp(),
                        gamme.getnSequenceImp(), gamme.getNofImp(), gamme.getWoidImp(), gamme.getEcnImp(),
                        gamme.getPackagingImp(), gamme.getQuantiteImp(), "2");
                break;
            }
        }

        Integer serie = null;
        Integer max = gammeTechniqueImprimerService.findMaxSerie();
        if (max != null && gti != null) {
            serie = (max) + 1;
            gti.setnSerieGammeImp(serie);
            gammeTechniqueImprimerService.save(gti, user);
            return new ResponseEntity<GammeTechniqueImprimer>(gti, HttpStatus.CREATED);

        }
        return new ResponseEntity<String>("bad", HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/split")
    @PreAuthorize("hasRole('ADMIN') or hasRole('IMPORTER')")
    public ResponseEntity<?> split(@RequestParam(value = "serie", required = false) String serie,
                                   @RequestParam(value = "qty", required = false) Integer qty, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        GammeTechniqueImprimer gti = null;
        GammeTechniqueImprimer gti2 = null;
        GammeTechniqueImprimer gamme = gammeTechniqueImprimerService.findByNSerieGammeImp(Integer.parseInt(serie));
        if (gamme == null || gamme.getQuantiteImp() == null || Integer.parseInt(gamme.getQuantiteImp()) < qty) {
            return new ResponseEntity<String>("bad", HttpStatus.BAD_REQUEST);
        }
        if (gamme != null) {
            Long maxId = gammeTechniqueImprimerService.findMaxId();
            gti = new GammeTechniqueImprimer(maxId + 1, gamme.getTitreImp(), gamme.getCode1Imp(), gamme.getCode3Imp(),
                    gamme.getCode5Imp(), gamme.getPartNumberImp(), gamme.getDescriptionImp(), gamme.getElaborerparImp(),
                    gamme.getDateElaborationImp(), gamme.getValiderparImp(), gamme.getModifierParImp(),
                    gamme.getDateModificationImp(), gamme.getValiderModParImp(), gamme.getShiftImp(),
                    gamme.getDateImprissionImp(), gamme.getDateRechercheImp(), gamme.getUserNameImp(),
                    gamme.getIdGamme1(), "Split", gamme.getSupplierKitImp(), gamme.getSiteImp(),
                    gamme.getIndiceLabelImp(), gamme.getColorLabelImp(), gamme.getCustomerPN_LabelImp(),
                    gamme.getJlr_PNLabelImp(), gamme.getqLEvelLabelImp(), gamme.getXatnLabelImp(),
                    gamme.getDescriptionLabelImp(), gamme.getSoulignerLabelImp(), gamme.getMatriculeLavelImp(),
                    gamme.getnSerieGammeImp(), gamme.getnSequenceImp(), gamme.getNofImp(), gamme.getWoidImp(),
                    gamme.getEcnImp(), gamme.getPackagingImp(), qty + "", "2");
            gti2 = new GammeTechniqueImprimer(maxId + 2, gamme.getTitreImp(), gamme.getCode1Imp(), gamme.getCode3Imp(),
                    gamme.getCode5Imp(), gamme.getPartNumberImp(), gamme.getDescriptionImp(), gamme.getElaborerparImp(),
                    gamme.getDateElaborationImp(), gamme.getValiderparImp(), gamme.getModifierParImp(),
                    gamme.getDateModificationImp(), gamme.getValiderModParImp(), gamme.getShiftImp(),
                    gamme.getDateImprissionImp(), gamme.getDateRechercheImp(), gamme.getUserNameImp(),
                    gamme.getIdGamme1(), "Split", gamme.getSupplierKitImp(), gamme.getSiteImp(),
                    gamme.getIndiceLabelImp(), gamme.getColorLabelImp(), gamme.getCustomerPN_LabelImp(),
                    gamme.getJlr_PNLabelImp(), gamme.getqLEvelLabelImp(), gamme.getXatnLabelImp(),
                    gamme.getDescriptionLabelImp(), gamme.getSoulignerLabelImp(), gamme.getMatriculeLavelImp(),
                    gamme.getnSerieGammeImp(), gamme.getnSequenceImp(), gamme.getNofImp(), gamme.getWoidImp(),
                    gamme.getEcnImp(), gamme.getPackagingImp(), (Integer.parseInt(gamme.getQuantiteImp()) - qty) + "",
                    "2");
        }

        Integer max = gammeTechniqueImprimerService.findMaxSerie();
        if (max != null && gti != null) {
            gti.setnSerieGammeImp((max) + 1);
            gammeTechniqueImprimerService.save(gti, user);
            gti2.setnSerieGammeImp((max) + 2);
            gammeTechniqueImprimerService.save(gti2, user);
            return new ResponseEntity<GammeTechniqueImprimer>(gti, HttpStatus.CREATED);

        }
        return new ResponseEntity<String>("bad", HttpStatus.BAD_REQUEST);
    }

    @Autowired
    private ProduitFinitRepository produitFinitRepository;
    @Autowired
    private CuttingPlanRepository cuttingPlanRepository;
    @Autowired
    private ZoneRepository zoneRepository;


    @PostMapping("/refresh/{sequence}")
    public CuttingRequestV2 refresh(@PathVariable String sequence) {
        //try {
//			CuttingRequestV2 obj = cuttingRequestV2Service.findBySequence(sequence);
//			if (obj != null)
//				continue;
        List<ProduitFinit> arrPf = produitFinitRepository.findBySequence(sequence);
        CuttingRequestV2 obj = service.findBySequence(sequence);
        if (obj == null) {
            obj = new CuttingRequestV2();
            System.out.println("CuttingPlan : " + Long.parseLong(arrPf.get(0).getIdPlanProduiFinit()));
            List<CuttingPlan> cpArr = cuttingPlanRepository
                    .findByCMSId(Long.parseLong(arrPf.get(0).getIdPlanProduiFinit()));
            CuttingPlan cp = null;
            if(!cpArr.isEmpty()) {
                cp = cpArr.get(0);
            }

            Map<String, Double> tempCoupeMap = new HashedMap<String, Double>();
            Map<String, Double> perimetreMap = new HashedMap<String, Double>();
            Map<String, Integer> nbrPieceMap = new HashedMap<String, Integer>();

            if (cp != null) {
                for (CuttingPlanMaterial cpm : cp.getCuttingPlanMaterials()) {
                    for (CuttingPlanMaterialPlacement cpmp : cpm.getCuttingPlanMaterialPlacement()) {
                        if (cpmp.getPlacement() != null) {
                            if (cpmp.getPerimetre() != null && cpmp.getTempsDeCoupe() > 0
                                    && cpmp.getPerimetre() > 0) {
                                perimetreMap.put(cpmp.getPlacement(), cpmp.getPerimetre());
                            } else {
                                perimetreMap.put(cpmp.getPlacement(),
                                        ExcelHelper.getPerimetre(cpmp.getPlacement()));
                            }
                            if(!nbrPieceMap.containsKey(cpmp.getPlacement())) {
                                Integer countNbrPiece = ExcelHelper.getTotalEmp(cpmp.getPlacement());
                                if(countNbrPiece != null) {
                                    nbrPieceMap.put(cpmp.getPlacement(), countNbrPiece);
                                } else {
                                    nbrPieceMap.put(cpmp.getPlacement(), 0);
                                }
                            }

                            if (cpmp.getTempsDeCoupe() != null && cpmp.getTempsDeCoupe() > 0) {
                                tempCoupeMap.put(cpmp.getPlacement(), cpmp.getTempsDeCoupe());
                            }

                        }
                    }
                }
            }
            obj.setSequence(sequence);
            obj.setCuttingPlanId(cp.getId());
            obj.setProjet(cp.getProjet());
            obj.setVersion(cp.getVersion());
            obj.setModele(cp.getDescription());
            obj.setDefinition(cp.getDefinition());
            Projet projet = projetService.findByObjId(cp.getProjet());
            if (projet != null && projet.getZone() != null) {
                obj.setZone(projet.getZone());
            } else {
                obj.setZone(zoneRepository.findByCode(arrPf.get(0).getAreaProduitFinit()));
            }
            List<CuttingRequestPartNumberV2> cppnArr = new ArrayList<CuttingRequestPartNumberV2>();
            for (ProduitFinit pf : arrPf) {
                CuttingRequestPartNumberV2 pn = new CuttingRequestPartNumberV2();
                pn.setDescription(pf.getDesiProdFinit());
                pn.setPartNumber(pf.getRefProdFinit());
                pn.setItem(pf.getRefProdSemi());
                pn.setQuantity(Integer.parseInt(pf.getQtyTotalPartNumber()));
                pn.setWo(pf.getNoff());
                pn.setWoid(pf.getWoid());
                pn.setCuttingRequest(obj);
                cppnArr.add(pn);
            }
            obj.setCuttingRequestPartNumbers(cppnArr);

            List<CuttingRequestSerieV2> crsArr = new ArrayList<CuttingRequestSerieV2>();
            for (Matlassage mt : matlassageRepository.findByNofOrderByNserie(sequence)) {
                try {


                    Coupe coupe = coupeRepository.findFirstByNserie(mt.getNserie());
                    System.out.println(mt.getNserie());
                    CuttingRequestSerieV2 crs = new CuttingRequestSerieV2();
                    crs.setSerie(mt.getNserie() + "");
                    crs.setPartNumberMaterial(mt.getReftissu());
                    crs.setDescription(mt.getDescription());
                    crs.setMatelassageEndroit(mt.getSens());
                    crs.setLongueur(Double.parseDouble(mt.getLongueur()));
                    crs.setQuantite(mt.getQuantite());
                    crs.setPartNumbers(mt.getModele());
                    crs.setGroupPlacement(1);
                    crs.setActivated(true);
                    crs.setMachine(mt.getMachine());
                    if (mt.getReturnMagasin() != null)
                        crs.setRetourMagasin(Double.parseDouble(mt.getReturnMagasin()));
                    crs.setMaxDrill(null);
                    crs.setMaxPlie(null);
                    crs.setMaxPlieDrill(null);
                    crs.setNbrCouche(Integer.parseInt(mt.getnCouches()));
                    crs.setPlacement(mt.getPlacement());
                    crs.setLaize(Double.parseDouble(mt.getLaLaizeDemande()));
                    crs.setConfig(coupe.getConfiguration());
                    crs.setDrill(coupe.getDrill1() + "," + coupe.getDrill2());

                    crs.setPerimetre(perimetreMap.get(crs.getPlacement()));
                    crs.setNbrPiece(nbrPieceMap.getOrDefault(crs.getPlacement(), 0));
                    if(crs.getNbrCouche() != null && crs.getNbrPiece() != null) {
                        crs.setNbrPieceTotal((double) (crs.getNbrPiece() * crs.getNbrCouche()));
                    }

                    if (tempCoupeMap.containsKey(crs.getPlacement()) && tempCoupeMap.get(crs.getPlacement()) > 0) {
                        crs.setTempsDeCoupe(tempCoupeMap.get(crs.getPlacement()));
                    } else {
                        CuttingSpeed speed = cuttingSpeedService.findById(crs.getConfig());
                        if (crs.getPerimetre() != null) {
                            if (speed != null) {
                                crs.setTempsDeCoupe(UtilFunctions
                                        .convertTwoDigit(crs.getPerimetre() / (speed.getVitesse() * 100), 5));
                            } else {
                                crs.setTempsDeCoupe(UtilFunctions.convertTwoDigit(crs.getPerimetre() / 300, 5));
                            }
                        }
                    }

                    crs.setCuttingRequest(obj);
                    if (obj.getPlanningDate() == null || obj.getShift() == null) {
                        obj.setPlanningDate(
                                LocalDate.parse(mt.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                        LocalTime time = LocalTime.parse(mt.getHeure(), DateTimeFormatter.ofPattern("HH:mm"));
                        time.plusHours(2);
                        int hour = time.getHour();
                        if (hour < 8 && hour >= 0) {
                            obj.setShift("1");
                        } else if (hour < 16 && hour >= 8) {
                            obj.setShift("2");
                        } else {
                            obj.setShift("3");
                        }
                    }
                    crs.setPlanningDate(obj.getPlanningDate());
                    crs.setShift(obj.getShift());

                    if (coupe.getDatedebut() != null && !coupe.getDatedebut().trim().isEmpty())
                        crs.setDateDebutCoupe(LocalDateTime.parse(coupe.getDatedebut(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    if (coupe.getDateFin() != null && !coupe.getDateFin().trim().isEmpty())
                        crs.setDateFinCoupe(LocalDateTime.parse(coupe.getDateFin(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                    if (mt.getStatu().equalsIgnoreCase("complet")) {
                        crs.setStatusMatelassage("Complete");
                    } else if (mt.getStatu().equalsIgnoreCase("incomplet")) {
                        crs.setStatusMatelassage("Incomplete");
                    } else if (mt.getStatu().equalsIgnoreCase("Non demarre")) {
                        crs.setStatusMatelassage("Waiting");
                    } else {
                        crs.setStatusMatelassage("In progress");
                    }

                    if (coupe.getStatut().equalsIgnoreCase("complet")) {
                        crs.setStatusCoupe("Complete");
                    } else if (coupe.getStatut().equalsIgnoreCase("incomplet")) {
                        crs.setStatusCoupe("Incomplete");
                    } else if (coupe.getStatut().equalsIgnoreCase("Non demarre")) {
                        if (crs.getDateDebutCoupe() != null && crs.getDateFinCoupe() == null) {
                            crs.setStatusCoupe("In progress");
                        } else {
                            crs.setStatusCoupe("Waiting");
                        }
                    }
                    crs.setTableMatelassage(mt.getTablee());
                    crs.setTableCoupe(coupe.getMachine());
                    crs.setMatelasseur1(mt.getMatMatlasseur1());
                    crs.setMatelasseur2(mt.getMatMatlasseur2());
                    crs.setMatelasseur3(mt.getMatMatlasseur3());
                    crs.setMatelasseur4(mt.getMatMatlasseur4());

                    crs.setCoupeur1(coupe.getMatricule());
                    crsArr.add(crs);
                } catch (Exception e) {
                    System.out.println("Error CRS : " + mt.getNserie() + " : " + e.getMessage());
                }
            }
            obj.setCuttingRequestSeries(crsArr);
            List<CuttingRequestBoxV2> crbArr = new ArrayList<CuttingRequestBoxV2>();
            for (GammeTechniqueImprimer gt : gammeTechniqueImprimerService.findBySequence(sequence)) {
                CuttingRequestBoxV2 crb = new CuttingRequestBoxV2();
                crb.setId((gt.getnSerieGammeImp()) + "");
                crb.setPartNumber(gt.getPartNumberImp());
                crb.setDescription(gt.getDescriptionImp());
                crb.setItem(gt.getCode3Imp());
                crb.setWo(gt.getNofImp());
                crb.setWoid(gt.getWoidImp());
                crb.setQtyBox(Integer.parseInt(gt.getQuantiteImp()));
                crb.setCuttingRequest(obj);
                crbArr.add(crb);
            }
            obj.setCuttingRequestBoxs(crbArr);
            CuttingRequestV2 newObj = service.save(obj);
            return newObj;

        } else {
            List<CuttingPlan> cpArr = cuttingPlanRepository
                    .findByCMSId(Long.parseLong(arrPf.get(0).getIdPlanProduiFinit()));
            CuttingPlan cp = null;
            if(!cpArr.isEmpty()) {
                cp = cpArr.get(0);
            }
            Map<String, Double> tempCoupeMap = new HashedMap<String, Double>();
            Map<String, Double> perimetreMap = new HashedMap<String, Double>();
            Map<String, Integer> nbrPieceMap = new HashedMap<String, Integer>();

            for (CuttingPlanMaterial cpm : cp.getCuttingPlanMaterials()) {
                for (CuttingPlanMaterialPlacement cpmp : cpm.getCuttingPlanMaterialPlacement()) {
                    if (cpmp.getPlacement() != null) {
                        if (cpmp.getPerimetre() != null && cpmp.getPerimetre() > 0) {
                            perimetreMap.put(cpmp.getPlacement(), cpmp.getPerimetre());
                        } else {
                            perimetreMap.put(cpmp.getPlacement(),
                                    ExcelHelper.getPerimetre(cpmp.getPlacement()));
                        }
                        if(!nbrPieceMap.containsKey(cpmp.getPlacement())) {
                            Integer countNbrPiece = ExcelHelper.getTotalEmp(cpmp.getPlacement());
                            if(countNbrPiece != null) {
                                nbrPieceMap.put(cpmp.getPlacement(), countNbrPiece);
                            } else {
                                nbrPieceMap.put(cpmp.getPlacement(), 0);
                            }
                        }

                        if (cpmp.getTempsDeCoupe() != null && cpmp.getTempsDeCoupe() > 0) {
                            tempCoupeMap.put(cpmp.getPlacement(), cpmp.getTempsDeCoupe());
                        }

                    }
                }
            }

            obj.setSequence(sequence);
            obj.setCuttingPlanId(cp.getId());
            obj.setProjet(cp.getProjet());
            obj.setVersion(cp.getVersion());
            obj.setModele(cp.getDescription());
            obj.setDefinition(cp.getDefinition());
            Projet projet = projetService.findByObjId(cp.getProjet());
            if (projet != null && projet.getZone() != null) {
                obj.setZone(projet.getZone());
            } else {
                obj.setZone(zoneRepository.findByCode(arrPf.get(0).getAreaProduitFinit()));
            }

            List<CuttingRequestPartNumberV2> cppnArr = new ArrayList<CuttingRequestPartNumberV2>();
            for (ProduitFinit pf : arrPf) {
                CuttingRequestPartNumberV2 pn = new CuttingRequestPartNumberV2();
                pn.setDescription(pf.getDesiProdFinit());
                pn.setPartNumber(pf.getRefProdFinit());
                pn.setItem(pf.getRefProdSemi());
                pn.setQuantity(Integer.parseInt(pf.getQtyTotalPartNumber()));
                pn.setWo(pf.getNoff());
                pn.setWoid(pf.getWoid());
                pn.setCuttingRequest(obj);
                cppnArr.add(pn);
            }
            obj.setCuttingRequestPartNumbers(cppnArr);

            List<CuttingRequestSerieV2> crsArr = new ArrayList<CuttingRequestSerieV2>();
            for (Matlassage mt : matlassageRepository.findByNofOrderByNserie(sequence)) {
                try {
                    Coupe coupe = coupeRepository.findFirstByNserie(mt.getNserie());
                    CuttingRequestSerieV2 crs = new CuttingRequestSerieV2();
                    Optional<CuttingRequestSerieV2> crsopt = obj.getCuttingRequestSeries().stream().filter(
                                    cuttingRequestSerie -> cuttingRequestSerie.getSerie().equals(mt.getNserie().toString()))
                            .findFirst();
                    if (crsopt.isPresent()) {
                        crs = crsopt.get();
                    }
                    crs.setSerie(mt.getNserie() + "");
                    crs.setPartNumberMaterial(mt.getReftissu());
                    crs.setDescription(mt.getDescription());
                    crs.setMatelassageEndroit(mt.getSens());
                    crs.setLongueur(Double.parseDouble(mt.getLongueur()));
                    crs.setQuantite(mt.getQuantite());
                    crs.setPartNumbers(mt.getModele());
                    crs.setGroupPlacement(1);
                    crs.setActivated(true);
                    crs.setMachine(mt.getMachine());

                    if (mt.getReturnMagasin() != null)
                        crs.setRetourMagasin(Double.parseDouble(mt.getReturnMagasin()));
                    crs.setMaxDrill(null);
                    crs.setMaxPlie(null);
                    crs.setMaxPlieDrill(null);
                    crs.setNbrCouche(Integer.parseInt(mt.getnCouches()));
                    crs.setPlacement(mt.getPlacement());
                    crs.setLaize(Double.parseDouble(mt.getLaLaizeDemande()));
                    crs.setConfig(coupe.getConfiguration());
                    crs.setDrill(coupe.getDrill1() + "," + coupe.getDrill2());
                    crs.setPerimetre(perimetreMap.get(crs.getPlacement()));
                    crs.setNbrPiece(nbrPieceMap.getOrDefault(crs.getPlacement(), 0));
                    if(crs.getNbrCouche() != null && crs.getNbrPiece() != null) {
                        crs.setNbrPieceTotal((double) (crs.getNbrPiece() * crs.getNbrCouche()));
                    }

                    crs.setCuttingRequest(obj);
                    if (obj.getPlanningDate() == null || obj.getShift() == null) {
                        obj.setPlanningDate(
                                LocalDate.parse(mt.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                        LocalTime time = LocalTime.parse(mt.getHeure(), DateTimeFormatter.ofPattern("HH:mm"));
                        time.plusHours(2);
                        int hour = time.getHour();
                        if (hour < 8 && hour >= 0) {
                            obj.setShift("1");
                        } else if (hour < 16 && hour >= 8) {
                            obj.setShift("2");
                        } else {
                            obj.setShift("3");
                        }
                    }
                    crs.setPlanningDate(obj.getPlanningDate());
                    crs.setShift(obj.getShift());
//				if(mt.getDate() != null && !mt.getDate().trim().isEmpty() && mt.getHeure() != null && !mt.getHeure().trim().isEmpty()) {
//					if(crs.getDateDebutMatelassage() == null) crs.setDateDebutMatelassage(LocalDateTime.parse(mt.getDate() + " "+ mt.getHeure(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
//					if(crs.getDateFinMatelassage() == null) crs.setDateFinMatelassage(crs.getDateDebutMatelassage());
//				}

                    if (crs.getDateDebutCoupe() == null && coupe.getDatedebut() != null
                            && !coupe.getDatedebut().trim().isEmpty())
                        crs.setDateDebutCoupe(LocalDateTime.parse(coupe.getDatedebut(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    if (crs.getDateFinCoupe() == null && coupe.getDateFin() != null
                            && !coupe.getDateFin().trim().isEmpty())
                        crs.setDateFinCoupe(LocalDateTime.parse(coupe.getDateFin(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                    if (mt.getStatu().equalsIgnoreCase("complet")) {
                        crs.setStatusMatelassage("Complete");
                    } else if (mt.getStatu().equalsIgnoreCase("incomplet")) {
                        crs.setStatusMatelassage("Incomplete");
                    } else if (mt.getStatu().equalsIgnoreCase("Non demarre")) {
                        crs.setStatusMatelassage("Waiting");
                    } else {
                        crs.setStatusMatelassage("In progress");
                    }

                    if (coupe.getStatut().equalsIgnoreCase("complet")) {
                        crs.setStatusCoupe("Complete");
                    } else if (coupe.getStatut().equalsIgnoreCase("incomplet")) {
                        crs.setStatusCoupe("Incomplete");
                    } else if (coupe.getStatut().equalsIgnoreCase("Non demarre")) {
                        if (crs.getDateDebutCoupe() != null && crs.getDateFinCoupe() == null) {
                            crs.setStatusCoupe("In progress");
                        } else {
                            crs.setStatusCoupe("Waiting");
                        }

                    }
                    crs.setTableMatelassage(mt.getTablee());
                    crs.setTableCoupe(coupe.getMachine());
                    crs.setMatelasseur1(mt.getMatMatlasseur1());
                    crs.setMatelasseur2(mt.getMatMatlasseur2());
                    crs.setMatelasseur3(mt.getMatMatlasseur3());
                    crs.setMatelasseur4(mt.getMatMatlasseur4());

                    crs.setCoupeur1(coupe.getMatricule());
                    if (tempCoupeMap.containsKey(crs.getPlacement()) && tempCoupeMap.get(crs.getPlacement()) > 0) {
                        crs.setTempsDeCoupe(tempCoupeMap.get(crs.getPlacement()));
                    } else {
                        if (crs.getPerimetre() != null) {
                            CuttingSpeed speed = cuttingSpeedService.findById(crs.getConfig());
                            if (speed != null && speed.getVitesse() != null) {
                                crs.setTempsDeCoupe(UtilFunctions
                                        .convertTwoDigit(crs.getPerimetre() / (speed.getVitesse() * 100), 5));
                            } else {
                                crs.setTempsDeCoupe(UtilFunctions.convertTwoDigit(crs.getPerimetre() / 300, 5));
                            }

                        }
                    }

                    crsArr.add(crs);
                } catch (Exception e) {
                    System.out.println("Exception CRS : " + mt.getNserie() + " : " + e.getMessage());
                }
            }
            obj.setCuttingRequestSeries(crsArr);
            List<CuttingRequestBoxV2> crbArr = new ArrayList<CuttingRequestBoxV2>();
            for (GammeTechniqueImprimer gt : gammeTechniqueImprimerService.findBySequence(sequence)) {
                CuttingRequestBoxV2 crb = new CuttingRequestBoxV2();
                crb.setId((gt.getnSerieGammeImp()) + "");
                crb.setPartNumber(gt.getPartNumberImp());
                crb.setDescription(gt.getDescriptionImp());
                crb.setItem(gt.getCode3Imp());
                crb.setWo(gt.getNofImp());
                crb.setWoid(gt.getWoidImp());
                crb.setQtyBox(Integer.parseInt(gt.getQuantiteImp()));
                crb.setCuttingRequest(obj);
                crbArr.add(crb);
            }
            obj.setCuttingRequestBoxs(crbArr);
            CuttingRequestV2 newObj = service.save(obj);
            return newObj;
        }
//		} catch (Exception e) {
//			System.out.println("Sequence : " + "ERROR : " + sequence + " : " + e.getMessage());
//			return null;
//		}
    }

    @DeleteMapping("/{sequence}")
    public void deleteBySequence(@PathVariable String sequence) {
        service.deleteBySequence(sequence);
    }

}
