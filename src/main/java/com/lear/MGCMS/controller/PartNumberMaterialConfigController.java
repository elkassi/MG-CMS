package com.lear.MGCMS.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import com.lear.MGCMS.domain.*;
import com.lear.MGCMS.repositories.ReftissuCategoryRepository;
import com.lear.MGCMS.repositories.ReftissuMachineRepository;
import com.lear.MGCMS.repositories.ReftissuMarginRepository;
import com.lear.MGCMS.services.*;
import com.lear.MGCMS.services.cms.ItemPlanCoupeService;
import com.lear.cms.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/partNumberMaterialConfig")
public class PartNumberMaterialConfigController {

    private static final Logger log = LoggerFactory.getLogger(PartNumberMaterialConfigController.class);

    @Autowired
    private PartNumberMaterialConfigService service;

    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @Autowired
    private UserService userService;

    @Autowired
    private PartNumberMaterialConfigHistoryService partNumberMaterialConfigHistoryService;
    @Autowired
    private ItemPlanCoupeService itemPlanCoupeService;

    @GetMapping("/all")
    public Page<PartNumberMaterialConfigAll> findAll(
            @RequestParam(value = "partNumberMaterial", defaultValue = "", required = false) String partNumberMaterial,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy
    ) {
        return service.findAll(partNumberMaterial, page, size, sortBy);
    }

    @Autowired
    private PlieConfigService plieConfigService;

