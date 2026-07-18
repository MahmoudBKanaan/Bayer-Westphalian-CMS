package com.bayerwestphalian.campaign.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByStatusOrderByFullNameAsc(UserStatus status);

    List<User> findByEmailEndingWithIgnoreCase(String emailSuffix);

    /**
     * Active users holding a given system role (for follow-up assignee pickers and similar).
     */
    @Query(
            """
            select distinct u from User u
            join UserRole ur on ur.user = u
            join ur.role r
            where r.name = :roleName
              and u.status = :status
            order by u.fullName asc
            """)
    List<User> findActiveUsersWithRole(
            @Param("roleName") SystemRoleName roleName, @Param("status") UserStatus status);

    /**
     * Whether the given user is active and holds the system role (follow-up CSA assignee checks).
     */
    @Query(
            """
            select case when count(u) > 0 then true else false end
            from User u
            join UserRole ur on ur.user = u
            join ur.role r
            where u.id = :userId
              and r.name = :roleName
              and u.status = :status
            """)
    boolean isActiveUserWithRole(
            @Param("userId") UUID userId,
            @Param("roleName") SystemRoleName roleName,
            @Param("status") UserStatus status);
}
