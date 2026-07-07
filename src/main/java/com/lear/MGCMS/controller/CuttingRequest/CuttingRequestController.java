package com.lear.MGCMS.controller.CuttingRequest;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;

import com.lear.MGCMS.domain.CuttingRequest.*;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData;
import com.lear.MGCMS.payload.StatCoupe;
import com.lear.MGCMS.repositories.GammeTechniqueRepository;
import com.lear.MGCMS.security.Constants;
import com.lear.MGCMS.services.CuttingRequest.data.CuttingRequestDataService;
import com.lear.MGCMS.services.QueryService;
import com.lear.MGCMS.services.splice.MarkerService;
import com.lear.MGCMS.utils.ExcelHelper;
import com.lear.cms.domain.*;
import com.lear.cms.repositories.*;
import com.lear.ctc.domain.SequenceDetails;
import com.lear.ctc.domain.Sequences;
import com.lear.ctc.repositories.SequenceDetailsRepository;
import com.lear.ctc.repositories.SequencesRepository;
import com.lear.learpokayoke.domain.PokaYokeMain;
import com.lear.learpokayoke.repositories.PokaYokeMainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import com.lear.MGCMS.domain.PartNumberInfo;
import com.lear.MGCMS.domain.PlanningDetails;
import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanLight2Repository;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.PartNumberInfoService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.WorkOrderService;
import com.lear.MGCMS.services.CuttingRequest.CuttingRequestSerieInfoService;
import com.lear.MGCMS.services.CuttingRequest.CuttingRequestService;
import com.lear.MGCMS.services.ctc.GammeTechniqueImprimerService;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;

@RestController
@RequestMapping("/api/cuttingRequest")
public class CuttingRequestController {

    @Autowired
    private CuttingRequestService service;
    @Autowired
    private CuttingRequestSerieInfoService cuttingRequestSerieInfoService;

    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @Autowired
    private PartNumberInfoService partNumberInfoService;

    @Autowired
    private UserService userService;

    @Autowired
    private GammeTechniqueImprimerService gammeTechniqueImprimerService;
    @Autowired
    private CuttingPlanLight2Repository cuttingPlanLight2Repository;

    @Autowired
    private QueryService queryService;

    @Autowired
    private MatlassageRepository matlassageRepository;
    @Autowired
    private SuiviMatelassageRepository suiviMatelassageRepository;
    @Autowired
    private SuiviCoupeRepository suiviCoupeRepository;
    @Autowired
    private CoupeRepository coupeRepository;
    @Autowired
    private PokaYokeMainRepository pokaYokeMainRepository;
    @Autowired
    private SuiviPlanningRepository suiviPlanningRepository;
    @Autowired
    private AsprovaWORepository asprovaWORepository;
    @Autowired
    private OrderScheduleRepository orderScheduleRepository;
    @Autowired
    private ProduitFinitRepository produitFinitRepository;
    @Autowired
    private MarkerService markerService;
    @Autowired
    private SequencesRepository sequencesRepository;
    @Autowired
    private SequenceDetailsRepository sequenceDetailsRepository;

    @Autowired
    private WorkOrderService workOrderService;


    @GetMapping("/{sequence}")
    public ResponseEntity<?> findBySequence(@PathVariable String sequence) {
        CuttingRequest cr = service.findBySequence(sequence);
        if (cr == null) {
            return new ResponseEntity<String>("sequence " + sequence + " not found", HttpStatus.BAD_REQUEST);
        }
        cr.setCmsId(cuttingPlanLight2Repository.findCmsIdById(cr.getCuttingPlanId()));
        return new ResponseEntity<CuttingRequest>(cr, HttpStatus.OK);
    }

    @Autowired
    private CuttingRequestDataService cuttingRequestDataService;

