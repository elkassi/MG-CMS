package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.PartNumberValidatedWeight;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PartNumberValidatedWeightRepository extends CrudRepository<PartNumberValidatedWeight, Long> {

    List<PartNumberValidatedWeight> findByPartnumber(String partnumber);

    PartNumberValidatedWeight findFirstByPartnumberOrderByValidatedAtDesc(String partnumber);

    @Query("select avg(bw.sentWeight) from BoxWeight bw where bw.boxId in " +
           "(select bw2.boxId from BoxWeight bw2 where bw2.boxId is not null) " +
           "and bw.sentBy is not null")
    Double findAverageWeight();
}
