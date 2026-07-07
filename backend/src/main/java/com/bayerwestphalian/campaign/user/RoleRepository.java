package com.bayerwestphalian.campaign.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(SystemRoleName name);

    boolean existsByName(SystemRoleName name);

    List<Role> findByMvpRoleOrderByDisplayNameAsc(boolean mvpRole);
}