    @GetMapping
    public List<CuttingRequestData> findAll2(
            @RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "shift", required = false) String shift
    ) {
        List<CuttingRequestData> arr = cuttingRequestDataService.findList(date, shift);
        for (CuttingRequestData cr : arr) {
            cr.setCmsId(cuttingPlanLight2Repository.findCmsIdById(cr.getCuttingPlanId()));
        }
        return arr;
    }

    @GetMapping("/stat")
    public List<StatCoupe> findAllStat(
            @RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "shift", required = false) String shift,
            @RequestParam(value = "zone", required = false) String zone
    ) {
        return service.findAllStat(date, shift, zone);
    }

    @Value("${lear.labelsFolder}")
    private String labelsFolder;

    @PostMapping("/printSerie")
    public ResponseEntity<?> printTicket(@RequestBody List<CuttingRequestSerieInfo> arr, BindingResult result, Authentication authentication) throws IOException {
        User user = userService.findByUsername(authentication.getName());

        boolean authorized = false;
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_IMPORTER")
                    || role.getName().equals("ROLE_ADMIN")
                    || role.getName().equals("ROLE_CHEF_DE_ZONE")
                    || role.getName().equals("ROLE_CHEF_EQUIPE")
                //ROLE_CHEF_DE_ZONE') or hasRole('CHEF_EQUIPE
            ) {
                authorized = true;
                break;
            }
        }
        if (!authorized) {
            return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
//      FileReader fr = new FileReader(new File("\\\\MATNR-FP01\\GROUPS\\CMS WEB\\cmsFolder\\tickets\\ticketSerie.prn"));
        FileReader fr = new FileReader(new File(labelsFolder + "ticketSerie.prn"));
        BufferedReader bfr = new BufferedReader(fr);
        String content = "";
        String line;
        while ((line = bfr.readLine()) != null) {
            content += line + "\n";
        }
        bfr.close();
        fr.close();
        if (arr.size() > 0) {
            CuttingRequestSerieInfo crsi = arr.get(0);
            CuttingRequestInfo cr = crsi.getCuttingRequest();
            List<String> arrAirbag = queryService.getReftissuAirbag();

            // sort arr by serie
            arr.sort((a, b) -> a.getSerie().compareTo(b.getSerie()));
            for (CuttingRequestSerieInfo obj : arr) {
                String contentTicket = content;
                contentTicket = contentTicket.replaceAll("@modele", cr.getModele());
                contentTicket = contentTicket.replaceAll("@sequence", cr.getSequence());
                contentTicket = contentTicket.replaceAll("@reftissu", obj.getPartNumberMaterial());
                contentTicket = contentTicket.replaceAll("@description", Matcher.quoteReplacement(obj.getDescription()));
                contentTicket = contentTicket.replaceAll("@serie", obj.getSerie());
                contentTicket = contentTicket.replaceAll("@placement", obj.getPlacement());
                contentTicket = contentTicket.replaceAll("@kit", obj.getPartNumbers());
                contentTicket = contentTicket.replaceAll("@longueur", obj.getLongueur() + "");
                contentTicket = contentTicket.replaceAll("@nbrCouche", obj.getNbrCouche() + "");
                contentTicket = contentTicket.replaceAll("@laize", obj.getLaize() + "");
                contentTicket = contentTicket.replaceAll("@placement", obj.getPlacement());
                contentTicket = contentTicket.replaceAll("@sens", obj.getMatelassageEndroit());
                contentTicket = contentTicket.replaceAll("@config", Matcher.quoteReplacement(obj.getConfig()));
                //numberOfOptions
                if (obj.getNumberOfOptions() != null && obj.getNumberOfOptions() > 0) {
                    contentTicket = contentTicket.replaceAll("@numberOfOptions", obj.getNumberOfOptions() + "");
                } else {
                    contentTicket = contentTicket.replaceAll("@numberOfOptions", "");
                }

                if (arrAirbag.contains(obj.getPartNumberMaterial())) {
                    contentTicket = contentTicket.replaceAll("@airbag", "^FT596,410^XG005.GRF,1,1^FS");
                } else {
                    contentTicket = contentTicket.replaceAll("@airbag", "");
                }
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
                contentTicket = contentTicket.replaceAll("@date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                contentTicket = contentTicket.replaceAll("@pageNumb", obj.getInd() + "");
                contentTicket = contentTicket.replaceAll("@totalPages", obj.getTotal() + "");
//                System.out.println(contentTicket);
                printLocal(contentTicket);
//                printServer(contentTicket, user.getIpPrinter());
            }
            return new ResponseEntity<String>("Printed : " + arr.size(), HttpStatus.OK);

        }
        return new ResponseEntity<String>("list empty", HttpStatus.BAD_REQUEST);

    }

    void printLocal(String contentTicket) {
        PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
        PrintService ps = null;
        try {
            PrintService pss[] = PrintServiceLookup.lookupPrintServices(null, null);
            int count = pss.length;
            for (int i = 0; i < count; i++) {
                if (pss[i].getName().trim().startsWith("Zebra GX420t")) {
                    pss[i].createPrintJob();
                    ps = pss[i];
                    break;
                }
            }
            if (ps == null) {
                for (int i = 0; i < count; i++) {
                    if (pss[i].getName().trim().startsWith("ZDesigner GX420t")) {
                        pss[i].createPrintJob();
                        ps = pss[i];
                        break;
                    }
                }
            }
            if (ps == null) {
                for (int i = 0; i < count; i++) {
                    if (pss[i].getName().trim().equalsIgnoreCase("ZEBRA")) {
                        pss[i].createPrintJob();
                        ps = pss[i];
                        break;
                    }
                }
            }
            try {
                DocPrintJob job = ps.createPrintJob();
                DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
                Doc doc = new SimpleDoc(contentTicket.getBytes(), flavor, null);
                job.print(doc, pras);
            } catch (PrintException e) {
                Constants.writeLogs(e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    Constants.writeLogs(element.toString());
                }
            }

        } catch (Exception e) {
            Constants.writeLogs(e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                Constants.writeLogs(element.toString());
            }
        }

    }

    void printServer(String contentTicket, String ipaddress) {
        Socket clientSocket;
        try {
            clientSocket = new Socket(ipaddress, 9100);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            outToServer.writeBytes(contentTicket);
            clientSocket.close();
        } catch (IOException e) {
            Constants.writeLogs(e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                Constants.writeLogs(element.toString());
            }
        }
    }


    public String getNewSequence(String seq1, String seq2, LocalDateTime date) {
        String maxSeq;

        if (seq1 == null && seq2 == null) {
            // Both sequences are null, use the current date and time
            maxSeq = date.format(DateTimeFormatter.ofPattern("ddMMyyHHmm")) + "00";
        } else if (seq1 == null) {
            // Only seq1 is null, use seq2
            maxSeq = seq2;
        } else if (seq2 == null) {
            // Only seq2 is null, use seq1
            maxSeq = seq1;
        } else {
            // Both sequences are not null, find the maximum
            maxSeq = seq1.compareTo(seq2) > 0 ? seq1 : seq2;
        }

        // Extract the numeric part and increment it
        int numericPart = Integer.parseInt(maxSeq.substring(10)) + 1;

        // Format the new sequence
        String newSequence = maxSeq.substring(0, 10) + String.format("%02d", numericPart);

        return newSequence;
    }


    @Autowired
    private GammeTechniqueCMSRepository gammeTechniqueCMSRepository;

    @PostMapping
    public ResponseEntity<?> save(@RequestBody CuttingRequest obj, BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;
        User user = userService.findByUsername(authentication.getName());

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

        LocalDateTime date = LocalDateTime.now();
        if (obj.getSequence() == null) {
            obj.setCreatedAt(date);
            obj.setCreatedBy(user);
        } else {

        }
        boolean newSequence = false;
        if (obj.getSequence() == null) {
            newSequence = true;
            String maxSequence = service.maxSequencesWithPrefix(date.format(DateTimeFormatter.ofPattern("ddMMyyHHmm")));
            String maxSequenceCMS = coupeRepository.getMaxNofLike(date.format(DateTimeFormatter.ofPattern("ddMMyyHHmm")) + "%");
            String newSeq = getNewSequence(maxSequence, maxSequenceCMS, date);
            obj.setSequence(newSeq);
        }


        Map<String, GammeTechnique> gtMap = new HashMap<>();
        if (!newSequence) {
            return new ResponseEntity<String>("you can't update the sequence", HttpStatus.BAD_REQUEST);
        }
        List<CuttingRequestBox> crbArr = new ArrayList<CuttingRequestBox>();
        Integer idMaxBox = service.getMaxIdBox();
        if (obj.getCuttingRequestBoxs().size() > 0) {
            return new ResponseEntity<String>("you can't update the sequence", HttpStatus.BAD_REQUEST);
        }
        if(idMaxBox == null) {
            idMaxBox = 75000000;
        }
        String nSerieGammeImpCMS = gammeTechniqueImprimerService.getMaxNSerieGammeImp();
        if (nSerieGammeImpCMS != null) {
            idMaxBox = Math.max(idMaxBox, Integer.parseInt(nSerieGammeImpCMS));
        }
        for (CuttingRequestPartNumber crpn : obj.getCuttingRequestPartNumbers()) {
            GammeTechnique gt = gammeTechniqueCMSRepository.findFirstByPartNumber(crpn.getPartNumber());
            if (gt == null) {
                return new ResponseEntity<String>(crpn.getPartNumber() + " Gamme Technique CMS not found", HttpStatus.BAD_REQUEST);
            }
            if (gt.getPackaging() == null || gt.getPackaging().isEmpty()) {
                return new ResponseEntity<String>(crpn.getPartNumber() + " Gamme Technique CMS with no packaging", HttpStatus.BAD_REQUEST);
            }
            gtMap.put(crpn.getPartNumber(), gt);
            crpn.setPackageQty(Integer.parseInt(gt.getPackaging()));
//                PartNumberInfo pni = partNumberInfoService.findByPartNumber(crpn.getPartNumber());
//                if (pni != null && pni.getPackageQty() != null) {
//                    crpn.setPackageQty(pni.getPackageQty());
//                } else {
//                    Integer packaging = queryService.getPackaginFromCMS(crpn.getPartNumber());
//                    if (packaging != null) {
//                        crpn.setPackageQty(packaging);
//                    } else {
//                        crpn.setPackageQty(10);
//                    }
//                }

            if (obj.getCuttingRequestBoxs().size() == 0) {
                int qty = crpn.getQuantity();
                while (qty > 0) {
                    CuttingRequestBox crb = new CuttingRequestBox();
                    idMaxBox++;
                    crb.setId(idMaxBox + "");
                    crb.setCuttingRequest(obj);
                    crb.setPartNumber(crpn.getPartNumber());
                    crb.setDescription(crpn.getDescription());
                    crb.setItem(crpn.getItem());
                    crb.setQtyBox(Math.min(qty, crpn.getPackageQty()));
                    crb.setWo(crpn.getWo());
                    crb.setWoid(crpn.getWoid());
                    crbArr.add(crb);
                    qty -= Math.min(qty, crpn.getPackageQty());
                }
            }

            crpn.setCuttingRequest(obj);
        }
        if (obj.getCuttingRequestBoxs().size() == 0) {
            obj.setCuttingRequestBoxs(crbArr);
        }


        Integer ind = 1;
        Integer maxSerie = service.getMaxSerieWithPrefix(date.format(DateTimeFormatter.ofPattern("yyyy")));
        String maxSerieMatStr = suiviMatelassageRepository.getMaxSerieWithPrefix(date.format(DateTimeFormatter.ofPattern("yyyy")) + "%");
        String maxSerieCoupeStr = suiviCoupeRepository.getMaxSerieWithPrefix(date.format(DateTimeFormatter.ofPattern("yyyy")) + "%");
        if (maxSerieMatStr != null) {
            maxSerie = Math.max(maxSerie, Integer.parseInt(maxSerieMatStr));
        }
        if (maxSerieCoupeStr != null) {
            maxSerie = Math.max(maxSerie, Integer.parseInt(maxSerieCoupeStr));
        }
        for (CuttingRequestSerie crs : obj.getCuttingRequestSeries()) {
            crs.setCuttingRequest(obj);
            if (crs.getSerie() == null) {
                maxSerie++;
                crs.setSerie(maxSerie.toString());
            }

            if (obj.getSequence() == null) {
                crs.setCreatedAt(date);
                crs.setPlanningDate(obj.getPlanningDate());
                crs.setShift(obj.getShift());
                crs.setInd(ind);
                Integer qty = ExcelHelper.getTotalEmp(crs.getPlacement());
                if(qty != null) {
                    crs.setNbrPiece(qty);
                    crs.setNbrPieceTotal((double) (crs.getNbrCouche() * qty));
                }
            }
            ind += 1;
        }

        // New sequences enter the lifecycle as IMPORTED (pre-release picklist
        // candidate). Existing rows (re-import / edit) keep their current status.
        if (newSequence) {
            obj.setSequenceStatus(SequenceStatus.IMPORTED);
        }
        CuttingRequest newObj = service.save(obj);
        int hour = date.getHour();
        String shift = "A";
        if (hour >= 6 && hour <= 14) {
            shift = ("B");
        } else if (hour >= 14 && hour <= 22) {
            shift = ("C");
        }
        Integer maxQuantity = 0;
        // get max id in SuiviPlanning
        Long maxIdSuiviPlanning = suiviPlanningRepository.getMaxId();
        if (maxIdSuiviPlanning == null) {
            maxIdSuiviPlanning = 0L;
        }
        // get max id in produitFinit
        Long maxIdProduitFinit = produitFinitRepository.getMaxId();
        if (maxIdProduitFinit == null) {
            maxIdProduitFinit = 0L;
        }

        // PLT Viwer : Sequences
        try {
            List<Sequences> arrSequences = sequencesRepository.findBySequence(newObj.getSequence());
            Sequences sd = new Sequences();
            if (arrSequences.size() > 1) {
                sequencesRepository.deleteAll(arrSequences);
            }
        } catch (Exception e) {
            System.out.println("Importation sequencesRepository problem : " + e.getMessage());
        }

        //max id of orderScheduleRepository
        Long maxIdOrderSchedule = orderScheduleRepository.getMaxId();
        if (maxIdOrderSchedule == null) {
            maxIdOrderSchedule = 0L;
        }
        //suiviPlanning , AsprovaWO, ProduitFinit, OrderSchedule, Sequences
        for (CuttingRequestPartNumber crpn : newObj.getCuttingRequestPartNumbers()) {
            maxQuantity = Math.max(maxQuantity, crpn.getQuantity());
            SuiviPlanning sp = new SuiviPlanning();
            maxIdSuiviPlanning++;
            sp.setId(maxIdSuiviPlanning);
            sp.setNof(crpn.getWo());
            sp.setProjet(newObj.getProjet());
            sp.setRefProdFinit(crpn.getPartNumber());
            sp.setDesiProdFinit(crpn.getDescription());
            sp.setRefProdSemi(crpn.getItem());
            sp.setNbrKits(crpn.getQuantity() + "");
            sp.setStatu("Non demarre");
            sp.setDatesuivi(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            sp.setModele(newObj.getModele());
            if (hour >= 6 && hour <= 14) {
                sp.setShift("B");
            } else if (hour >= 14 && hour <= 22) {
                sp.setShift("C");
            } else if (newObj.getShift().equals("3")) {
                sp.setShift("C");
            } else {
                sp.setShift(newObj.getShift());
            }
            sp.setHeureDebut("");
            sp.setHeureFin("");
            sp.setHeuresuivi(date.format(DateTimeFormatter.ofPattern("HH:mm")));
            sp.setHeureDebutC("");
            sp.setHeureFinC("");
            sp.setStatuC("Non demarre");
            sp.setTempsM("0");
            sp.setTempsC("0");
            sp.setEquipe(user.getFirstName() + " " + user.getLastName());
            sp.setDatee(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            sp.setWoid(newObj.getCuttingPlanId() + "");
            sp.setnSequence(newObj.getSequence());
            sp.setQtyTotalPartNumber(crpn.getQuantity() + "");
            sp.setStatusPlan("Closed");
            sp.setDefinition(newObj.getDefinition());
            sp.setAreaSuiviPlanning("1A");
            sp.setIdPlanSuiviPlanning(obj.getCmsId() + "");
            suiviPlanningRepository.save(sp);

            AsprovaWO asp = new AsprovaWO();
            asp.setIdOrderSchedule(Integer.parseInt(crpn.getWo()));
            asp.setIdItemNumberAsprovaWO(Integer.parseInt(maxIdSuiviPlanning + ""));
            asp.setItemNumberAsprovaWO(crpn.getItem());
            asp.setMarkerGroupIdAsprovaWO("");
            asp.setIdAsprovaAsprovaWO("null");
            asp.setQtyAsrpovaAsprovaWO(Double.parseDouble(crpn.getQuantity() + ""));
            asprovaWORepository.save(asp);

            maxIdProduitFinit++;
            ProduitFinit pf = new ProduitFinit();
            pf.setId(maxIdProduitFinit);
            pf.setNoff(crpn.getWo());
            pf.setRefProdFinit(crpn.getPartNumber());
            pf.setDesiProdFinit(crpn.getDescription());
            pf.setRefProdSemi(crpn.getItem());
            pf.setNbrKit(crpn.getQuantity() + "");
            pf.setWoid(obj.getCmsId() + "");
            pf.setnSequence(newObj.getSequence());
            pf.setQtyTotalPartNumber(crpn.getQuantity() + "");
            pf.setStatusPlan("Closed");
            pf.setAreaProduitFinit("1A");
            pf.setIdPlanProduiFinit(obj.getCmsId() + "");
            produitFinitRepository.save(pf);

            //WORK order schedule
            try {
                OrderSchedule os = orderScheduleRepository.findById(Long.parseLong(crpn.getWo())).orElse(new OrderSchedule());
                if (os.getIdDemande() == null) {
                    if (crpn.getWo() != null) {
                        os.setIdDemande(Long.parseLong(crpn.getWo()));
                        maxIdOrderSchedule = Math.max(maxIdOrderSchedule, Long.parseLong(crpn.getWo()));
                    } else {
                        maxIdOrderSchedule++;
                        os.setIdDemande(maxIdOrderSchedule);
                    }
                    os.setSiteDemande("CUT-KIT");
                    os.setPartNumberDemande(crpn.getItem());
                    os.setQuantiteDemande(crpn.getQuantity());
                    os.setDateDemande(newObj.getDueDate());
                    os.setShiftDemande(newObj.getDueShift());
                    os.setMatriculeDemandeurDemande("CMS WEB");
                    os.setNomDemandeurDemande("CMS WEB");
                    os.setStatusDemande("O");
                    os.setStatusPSDemande("Queu");
                    os.setStatusReceptionSewingDemande("Wait");
                    os.setCreationDateDemande(date.toLocalDate());
                    os.setCreationHourDemande(date.toLocalTime());
                    os.setModificationDateDemande(date.toLocalDate());
                    os.setModificationHourDemande(date.toLocalTime());
                    os.setUserNameDemande(user.getFirstName() + " " + user.getLastName());
                    os.setHostNameDemande("CMS WEB");
                    os.setSessionWDemande("CMS WEB");
                    os.setWorkCenterDemande("CUT");
                    os.setMarkerID(crpn.getWoid());
                    os.setMarkerGroupIDD(os.getIdDemande() + "");
                    os.setImportDateD(date);
                    orderScheduleRepository.save(os);
                } else {
                    if (Objects.equals(os.getStatusDemande(), "F")) {
                        os.setStatusDemande("O");
                        os.setMarkerID(crpn.getWoid());
                        orderScheduleRepository.save(os);
                    }
                }
            } catch (Exception e) {
                System.out.println("Importation orderScheduleRepository problem : " + e.getMessage());
            }

            try {
                Sequences sd = new Sequences();
                sd.setSequence(newObj.getSequence());
                sd.setCoverPartNumber(crpn.getPartNumber());
                sd.setCreatedAt(date);
                sd.setUpdatedAt(date);
                try {
                    sequencesRepository.save(sd);
                } catch (Exception firstSaveException) {
                    Long maxSequenceId = sequencesRepository.getMaxId();
                    sd.setId(maxSequenceId == null ? 1L : maxSequenceId + 1L);
                    sequencesRepository.save(sd);
                }

            } catch (Exception e) {
                System.out.println("Importation sequencesRepository problem : " + e.getMessage());
            }

        }
        Long suiviMatelassageIdMax = suiviMatelassageRepository.getMaxId();
        Long coupeIdMax = coupeRepository.getMaxId();
        Long suiviCoupeIdMax = suiviCoupeRepository.getMaxId();
        // Matlassage, SuiviMatelassage Coupe SuiviCoupe PokaYokeMain Marker SequenceDetails
        for (CuttingRequestSerie crs : newObj.getCuttingRequestSeries()) {
            try {
                Matlassage mt = new Matlassage();
                mt.setNserie(Long.parseLong(crs.getSerie()));
                mt.setNof(obj.getSequence());
                mt.setLongueur(crs.getLongueur() + "");
                mt.setSens(crs.getMatelassageEndroit());
                mt.setModele(newObj.getModele());
                mt.setQuantite(maxQuantity + "");
                mt.setDate(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                mt.setTablee("");
                mt.setnCouches(crs.getNbrCouche() + "");
                mt.setLaLaizeDemande(crs.getLaize() + "");
                mt.setReftissu(crs.getPartNumberMaterial());
                mt.setDescription(crs.getDescription());
                mt.setReturnMagasin("0");
                mt.setMatMatlasseur1("");
                mt.setMatMatlasseur2("");
                mt.setMatMatlasseur3("");
                mt.setMatMatlasseur4("");
                mt.setPlacement(crs.getPlacement());
                mt.setHeure(date.format(DateTimeFormatter.ofPattern("HH:mm")));
                mt.setEquipe(shift);

                mt.setStatu("Non demarre");
                mt.setMachine(crs.getMachine());
                mt.setDefinition(newObj.getDefinition());
                mt.setAreaMatelassage("1A");
                matlassageRepository.save(mt);
            } catch (Exception e) {
                System.out.println("Importation PokaYokeMainRepository problem : " + e.getMessage());
            }
            // now we save suivi matelassage
            try {
                SuiviMatelassage sm = new SuiviMatelassage();
                suiviMatelassageIdMax++;
                sm.setId(suiviMatelassageIdMax);
                sm.setNserie(crs.getSerie());
                sm.setNof(newObj.getSequence());
                sm.setModele(newObj.getModele());
                sm.setRefTissu(crs.getPartNumberMaterial());
                sm.setDesignation(crs.getDescription());
                sm.setTablee("");
                sm.setDateDebut("");
                sm.setDateFin("");
                sm.setDebutIncomplet("");
                sm.setFinIncomplet("");
                sm.setStatu("Non demarre");
                sm.setMachine("" + crs.getMachine());
                sm.setShift(shift);
                sm.setDate(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                sm.setTempsM("0");
                sm.setDefinition(newObj.getDefinition());
                sm.setAreaSuiviMatelassage("1A");
                suiviMatelassageRepository.save(sm);
            } catch (Exception e) {
                System.out.println("Importation PokaYokeMainRepository problem : " + e.getMessage());
            }
            // Enregistrement de Coupe
            try {
                Coupe coupe = new Coupe();
                coupeIdMax++;
                coupe.setId(coupeIdMax);
                coupe.setNof(newObj.getSequence());
                coupe.setNserie(Long.parseLong(crs.getSerie()));
                coupe.setDatedebut("");
                coupe.setDateFin("");
                String[] drills = crs.getDrill().split(",");
                if (drills.length < 1 || drills[0].isEmpty()) {
                    coupe.setDrill1("0");
                } else {
                    coupe.setDrill1(drills[0]);
                }
                if (drills.length < 2 || drills[1].isEmpty()) {
                    coupe.setDrill2("0");
                } else {
                    coupe.setDrill2(drills[1]);
                }
                coupe.setOrigineX("");
                coupe.setOrigineY("");
                coupe.setPlacement(crs.getPlacement());
                coupe.setConfiguration(crs.getConfig());
                coupe.setMatricule("");
                coupe.setMachine("");
                coupe.setTempsCoupe("");
                coupe.setStatut("Non demarre");
                coupe.setDrill(coupe.getDrill1() + "-" + coupe.getDrill2());
                coupe.setMatricule2("");
                coupe.setMachine2("");
                coupeRepository.save(coupe);
            } catch (Exception e) {
                System.out.println("Importation PokaYokeMainRepository problem : " + e.getMessage());
            }
            try {
                SuiviCoupe suiviCoupe = new SuiviCoupe();
                suiviCoupeIdMax++;
                suiviCoupe.setId(suiviCoupeIdMax);
                suiviCoupe.setNof(newObj.getSequence());
                suiviCoupe.setNserie(crs.getSerie());
                suiviCoupe.setModele(newObj.getModele());
                suiviCoupe.setRefTissu(crs.getPartNumberMaterial());
                suiviCoupe.setDesignation(crs.getDescription());
                suiviCoupe.setTablee("");
                suiviCoupe.setDateDebut("");
                suiviCoupe.setFinIncomplet("");
                suiviCoupe.setDebutIncomplet("");
                suiviCoupe.setDateFin("");
                suiviCoupe.setStatu("Non demarre");
                suiviCoupe.setMachine(crs.getMachine());
                suiviCoupe.setTempsCoupe("");
                suiviCoupe.setShift("");
                suiviCoupe.setTempsArret("");
                suiviCoupe.setPlacement(crs.getPlacement());
                suiviCoupe.setLongueur(crs.getLongueur() + "");
                suiviCoupe.setNbrCouches(crs.getNbrCouche() + "");
                suiviCoupe.setDate(date.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                suiviCoupe.setTempsCoupeBrute("0");
                suiviCoupe.setTempsReactivite("0");
                suiviCoupe.setType("Cycle");
                suiviCoupe.setTablee2("--");
                suiviCoupe.setShift2("--");
                suiviCoupe.setDate2("--");
                suiviCoupe.setAreaSuiviCoupe("1A");
                suiviCoupeRepository.save(suiviCoupe);
            } catch (Exception e) {
                System.out.println("Importation PokaYokeMainRepository problem : " + e.getMessage());
            }
            try {
                List<PokaYokeMain> pkArr = pokaYokeMainRepository.findByMarker(crs.getPlacement());
                PokaYokeMain pkObj = new PokaYokeMain();
                if (pkArr.size() == 1) {
                    pkObj = pkArr.get(0);
                    if (!crs.getPartNumberMaterial().equalsIgnoreCase(pkObj.getMaterial())) {
                        pkObj.setMaterial(crs.getPartNumberMaterial());
                        pokaYokeMainRepository.save(pkObj);
                    }
                } else {
                    if (pkArr.size() > 1) {
                        pokaYokeMainRepository.deleteByMarker(crs.getPlacement());
                    }
                    pkObj.setDate(date);
                    pkObj.setMaterial(crs.getPartNumberMaterial());
                    pkObj.setMarker(crs.getPlacement());
                    pokaYokeMainRepository.save(pkObj);
                }
            } catch (Exception e) {
                System.out.println("Importation PokaYokeMainRepository problem : " + e.getMessage());
            }
            try {
                markerService.updateMarker2(crs.getPlacement(), crs.getLongueur(), crs);
            } catch (Exception e) {
                System.out.println("Importation markerService problem : " + e.getMessage());
            }

            try {
                List<SequenceDetails> arrSequenceDetails = sequenceDetailsRepository.findBySerialNumber(crs.getSerie());
                SequenceDetails sd = new SequenceDetails();
                if (arrSequenceDetails.size() > 1) {
                    sequenceDetailsRepository.deleteAll(arrSequenceDetails);
                } else if (arrSequenceDetails.size() == 1) {
                    sd = arrSequenceDetails.get(0);
                    sd.setSequence(newObj.getSequence());
                    sd.setMarker(crs.getPlacement());
                    sd.setMaterialPartNumber(crs.getPartNumberMaterial());
                    if (sd.getCreatedAt() == null) {
                        sd.setCreatedAt(date);
                    }
                    sd.setUpdatedAt(date);
                    sequenceDetailsRepository.save(sd);
                } else {
                    sd.setSerialNumber(crs.getSerie());
                    sd.setSequence(newObj.getSequence());
                    sd.setMarker(crs.getPlacement());
                    sd.setMaterialPartNumber(crs.getPartNumberMaterial());
                    if (sd.getCreatedAt() == null) {
                        sd.setCreatedAt(date);
                    }
                    sequenceDetailsRepository.save(sd);
                }
            } catch (Exception e) {
                System.out.println("Importation sequenceDetailsRepository problem : " + e.getMessage());
            }
        }
        Long maxIdGammeTechniqueImprimer = gammeTechniqueImprimerService.getMaxId();
        //GammeTechniqueImprimer
        for (CuttingRequestBox crb : newObj.getCuttingRequestBoxs()) {
            try {
                GammeTechniqueImprimer gt = new GammeTechniqueImprimer();
                GammeTechnique gtObj = gtMap.get(crb.getPartNumber());
                maxIdGammeTechniqueImprimer++;
                gt.setIdGammeImp(maxIdGammeTechniqueImprimer);
                gt.setnSequenceImp(newObj.getSequence());
                gt.setTitreImp(gtObj.getTitre());
                gt.setCode1Imp(gtObj.getCode1() == null ? "" : gtObj.getCode1());
                gt.setCode3Imp(gtObj.getCode3());
                gt.setCode5Imp(gtObj.getCode5());
                gt.setPartNumberImp(gtObj.getPartNumber());
                gt.setDescriptionImp(gtObj.getDescription());
                gt.setElaborerparImp(gtObj.getElaborerpar());
                gt.setDateElaborationImp(gtObj.getDateElaboration());
                gt.setValiderparImp(gtObj.getValiderpar());
                gt.setModifierParImp(gtObj.getModifierPar());
                gt.setDateModificationImp(gtObj.getDateModification());
                gt.setValiderModParImp(gtObj.getValiderModPar());
                gt.setPackagingImp(gtObj.getPackaging() + "");
                gt.setEcnImp(gtObj.getEcn());
                gt.setnSerieGammeImp(Integer.parseInt(crb.getId()));
                gt.setNofImp(crb.getWo());
                gt.setWoidImp(crb.getWoid());
                gt.setQuantiteImp(crb.getQtyBox() + "");
                gt.setShiftImp(shift);
                gt.setDateImprissionImp(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                gt.setDateRechercheImp(date.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                gt.setNbrImprissionImp("1");
                gt.setUserNameImp("" + user.getFirstName() + " " + user.getLastName());
                gt.setIdGamme1(Integer.parseInt(user.getMatricule() + ""));
                gt.setStatusID("1ereImprission");
                gt.setSupplierKitImp(gtObj.getSupplierKit());
                gt.setSiteImp(gtObj.getSite());
                gt.setIndiceLabelImp(gtObj.getIndiceLabel());
                gt.setColorLabelImp(gtObj.getColorLabel());
                gt.setCustomerPN_LabelImp(gtObj.getCustomerPNLabel());
                gt.setJlr_PNLabelImp(gtObj.getJlrPNLabel());
                gt.setqLEvelLabelImp(gtObj.getqLEvelLabel());
                gt.setXatnLabelImp(gtObj.getXatnLabel());
                gt.setDescriptionLabelImp(gtObj.getDescriptionLabel());
                gt.setSoulignerLabelImp(gtObj.getSoulignerLabel());
                gt.setMatriculeLavelImp("");
                gammeTechniqueImprimerService.save(gt, user);
            } catch (Exception e) {
                System.out.println("Importation gammeTechniqueImprimerService problem : " + e.getMessage());
            }
        }

        Map<String, Integer> importedQtyByWo = new LinkedHashMap<>();
        for (CuttingRequestPartNumber crpn : newObj.getCuttingRequestPartNumbers()) {
            if (crpn.getWo() == null || crpn.getQuantity() == null) {
                continue;
            }
            importedQtyByWo.merge(crpn.getWo(), crpn.getQuantity(), Integer::sum);
        }

        List<Map<String, Object>> splitInfos = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : importedQtyByWo.entrySet()) {
            try {
                Map<String, Object> splitResult = workOrderService.splitWorkOrder(entry.getKey(), entry.getValue(), user);
                if (Boolean.TRUE.equals(splitResult.get("split"))) {
                    splitInfos.add(splitResult);
                }
            } catch (Exception e) {
                System.out.println("Importation splitWorkOrder problem : " + e.getMessage());
            }
        }

        if (!splitInfos.isEmpty()) {
            newObj.setSplitInfos(splitInfos);
            if (splitInfos.size() == 1) {
                newObj.setSplitInfo(splitInfos.get(0));
            }
        }

        return new ResponseEntity<CuttingRequest>(newObj, HttpStatus.CREATED);
    }

    @DeleteMapping("/{sequence}")
    public ResponseEntity<?> deleteBySequence(@PathVariable String sequence, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

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
//        service.deleteBySequence(sequence);
        queryService.deleteBySequenceCMSWEB(sequence);
        queryService.deleteBySequenceCMS(sequence);
        return new ResponseEntity<String>("DELETED", HttpStatus.OK);

    }


}