    @GetMapping("/pns/{pns}")
    public List<PartNumberMaterialConfigAll> findByPns(
            @PathVariable List<String> pns,
            @RequestParam(value = "projet", required = false) String projet
    ) {
        List<PartNumberMaterialConfigAll> list = service.findByPns(pns);
        for(PartNumberMaterialConfigAll obj : list) {
            // Handle rotation conversion
            String rotation = plieConfigService.findByPartNumberMaterialAndProjet(obj.getPartNumberMaterial(), projet);
            if(rotation != null) {
                System.out.println(obj.getPartNumberMaterial() + " Rotation changed from " + obj.getRotation() + " to " + rotation);
                obj.setRotation(rotation);
            }
            
            // Handle plie conversion for each machine
            if(obj.getReftissuMachines() != null) {
                for(ReftissuMachine machine : obj.getReftissuMachines()) {
                    // Convert maxPlie
                    Integer originalMaxPlie = machine.getMaxPlie();
                    Integer convertedMaxPlie = plieConfigService.convertPlieValue(originalMaxPlie, obj.getPartNumberMaterial(), projet);
                    if(convertedMaxPlie != null && !convertedMaxPlie.equals(originalMaxPlie)) {
                        System.out.println(obj.getPartNumberMaterial() + " MaxPlie changed from " + originalMaxPlie + " to " + convertedMaxPlie);
                        machine.setMaxPlie(convertedMaxPlie);
                    }
                    
                    // Convert maxPlieDrill - optimize by reusing convertedMaxPlie if values are the same
                    Integer originalMaxPlieDrill = machine.getMaxPlieDrill();
                    Integer convertedMaxPlieDrill;
                    
                    if(originalMaxPlieDrill != null && originalMaxPlieDrill.equals(originalMaxPlie)) {
                        // Reuse the already converted value since original values are the same
                        convertedMaxPlieDrill = convertedMaxPlie;
                    } else {
                        // Different values, need to convert separately
                        convertedMaxPlieDrill = plieConfigService.convertPlieValue(originalMaxPlieDrill, obj.getPartNumberMaterial(), projet);
                    }
                    
                    if(convertedMaxPlieDrill != null && !convertedMaxPlieDrill.equals(originalMaxPlieDrill)) {
                        System.out.println(obj.getPartNumberMaterial() + " MaxPlieDrill changed from " + originalMaxPlieDrill + " to " + convertedMaxPlieDrill);
                        machine.setMaxPlieDrill(convertedMaxPlieDrill);
                    }
                }
            }
        }
        return list;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findByCode(@PathVariable String id) {
        PartNumberMaterialConfig obj = service.findByObjId(id);
        if (obj == null) {
            return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<PartNumberMaterialConfig>(obj, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> save(@Valid @RequestBody PartNumberMaterialConfig obj, BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;

        boolean hasCad = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CAD"));
        boolean hasCadFoam = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CAD_FOAM"));
        boolean isCadFoamOnly = hasCadFoam && !hasCad;

        PartNumberMaterialConfig existing = service.findByObjId(obj.getPartNumberMaterial());

        if (isCadFoamOnly) {
            if (existing == null) {
                // New record by CAD_FOAM: force fipDev=true
                obj.setFipDev(true);
            } else {
                if (existing.getFipDev() == null || !existing.getFipDev()) {
                    return new ResponseEntity<>("FIP dev non activé pour " + obj.getPartNumberMaterial() + " : modification interdite pour CAD FOAM", HttpStatus.FORBIDDEN);
                }
                // Preserve existing fipDev — CAD_FOAM cannot change it
                obj.setFipDev(existing.getFipDev());
            }
        } else if (hasCad) {
            if (existing == null && obj.getFipDev() == null) {
                obj.setFipDev(false);
            }
        }

        User user = userService.findByUsername(authentication.getName());
        PartNumberMaterialConfig newObj = service.save(obj, user);
        return new ResponseEntity<PartNumberMaterialConfig>(newObj, HttpStatus.CREATED);
    }
    @Autowired
    private ReftissuMachineRepository reftissuMachineRepository;

    @Autowired
    private ReftissuCategoryRepository reftissuCategoryRepository;

    @Autowired
    private ReftissuMarginRepository reftissuMarginRepository;


    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestBody List<String> pns,
            Authentication authentication) {
        List<ItemPlanCoupe> arr = itemPlanCoupeService.findByItemNumberPlanIn(pns);
        System.out.println("loadItemConfig Size : " + arr.size());
        for (ItemPlanCoupe objItem : arr) {

            reftissuMachineRepository.deleteByPartnumber(objItem.getItemNumberPlan());
            reftissuCategoryRepository.deleteByPartnumber(objItem.getItemNumberPlan());
            reftissuMarginRepository.deleteByPartnumber(objItem.getItemNumberPlan());

            PartNumberMaterialConfig obj = new PartNumberMaterialConfig();
            obj.setPartNumberMaterial(objItem.getItemNumberPlan());
            obj.setDescription(objItem.getDescriptionPlan());
            obj.setVitesse(300);
            obj.setRotation(objItem.getRotationPlan());
            if (objItem.getPlaquePlan() != null && !objItem.getPlaquePlan().trim().isEmpty()) {
                try {
                    obj.setPlaque(Double.parseDouble(objItem.getPlaquePlan()));
                } catch (Exception e) {
                    System.out.println("Plaque " + objItem.getItemNumberPlan() + " : " + e.getMessage());
                }
            }
            obj.setTauxScrap(3.0);
            obj.setCommentaire(objItem.getCommentPlan());
            obj.setPartNumberMaterial(objItem.getItemNumberPlan());
            obj.setCreatedAt(LocalDateTime.now());
            List<ReftissuCategory> reftissuCategoryArr = new ArrayList<ReftissuCategory>();
            List<CategoryLaizePlanCoupe> arrCetegory = itemPlanCoupeService.findCategories(objItem.getIdItemPlan());
            for (CategoryLaizePlanCoupe category : arrCetegory) {
                ReftissuCategory reftissuCategory = new ReftissuCategory();
                reftissuCategory.setCategory(category.getCategoryNamePlan());
                reftissuCategory.setBorneMin(category.getBorneMinCategoryPlan());
                reftissuCategory.setBorneMax(category.getBorneMaxCategoryPlan());
                reftissuCategory.setDefaultValue(category.getDefaultCategoryPlan());
                reftissuCategory.setDescription(category.getDescriptionCategoryPlan());
                reftissuCategory.setPartNumberMaterialConfig(obj);
                reftissuCategoryArr.add(reftissuCategory);
            }
            obj.setReftissuCategories(reftissuCategoryArr);

            List<ReftissuMargin> reftissuMarginArr = new ArrayList<ReftissuMargin>();
            List<SeuilLongueurPlanCoupe> arrSeuil = itemPlanCoupeService.findByIdItemForeign1Plan(objItem.getIdItemPlan());
            int intervalId = 1;
            for (SeuilLongueurPlanCoupe seuil : arrSeuil) {
                ReftissuMargin reftissuMargin = new ReftissuMargin();
                reftissuMargin.setIntervalId(intervalId);
                reftissuMargin.setLongueurMin(seuil.getSeuilMinPlan());
                reftissuMargin.setLongueurMax(seuil.getSeuilMaxPlan());
                List<String> pliesConfigs = new ArrayList<String>();
                List<IntervalSeuilPlanCoupe> intervalSeuilPlanCoupes = itemPlanCoupeService.findByIdSeuilForeignPlan(seuil.getIdSeuil_Plan());
                for (IntervalSeuilPlanCoupe intervalSeuil : intervalSeuilPlanCoupes) {
                    pliesConfigs.add(intervalSeuil.getMinPlieSeuilPlan().intValue() + ";" + intervalSeuil.getLongueurPlusSeuilPlan());
                }
                reftissuMargin.setPliesConfig(String.join("|", pliesConfigs));
                reftissuMargin.setPartNumberMaterialConfig(obj);
                reftissuMarginArr.add(reftissuMargin);
                intervalId++;
            }
            obj.setReftissuMargins(reftissuMarginArr);

            List<ReftissuMachine> reftissuMachineArr = new ArrayList<ReftissuMachine>();
            List<ItemMachinePlanCoupe> arrMachine = itemPlanCoupeService.findByIdItemForeignPlan(objItem.getIdItemPlan());
            for (ItemMachinePlanCoupe machine : arrMachine) {
                ReftissuMachine reftissuMachine = new ReftissuMachine();
                reftissuMachine.setMaxPlie(machine.getMaxPlieTotalPlan());
                reftissuMachine.setMaxPlieDrill(machine.getMaxPlieDrillPlan());
                reftissuMachine.setMaxDrill(machine.getSeuilDrillPlan().intValue());
                switch (machine.getIdMachineForeignPlan()) {
                    case 1:
                        reftissuMachine.setMachineType("Lectra");
                        break;
                    case 2:
                        reftissuMachine.setMachineType("Gerber");
                        break;
                    case 3:
                        reftissuMachine.setMachineType("DIE");
                        break;
                    case 4:
                        reftissuMachine.setMachineType("LASER-LSR");
                        break;
                    case 5:
                        reftissuMachine.setMachineType("Lectra IP6");
                        break;
                    case 6:
                        reftissuMachine.setMachineType("LASER-DXF");
                        break;
                }
                reftissuMachine.setDefaultValue(machine.getDefaultItemMachinePlan());
                List<String> pliesConfigs = new ArrayList<String>();
                List<IntervalItemMachinePlanCoupe> intervalItemMachines = itemPlanCoupeService.findByIdItemMachineForeignPlan(machine.getIdItemMachinePlan());
                for (IntervalItemMachinePlanCoupe intervalItemMachine : intervalItemMachines) {
                    pliesConfigs.add(intervalItemMachine.getMinPliePlan().intValue() + ";" + intervalItemMachine.getConfigurationPlan());
                    obj.setMatelassageEndroit(intervalItemMachine.getMatelassageEndroitPlan());
                }
                reftissuMachine.setPliesConfig(String.join("|", pliesConfigs));
                reftissuMachine.setPartNumberMaterialConfig(obj);
                reftissuMachineArr.add(reftissuMachine);
            }
            obj.setReftissuMachines(reftissuMachineArr);

            service.save(obj, null);
            System.out.println("loadItemConfig : " + objItem.getItemNumberPlan());
        }

        return new ResponseEntity<String>("Done", HttpStatus.OK);
    }

    @PostMapping("/saveToCms/{id}")
    public ResponseEntity<?> saveToCms(@PathVariable String id, Authentication authentication) {
        PartNumberMaterialConfig config = service.findByObjId(id);
        if (config == null) {
            return new ResponseEntity<>("PartNumberMaterialConfig not found: " + id, HttpStatus.BAD_REQUEST);
        }
        try {
            String username = authentication != null ? authentication.getName() : "system";
            itemPlanCoupeService.saveToCms(config, username);
            return new ResponseEntity<>("Saved to CMS successfully", HttpStatus.OK);
        } catch (Exception e) {
            log.error("PartNumberMaterialConfigController save failed", e);
            return new ResponseEntity<>("Error saving to CMS: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody PartNumberMaterialConfig obj, Authentication authentication) {
        boolean hasCad = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CAD"));
        boolean hasCadFoam = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CAD_FOAM"));
        boolean isCadFoamOnly = hasCadFoam && !hasCad;

        if (isCadFoamOnly) {
            PartNumberMaterialConfig existing = service.findByObjId(obj.getPartNumberMaterial());
            if (existing == null) {
                return new ResponseEntity<>("Référence introuvable : " + obj.getPartNumberMaterial(), HttpStatus.BAD_REQUEST);
            }
            if (existing.getFipDev() == null || !existing.getFipDev()) {
                return new ResponseEntity<>("FIP dev non activé pour " + obj.getPartNumberMaterial() + " : suppression interdite pour CAD FOAM", HttpStatus.FORBIDDEN);
            }
        }

        service.delete(obj);
        return new ResponseEntity<String>("deleted", HttpStatus.CREATED);
    }

}
