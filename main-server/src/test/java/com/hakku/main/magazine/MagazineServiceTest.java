package com.hakku.main.magazine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hakku.main.magazine.domain.Magazine;
import com.hakku.main.magazine.exception.MagazineNotFoundException;
import com.hakku.main.magazine.repository.MagazineRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MagazineServiceTest {

    @Mock
    private MagazineRepository repository;

    private MagazineService magazineService;

    @BeforeEach
    void setUp() {
        magazineService = new MagazineService(repository);
    }

    @Test
    @DisplayName("create: 매거진을 저장하고 마크다운 본문을 담은 응답을 반환한다")
    void create_savesMagazine() {
        when(repository.save(any(Magazine.class))).thenAnswer(inv -> inv.getArgument(0));

        MagazineResponse response = magazineService.create(new MagazineCommand(
                "EDITORIAL", "이주의 다꾸", "부제",
                "본문\n\n[데코 스티커](/products/7)", "/storage/images/cover", 1, true));

        assertThat(response.title()).isEqualTo("이주의 다꾸");
        assertThat(response.content()).contains("/products/7");
        assertThat(response.published()).isTrue();
        verify(repository).save(any(Magazine.class));
    }

    @Test
    @DisplayName("listPublished: 공개 목록은 발행(published)된 매거진만 정렬해 반환한다")
    void listPublished_returnsPublished() {
        when(repository.findPublishedOrdered()).thenReturn(List.of(
                new Magazine("K", "발행글", "s", "c", "/img", 0, true)));

        List<MagazineResponse> result = magazineService.listPublished();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("발행글");
        verify(repository).findPublishedOrdered();
    }

    @Test
    @DisplayName("get: 존재하지 않는 id 면 MagazineNotFoundException 을 던진다")
    void get_missing_throws() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> magazineService.get(99L))
                .isInstanceOf(MagazineNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("update: 기존 매거진의 제목·본문을 갱신한다")
    void update_updatesFields() {
        Magazine existing = new Magazine("K", "수정 전", "s", "old", "/img", 0, true);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Magazine.class))).thenAnswer(inv -> inv.getArgument(0));

        MagazineResponse response = magazineService.update(1L, new MagazineCommand(
                "K", "수정 후", "s", "new body", "/img", 0, true));

        assertThat(response.title()).isEqualTo("수정 후");
        assertThat(response.content()).isEqualTo("new body");
    }

    @Test
    @DisplayName("delete: 존재하지 않는 id 면 MagazineNotFoundException 을 던진다")
    void delete_missing_throws() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> magazineService.delete(5L))
                .isInstanceOf(MagazineNotFoundException.class);
    }
}
