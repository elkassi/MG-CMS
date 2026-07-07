package com.lear.ctc.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.ctc.domain.FilesHistory;

public interface FilesHistoryRepository  extends JpaRepository<FilesHistory, Long>, JpaSpecificationExecutor<FilesHistory> {

}
