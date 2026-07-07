package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlan;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight2;
import com.lear.MGCMS.domain.PartNumberMaterialConfigData;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.CuttingPlan.CuttingPlanService;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.PartNumberMaterialConfigDataService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.cms.ItemPlanCoupeService;
import com.lear.cms.domain.ItemPlanCoupe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/partNumberMaterialConfigData")
public class PartNumberMaterialConfigDataController {

    @Autowired
    private PartNumberMaterialConfigDataService service;
    @Autowired
    private UserService userService;
    @Autowired
    private MapValidationErrorService mapValidationErrorService;


    @GetMapping("/all")
    public Page<PartNumberMaterialConfigData> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy
    ) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        return service.findAll(filters, page, size, sortBy);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable String id) {
        PartNumberMaterialConfigData obj = service.findById(id);
        if(obj == null) {
            return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<PartNumberMaterialConfigData>(obj, HttpStatus.OK);
    }

    @Autowired
    private ItemPlanCoupeService itemPlanCoupeService;

    @PostMapping
    public ResponseEntity<?> save(@Valid @RequestBody PartNumberMaterialConfigData obj, BindingResult result, Authentication authentication) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;

        boolean hasCad = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CAD"));
        boolean hasCadFoam = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CAD_FOAM"));
        boolean isCadFoamOnly = hasCadFoam && !hasCad;

        PartNumberMaterialConfigData existing = service.findById(obj.getPartNumberMaterial());

        if (isCadFoamOnly) {
            if (existing == null) {
                obj.setFipDev(true);
            } else {
                if (existing.getFipDev() == null || !existing.getFipDev()) {
                    return new ResponseEntity<>("FIP dev non activé pour " + obj.getPartNumberMaterial() + " : modification interdite pour CAD FOAM", HttpStatus.FORBIDDEN);
                }
                obj.setFipDev(existing.getFipDev());
            }
        } else if (hasCad) {
            if (existing == null && obj.getFipDev() == null) {
                obj.setFipDev(false);
            }
        }

        User user = userService.findByUsername(authentication.getName());
        PartNumberMaterialConfigData newObj = service.save(obj);

        try {
            ItemPlanCoupe itemPlanCoupe = itemPlanCoupeService.findByItemNumberPlan(obj.getPartNumberMaterial());
            if (itemPlanCoupe != null) {
                // now we update the itemPlanCoupe
                itemPlanCoupe.setDescriptionPlan(obj.getDescription());
                itemPlanCoupe.setVitesseCoupePlan(obj.getVitesse() + "");
                itemPlanCoupe.setRotationPlan(obj.getRotation());
                itemPlanCoupe.setPlaquePlan(obj.getPlaque() + "");
                itemPlanCoupe.setTauxScrapPlan(obj.getTauxScrap() + "");
                itemPlanCoupe.setCommentPlan(obj.getCommentaire());
                itemPlanCoupeService.save(itemPlanCoupe);
            } else {
                // create a new itemPlanCoupe
                itemPlanCoupe = new ItemPlanCoupe();
                Integer max = itemPlanCoupeService.findMaxId();
                itemPlanCoupe.setIdItemPlan(max + 1);
                itemPlanCoupe.setItemNumberPlan(obj.getPartNumberMaterial());
                itemPlanCoupe.setDescriptionPlan(obj.getDescription());
                itemPlanCoupe.setVitesseCoupePlan(obj.getVitesse() + "");
                itemPlanCoupe.setRotationPlan(obj.getRotation());
                itemPlanCoupe.setPlaquePlan(obj.getPlaque() + "");
                itemPlanCoupe.setTauxScrapPlan(obj.getTauxScrap() + "");
                itemPlanCoupe.setCommentPlan(obj.getCommentaire());
                itemPlanCoupeService.save(itemPlanCoupe);
            }
        } catch (Exception e) {
            // Handle exception if needed
        }
        return new ResponseEntity<PartNumberMaterialConfigData>(newObj, HttpStatus.CREATED);
    }


}
