package com.bayerwestphalian.campaign.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

class RoleRepositoryTests {

    @Test
    void extendsJpaRepositoryForRoleAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(RoleRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(RoleRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments()).containsExactly(Role.class, UUID.class);
    }

    @Test
    void declaresKbRoleNameLookupMethods() throws Exception {
        Method findByName = RoleRepository.class.getMethod("findByName", SystemRoleName.class);
        Method existsByName = RoleRepository.class.getMethod("existsByName", SystemRoleName.class);

        assertThat(findByName.getGenericReturnType()).isEqualTo(optionalRole());
        assertThat(existsByName.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    void declaresMvpRoleFinderForRoleAdministration() throws Exception {
        Method method =
                RoleRepository.class.getMethod("findByMvpRoleOrderByDisplayNameAsc", boolean.class);

        assertThat(method.getGenericReturnType()).isEqualTo(roleList());
    }

    private static Type optionalRole() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("optionalRole").getGenericReturnType();
    }

    private static Type roleList() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("roleList").getGenericReturnType();
    }

    private interface ReturnTypes {
        Optional<Role> optionalRole();

        List<Role> roleList();
    }
}
