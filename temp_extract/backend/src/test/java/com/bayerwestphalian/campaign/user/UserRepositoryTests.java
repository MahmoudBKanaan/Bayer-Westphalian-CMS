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

class UserRepositoryTests {

    @Test
    void extendsJpaRepositoryForUserAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(UserRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(UserRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments()).containsExactly(User.class, UUID.class);
    }

    @Test
    void declaresKbEmailLookupMethodsForAuthenticationAndUniqueness() throws Exception {
        Method findByEmail = UserRepository.class.getMethod("findByEmailIgnoreCase", String.class);
        Method existsByEmail =
                UserRepository.class.getMethod("existsByEmailIgnoreCase", String.class);

        assertThat(findByEmail.getGenericReturnType()).isEqualTo(optionalUser());
        assertThat(existsByEmail.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    void declaresStatusFinderForUserAdministration() throws Exception {
        Method method =
                UserRepository.class.getMethod("findByStatusOrderByFullNameAsc", UserStatus.class);

        assertThat(method.getGenericReturnType()).isEqualTo(userList());
    }

    private static Type optionalUser() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("optionalUser").getGenericReturnType();
    }

    private static Type userList() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("userList").getGenericReturnType();
    }

    private interface ReturnTypes {
        Optional<User> optionalUser();

        List<User> userList();
    }
}
