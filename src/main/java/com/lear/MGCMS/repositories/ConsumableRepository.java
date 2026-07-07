package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.Consumable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ConsumableRepository extends JpaRepository<Consumable, String>, JpaSpecificationExecutor<Consumable> {

    @Query("SELECT t from Consumable t where (:of is not null)")
    Page<Consumable> findByFilter(PageRequest of);
    @Query("SELECT t from Consumable t where t.id = :id")
    Consumable findByObjId(String id);



}
