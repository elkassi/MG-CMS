package com.lear.MGCMS.repositories.dispatcher;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lear.MGCMS.domain.dispatcher.UserZone;

/**
 * Plain CRUD over {@link UserZone}. Phase 3's {@code UserZoneService} adds
 * the business logic (default-zone promotion, role-aware lookups, etc.).
 */
public interface UserZoneRepository extends JpaRepository<UserZone, Long> {

    /** All active (non-revoked) links for a user, by {@code users.matricule}. */
    @Query("SELECT uz FROM UserZone uz "
         + "WHERE uz.user.matricule = :matricule AND uz.revokedAt IS NULL")
    List<UserZone> findActiveByUserMatricule(String matricule);

    /** All active links pointing at a zone — i.e. every chef that owns it. */
    @Query("SELECT uz FROM UserZone uz "
         + "WHERE uz.zone.nom = :zoneNom AND uz.revokedAt IS NULL")
    List<UserZone> findActiveByZoneNom(String zoneNom);

    /** Look up the exact link (one per user+zone via unique constraint). */
    @Query("SELECT uz FROM UserZone uz "
         + "WHERE uz.user.matricule = :matricule AND uz.zone.nom = :zoneNom")
    Optional<UserZone> findByUserAndZone(String matricule, String zoneNom);

    /**
     * The user's default zone if one is flagged {@code is_default=1}. If the
     * user has several defaults (shouldn't happen but guard anyway) returns
     * the first one by insertion order.
     */
    @Query("SELECT uz FROM UserZone uz "
         + "WHERE uz.user.matricule = :matricule AND uz.revokedAt IS NULL AND uz.isDefault = true "
         + "ORDER BY uz.id ASC")
    List<UserZone> findDefaultsForUser(String matricule);

    /**
     * Paginated, joined list of active user-zone assignments.
     * Projects {@code User} names and {@code Zone} category so the admin
     * table needs no secondary fetches.
     */
    @Query("SELECT uz.user.matricule, uz.user.firstName, uz.user.lastName, " +
           "uz.zone.nom, uz.zone.category, uz.isDefault, uz.assignedBy, uz.assignedAt " +
           "FROM UserZone uz " +
           "WHERE uz.revokedAt IS NULL " +
           "AND (:matricule IS NULL OR LOWER(uz.user.matricule) LIKE LOWER(CONCAT('%',:matricule,'%'))) " +
           "AND (:zoneNom IS NULL OR uz.zone.nom = :zoneNom)")
    Page<Object[]> findAllActiveJoined(@Param("matricule") String matricule,
                                        @Param("zoneNom") String zoneNom,
                                        Pageable pageable);
}
