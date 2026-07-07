package com.lear.MGCMS.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.ProductionTable;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight;

import java.util.List;
import java.util.Optional;

public interface ProductionTableRepository extends JpaRepository<ProductionTable, Long>, JpaSpecificationExecutor<ProductionTable> {

	List<ProductionTable> findByNomIn(List<String> noms);

	@Query("SELECT t from ProductionTable t where t.nom like :nom and t.zone.nom like :zone and (:of is not null)")
	Page<ProductionTable> findByFilter(String nom, String zone, PageRequest of);
	@Query("SELECT t from ProductionTable t where t.id = :id")
	ProductionTable findProductionTableById(Long id);

	@Query("SELECT t from ProductionTable t where t.zone.nom = :zoneName")
	List<ProductionTable> findByZone(String zoneName);

	Optional<ProductionTable> findByNom(String nom);

	// ======================== ORDONNANCEMENT LIGHT QUERIES ========================

	@Query("SELECT t.id, t.nom, z.nom, mt.name, t.tableLength " +
		   "FROM ProductionTable t LEFT JOIN t.machineType mt JOIN t.zone z " +
		   "ORDER BY z.nom, t.nom")
	List<Object[]> findAllMachinesLight();

	@Query("SELECT t.nom FROM ProductionTable t WHERE t.machineType IS NOT NULL AND t.machineType.name = 'LASER-DXF'")
	List<String> findLaserDxfMachineNames();

	/**
	 * Machine-name list for every ProductionTable whose machineType.name = 'Gerber'.
	 * Used by {@code OrdonnancementService} to apply the Gerber &times; 2 factor
	 * in cutting-time resolution. See {@code CuttingTimeCalculator}.
	 */
	@Query("SELECT t.nom FROM ProductionTable t WHERE t.machineType IS NOT NULL AND t.machineType.name = 'Gerber'")
	List<String> findGerberMachineNames();

	// ======================== DISPATCHER QUERIES (Phase 3+) ========================

	/**
	 * Distinct zone names hosting at least one active machine whose
	 * {@code machineType.name} equals the argument. Used by
	 * {@code SerieZoneResolver} to find candidate STRICT/SHARED zones for
	 * a given serie's machine type.
	 */
	@Query("SELECT DISTINCT t.zone.nom FROM ProductionTable t "
		 + "WHERE t.machineType IS NOT NULL AND t.machineType.name = :machineTypeName "
		 + "  AND t.zone IS NOT NULL")
	List<String> findZonesHostingMachineType(String machineTypeName);

	/**
	 * Light projection: {@code (machineNom, machineTypeName)} for every
	 * machine in a zone. Feeds {@code ActiveMachineResolver} which
	 * intersects with the chef's confirmation list.
	 */
	@Query("SELECT t.nom, t.machineType.name FROM ProductionTable t "
		 + "WHERE t.zone.nom = :zoneNom AND t.machineType IS NOT NULL")
	List<Object[]> findMachinesWithTypeInZone(String zoneNom);

	/**
	 * Batch version of {@link #findMachinesWithTypeInZone}: returns
	 * {@code (zoneNom, machineNom, machineTypeName)} for all machines in the
	 * given zones. Used by the dispatcher hot path to avoid N+1 per-zone queries.
	 */
	@Query("SELECT t.zone.nom, t.nom, t.machineType.name FROM ProductionTable t "
		 + "WHERE t.zone.nom IN :zoneNoms AND t.machineType IS NOT NULL")
	List<Object[]> findMachinesWithTypeInZones(@org.springframework.data.repository.query.Param("zoneNoms") List<String> zoneNoms);

	/**
	 * Batch resolve {@code tableNom} → {@code (zoneNom, zoneCategory)}.
	 * Used by the live-charge LockResolver to detect implicit locks: when
	 * a {@code CuttingRequestSerie.tableCoupe} sits in a STRICT zone, the
	 * whole sequence is pinned to that zone regardless of its
	 * {@code dispatchedZone} or {@code zoneAcceptanceStatus}.
	 *
	 * <p>Columns: 0=tableNom, 1=zoneNom, 2=zoneCategory (STRICT/SHARED).
	 * Rows whose table has no zone are excluded.</p>
	 */
	@Query("SELECT t.nom, t.zone.nom, t.zone.category FROM ProductionTable t "
		 + "WHERE t.nom IN :tables AND t.zone IS NOT NULL")
	List<Object[]> findZoneInfoByTableNoms(@org.springframework.data.repository.query.Param("tables") List<String> tables);
}
