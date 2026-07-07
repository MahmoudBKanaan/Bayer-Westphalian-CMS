package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTokenTypeTests {

    @Test
    void supportsKbAccessAndRefreshTokenTypes() {
        assertThat(JwtTokenType.values())
                .containsExactly(JwtTokenType.ACCESS, JwtTokenType.REFRESH);
    }
}
