package com.lear.MGCMS.controller.cms;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.services.cms.ItemPlanCoupeService;
import com.lear.cms.domain.CategoryLaizePlanCoupe;
import com.lear.cms.domain.IntervalItemMachinePlanCoupe;
import com.lear.cms.domain.IntervalSeuilPlanCoupe;
import com.lear.cms.domain.ItemMachinePlanCoupe;
import com.lear.cms.domain.ItemPlanCoupe;
import com.lear.cms.domain.SeuilLongueurPlanCoupe;

@RestController
@RequestMapping("/api/itemPlanCoupe")
public class ItemPlanCoupeController {

	@Autowired
	private ItemPlanCoupeService service;
	
	@GetMapping("/{id}")
	public ItemPlanCoupe findById(@PathVariable String id) {
		return service.findByIdItemPlan(Integer.parseInt(id));
	}
	
	@GetMapping("/category/{id}")
	public List<CategoryLaizePlanCoupe> findCategory(@PathVariable String id) {
		return service.findCategories(Integer.parseInt(id));
	}
	
	@GetMapping("/seuil/{id}")
	public List<SeuilLongueurPlanCoupe> findByIdItemForeign1Plan(@PathVariable String id) {
		return service.findByIdItemForeign1Plan(Integer.parseInt(id));
	}
	@GetMapping("/seuil-interval/{id}")
	public List<IntervalSeuilPlanCoupe> findByIdSeuilForeignPlan(@PathVariable String id) {
		return service.findByIdSeuilForeignPlan(Integer.parseInt(id));
	}
	@GetMapping("/machine/{id}")
	public List<ItemMachinePlanCoupe> findByIdItemForeignPlan(@PathVariable String id) {
		return service.findByIdItemForeignPlan(Integer.parseInt(id));
	}
	@GetMapping("/machine-interval/{id}")
	public List<IntervalItemMachinePlanCoupe> findByIdItemMachineForeignPlan(@PathVariable String id) {
		return service.findByIdItemMachineForeignPlan(Integer.parseInt(id));
	}

	
}
