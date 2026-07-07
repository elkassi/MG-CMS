package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.ValidationQLaize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ValidationQLaizeRepository extends JpaRepository<ValidationQLaize, Long>, JpaSpecificationExecutor<ValidationQLaize> {
    List<ValidationQLaize> findAllByItemNumberAndRef(String itemNumber, String ref);
}
