package com.lear.learpokayoke.repositories;

import com.lear.learpokayoke.domain.PokaYokeMain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.List;

public interface PokaYokeMainRepository  extends JpaRepository<PokaYokeMain, Long>, JpaSpecificationExecutor<PokaYokeMain> {

    @Query("from PokaYokeMain where marker = :marker")
    List<PokaYokeMain> findByMarker(String marker);

    @Modifying
    @Transactional
    @Query("delete from PokaYokeMain where marker = :marker")
    void deleteByMarker(String marker);

}
