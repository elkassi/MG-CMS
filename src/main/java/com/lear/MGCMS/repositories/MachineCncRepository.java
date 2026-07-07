package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.MachineCnc;
import com.lear.MGCMS.domain.MachineCnc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MachineCncRepository  extends JpaRepository<MachineCnc, Long>, JpaSpecificationExecutor<MachineCnc> {
    MachineCnc findByName(String name);
}
