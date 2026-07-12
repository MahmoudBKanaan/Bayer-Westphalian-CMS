package com.bayerwestphalian.campaign.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTests {

    @Test
    void createsPageResponseFromExplicitMetadata() {
        PageResponse<String> response = PageResponse.of(List.of("Anna", "Lena"), 1, 2, 5, 3);

        assertThat(response.content()).containsExactly("Anna", "Lena");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
        assertThat(response.empty()).isFalse();
    }

    @Test
    void marksFirstPageFromExplicitMetadata() {
        PageResponse<String> response = PageResponse.of(List.of("Anna"), 0, 10, 1, 1);

        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
        assertThat(response.empty()).isFalse();
    }

    @Test
    void marksEmptyPageFromExplicitMetadata() {
        PageResponse<String> response = PageResponse.of(List.of(), 0, 10, 0, 0);

        assertThat(response.content()).isEmpty();
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
        assertThat(response.empty()).isTrue();
    }

    @Test
    void createsPageResponseFromSpringDataPage() {
        PageImpl<String> page =
                new PageImpl<>(List.of("Campaign A", "Campaign B"), PageRequest.of(2, 2), 6);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.content()).containsExactly("Campaign A", "Campaign B");
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(6);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isTrue();
        assertThat(response.empty()).isFalse();
    }

    @Test
    void normalizesNullContentToEmptyList() {
        PageResponse<String> response = PageResponse.of(null, 0, 10, 0, 0);

        assertThat(response.content()).isEmpty();
        assertThat(response.empty()).isTrue();
    }

    @Test
    void copiesContentDefensively() {
        List<String> content = new ArrayList<>();
        content.add("initial");

        PageResponse<String> response = PageResponse.of(content, 0, 10, 1, 1);
        content.add("later mutation");

        assertThat(response.content()).containsExactly("initial");
        assertThatThrownBy(() -> response.content().add("not allowed"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
