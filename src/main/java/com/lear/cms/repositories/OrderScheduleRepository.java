package com.lear.cms.repositories;

import com.lear.cms.domain.OrderSchedule;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;

public interface OrderScheduleRepository extends CrudRepository<OrderSchedule, Long>  {

	@Query("from OrderSchedule where dateDemande <= :date2 and dateDemande >= :date1 and siteDemande = 'CUT-KIT'")
	List<OrderSchedule> findBetweenInterval(LocalDate date1, LocalDate date2);

	@Query("from OrderSchedule where statusDemande in (:statusDemande) and  siteDemande = 'CUT-KIT'")
	List<OrderSchedule> findAllByStatu(List<String> statusDemande);
	@Query("from OrderSchedule where dateDemande = :dateDemande and shiftDemande = :shiftDemande and siteDemande = 'CUT-KIT'")
	List<OrderSchedule> findByDateAndShift(LocalDate dateDemande, String shiftDemande);
	// get list by dateDemande ,shiftDemande

	@Modifying
	@Transactional
	@Query(value = "UPDATE OrderSchedule "
	        + "SET statusDemande = :status"
	        + " WHERE idDemande IN (:wo) AND statusDemande != 'C' AND statusDemande != 'E'")
	void updateStatus(List<Long> wo , String status);

	@Modifying
	@Transactional
	@Query(value = "UPDATE dbo.Order_Schedule " +
			"SET Status_Demande = :status " +
			"FROM dbo.Order_Schedule AS ord " +
			"JOIN dbo.Asprova_WO AS wo ON wo.[ID_Order_Schedule] = ord.ID_Demande " +
			"JOIN dbo.suiviplanning AS suivi ON suivi.id = wo.ID_ItemNumber_Asprova_WO " +
			"WHERE suivi.NSequence = :sequence AND statusDemande != 'C' AND statusDemande != 'E'", nativeQuery = true)
	void updateStatusNew(String sequence , String status);

	@Query("SELECT MAX(idDemande) FROM OrderSchedule")
	Long getMaxId();

	@Query(value = "SELECT NEXT VALUE FOR dbo.seq_Order_Schedule_ID_Demande", nativeQuery = true)
	Long getNextIdFromSequence();

	@Query("FROM OrderSchedule where idDemande = :id")
	OrderSchedule findByIdDemande(Long id);
}
