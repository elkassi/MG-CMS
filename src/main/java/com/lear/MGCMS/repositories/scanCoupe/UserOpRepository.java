package com.lear.MGCMS.repositories.scanCoupe;

import com.lear.MGCMS.domain.scanCoupe.UserOp;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserOpRepository  extends CrudRepository<UserOp, Integer> {

	UserOp findByMatricule(Integer matricule);

	List<UserOp> findAllByBlockTrue();

}
