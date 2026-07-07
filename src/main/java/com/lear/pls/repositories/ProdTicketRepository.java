package com.lear.pls.repositories;

import com.lear.pls.domain.ProdTicket;
import com.lear.pls.domain.SubDemande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ProdTicketRepository extends JpaRepository<ProdTicket, Long>, JpaSpecificationExecutor<ProdTicket>  {

    ProdTicket getProdTicketById(long id);

    @Query("select labelId from ProdTicket where labelId in (:idRouleauArr) group by labelId")
    List<String> findIdRouleauInThis(List<String> idRouleauArr);
    @Query("from ProdTicket where labelId in (:idRouleauArr) order by reftissu, labelId, quantity")
    List<ProdTicket> findObjIdRouleauInThis(List<String> idRouleauArr);
    @Query("from ProdTicket where labelId not in (:list) " +
            " and createdAt >= :date1 and createdAt <= :date2 " +
            " order by reftissu,labelId,quantity")
    List<ProdTicket> findRestNotInIdRouleauInThis(List<String> list, LocalDateTime date1, LocalDateTime date2);
}
