package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.SerieRouleauTemp;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SerieRouleauTempRepository extends CrudRepository<SerieRouleauTemp, String> {
    //we need to get the list that was created in the last 15minutes
    @Query("from SerieRouleauTemp where reftissu = :reftissu and date > :date")
    List<SerieRouleauTemp> getEnCours(String reftissu, LocalDateTime date);

    List<SerieRouleauTemp> findByIdRouleauIn(List<String> idRouleaux);
    List<SerieRouleauTemp> findByIdRouleau(String idRouleau);
}
