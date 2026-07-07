package com.lear.cms.repositories;

import com.lear.cms.domain.SuiviPlanning;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.List;

public interface SuiviPlanningRepository extends CrudRepository<SuiviPlanning, Long> {
	
	@Query("from SuiviPlanning where nSequence = :sequence")
	SuiviPlanning findByNSequence(String sequence);
	@Query("from SuiviPlanning where nSequence = :sequence")
	List<SuiviPlanning> findListByNSequence(String sequence);

	/** (nSequence, Statu) rows for a batch of sequences — used to classify which sequences are still releasable ('Non demarre'). */
	@Query("select s.nSequence, s.statu from SuiviPlanning s where s.nSequence in :sequences")
	List<Object[]> findStatuBySequences(@Param("sequences") List<String> sequences);

	/**
	 * (nSequence, Statu) for every suiviplanning row — the full mirror source for
	 * the 20-min status sync. suiviplanning is the rolling planning set (a few
	 * hundred sequences), not the full completed history, so this stays bounded;
	 * driving off it (rather than a date_suivi window) means a status change is
	 * mirrored no matter how old the sequence's date_suivi is.
	 */
	@Query("select s.nSequence, s.statu from SuiviPlanning s")
	List<Object[]> findAllStatu();

	/**
	 * Coexisting writer for the logistics picklist. The WHERE guard keeps the
	 * external CMS desktop app race-safe: if it already released a sequence, this
	 * update simply skips that row and the caller reconciles by reading Statu.
	 */
	@Modifying
	@Transactional
	@Query("update SuiviPlanning s set s.statu = 'Released' "
			+ "where s.nSequence in :sequences and lower(s.statu) = lower('Non demarre')")
	int releaseNonDemarreBySequences(@Param("sequences") List<String> sequences);

	/**
	 * Compensating revert for a failed logistics commit. After
	 * {@link #releaseNonDemarreBySequences(List)} flipped 'Non demarre' -> 'Released'
	 * but the local mirror/allocation/snapshot then failed, this puts the just-released
	 * rows back to 'Non demarre'. Guarded on {@code lower(statu) = 'released'} so it
	 * only undoes rows still sitting at Released (a row already advanced by the external
	 * CMS desktop app — e.g. to 'En cours' — is left alone, not stomped backwards).
	 */
	@Modifying
	@Transactional
	@Query("update SuiviPlanning s set s.statu = 'Non demarre' "
			+ "where s.nSequence in :sequences and lower(s.statu) = lower('Released')")
	int revertReleasedToNonDemarreBySequences(@Param("sequences") List<String> sequences);

	/**
	 * The precise row ids currently at 'Non demarre' for a batch of sequences — the
	 * exact rows {@link #releaseNonDemarreBySequences(List)} will flip. Captured
	 * before the flip so the compensating revert can be scoped to only those rows,
	 * never a row of the same sequence that was already 'Released' beforehand.
	 */
	@Query("select s.id from SuiviPlanning s "
			+ "where s.nSequence in :sequences and lower(s.statu) = lower('Non demarre')")
	List<Long> findNonDemarreRowIdsBySequences(@Param("sequences") List<String> sequences);

	/**
	 * Row-level compensating revert for a failed logistics commit. Unlike
	 * {@link #revertReleasedToNonDemarreBySequences(List)}, which reverts every
	 * 'Released' row of the selected sequences, this puts back ONLY the rows this
	 * call flipped (their ids captured by {@link #findNonDemarreRowIdsBySequences(List)}
	 * before the flip). Guarded on {@code lower(statu) = 'released'} so a row already
	 * advanced by the external CMS desktop app is left alone, and so a sibling row
	 * that was already 'Released' from an earlier session is never touched.
	 */
	@Modifying
	@Transactional
	@Query("update SuiviPlanning s set s.statu = 'Non demarre' "
			+ "where s.id in :ids and lower(s.statu) = lower('Released')")
	int revertReleasedToNonDemarreByIds(@Param("ids") List<Long> ids);

	/**
	 * Chef-rectification write-through: force every row of a sequence to the
	 * given Statu, unguarded — the chef is the authority here. Keeping
	 * suiviplanning aligned is what stops the 20-min status sync
	 * (SuiviPlanningStatusSyncService) from reverting a manual correction.
	 */
	@Modifying
	@Transactional
	@Query("update SuiviPlanning s set s.statu = :statu where s.nSequence = :sequence")
	int updateStatuBySequence(@Param("sequence") String sequence, @Param("statu") String statu);

	@Query("select statu from SuiviPlanning where nSequence = :sequence")
	List<String> countTotalSequence(String sequence);

	@Modifying
	@Transactional
	@Query(value = "UPDATE SuiviPlanning "
			+ "SET Statu = 'En cours'"
			+ "where NSequence = :sequence and Statu != 'Complet'" , nativeQuery = true)
	void updateStatu1(String sequence);
	@Modifying
	@Transactional
	@Query(value = "UPDATE SuiviPlanning "
			+ "SET Statu = 'Complet'"
			+ "where NSequence = :sequence" , nativeQuery = true)
	void updateStatu2(String sequence);

	@Modifying
	@Transactional
	@Query(value = "UPDATE SuiviPlanning "
			+ "SET StatuC = 'En cours'"
			+ "where NSequence = :sequence and StatuC != 'Complet'" , nativeQuery = true)
	void updateStatuC1(String sequence);
	@Modifying
	@Transactional
	@Query(value = "UPDATE SuiviPlanning "
			+ "SET StatuC = 'Complet'"
			+ "where NSequence = :sequence" , nativeQuery = true)
	void updateStatuC2(String sequence);

	@Query("select max(id) from SuiviPlanning")
    Long getMaxId();
}
