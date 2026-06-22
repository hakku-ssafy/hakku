package com.hakku.main.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserRepository(MyBatis 매퍼) 통합 테스트. @ElementCollection(선호 스타일/컬러) 라운드트립 포함.
 * H2 스키마는 (JPA 제거 전까지) Hibernate ddl-auto=create-drop 으로 제공된다.
 */
@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Test
    @DisplayName("이메일로 저장된 회원을 조회한다")
    void findByEmail() {
        repository.save(new User("tester@hakku.dev", "테스터", Role.NORMAL));

        Optional<User> found = repository.findByEmail("tester@hakku.dev");

        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("findByEmail: 없는 이메일은 empty")
    void findByEmailMissing() {
        assertThat(repository.findByEmail("nobody@hakku.dev")).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail/existsById 는 존재 여부를 반환한다")
    void existsChecks() {
        User saved = repository.save(new User("seller@hakku.dev", "판매자", Role.SELLER));

        assertThat(repository.existsByEmail("seller@hakku.dev")).isTrue();
        assertThat(repository.existsByEmail("nobody@hakku.dev")).isFalse();
        assertThat(repository.existsById(saved.getId())).isTrue();
        assertThat(repository.existsById(999999L)).isFalse();
    }

    @Test
    @DisplayName("이메일은 유니크 제약 — 중복 저장 시 예외")
    void emailIsUnique() {
        repository.save(new User("dup@hakku.dev", "first", Role.NORMAL));

        assertThatThrownBy(() -> repository.save(new User("dup@hakku.dev", "second", Role.NORMAL)))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("선호 스타일/컬러 컬렉션이 저장·조회된다")
    void preferredCollectionsRoundTrip() {
        User user = repository.save(new User("pref@hakku.dev", "취향", Role.NORMAL));
        user.updateProfile("취향", null, Set.of("미니멀", "캐주얼"), Set.of("SUMMER"));
        repository.save(user);

        User reloaded = repository.findById(user.getId()).orElseThrow();

        assertThat(reloaded.getPreferredStyles()).containsExactlyInAnyOrder("미니멀", "캐주얼");
        assertThat(reloaded.getPreferredColors()).containsExactly("SUMMER");
    }

    @Test
    @DisplayName("프로필 갱신 시 컬렉션이 delete 후 재삽입된다")
    void updateReplacesCollections() {
        User user = repository.save(new User("rep@hakku.dev", "교체", Role.NORMAL));
        user.updateProfile("교체", null, Set.of("올드"), Set.of("WINTER"));
        repository.save(user);

        user.updateProfile("교체", null, Set.of("뉴"), Set.of("SPRING", "SUMMER"));
        repository.save(user);

        User reloaded = repository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getPreferredStyles()).containsExactly("뉴");
        assertThat(reloaded.getPreferredColors()).containsExactlyInAnyOrder("SPRING", "SUMMER");
    }

    @Test
    @DisplayName("findAllById 는 여러 회원을 조회한다(빈 입력은 빈 목록)")
    void findAllById() {
        User a = repository.save(new User("a@hakku.dev", "A", Role.NORMAL));
        User b = repository.save(new User("b@hakku.dev", "B", Role.NORMAL));

        List<User> found = repository.findAllById(List.of(a.getId(), b.getId()));

        assertThat(found).extracting(User::getEmail)
                .containsExactlyInAnyOrder("a@hakku.dev", "b@hakku.dev");
        assertThat(repository.findAllById(List.of())).isEmpty();
    }
}
