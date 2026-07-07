package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.MachineDxfRapport;
import com.lear.MGCMS.domain.MachineDxfRapportId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface MachineDxfRapportRepository  extends JpaRepository<MachineDxfRapport, MachineDxfRapportId>, JpaSpecificationExecutor<MachineDxfRapport> {

}
