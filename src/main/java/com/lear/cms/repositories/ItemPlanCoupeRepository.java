package com.lear.cms.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.cms.domain.ItemPlanCoupe;

import java.util.List;

public interface ItemPlanCoupeRepository extends CrudRepository<ItemPlanCoupe, Integer> {

	ItemPlanCoupe findByIdItemPlan(Integer id);

    @Query("SELECT i FROM ItemPlanCoupe i WHERE i.itemNumberPlan IN :pns")
    List<ItemPlanCoupe> findByItemNumberPlanIn(List<String> pns);

    @Query("SELECT i FROM ItemPlanCoupe i WHERE i.itemNumberPlan = :partNumberMaterial")
    ItemPlanCoupe findByItemNumberPlan(String partNumberMaterial);

    @Query("SELECT MAX(i.idItemPlan) FROM ItemPlanCoupe i")
    Integer findMaxId();
}
