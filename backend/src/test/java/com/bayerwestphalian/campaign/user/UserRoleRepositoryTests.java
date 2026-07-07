package com.bayerwestphalian.campaign.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

class UserRoleRepositoryTests {

    @Test
    void extendsJpaRepositoryForUserRoleMapping() {
        assertThat(JpaRepository.class).isAssignableFrom(UserRoleRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(UserRoleRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(UserRole.class, UserRoleId.class);
    }

    @Test
    void declaresAssignedRoleLookupForAuthenticationTokens() throws Exception {
        Method method = UserRoleRepository.class.getMethod("findByIdUserId", UUID.class);

        assertThat(method.getGenericReturnType()).isEqualTo(userRoleList());
    }

    private static Type userRoleList() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("userRoleList").getGenericReturnType();
    }

    private interface ReturnTypes {
        List<UserRole> userRoleList();
    }
}
