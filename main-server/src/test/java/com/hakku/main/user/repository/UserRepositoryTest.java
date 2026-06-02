package com.hakku.main.user.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Test
    @DisplayName("이메일로 저장된 회원을 조회한다")
    void findByEmail() {
        repository.save(new User("tester@hakku.dev", "테스터", Role.NORMAL));

        Optional<User> found = repository.findByEmail("tester@hakku.dev");

        assertTrue(found.isPresent());
        assertEquals("테스터", found.get().getNickname());
    }

    @Test
    @DisplayName("findByEmail: 없는 이메일은 empty")
    void findByEmailMissing() {
        assertTrue(repository.findByEmail("nobody@hakku.dev").isEmpty());
    }

    @Test
    @DisplayName("existsByEmail 은 가입 여부를 반환한다")
    void existsByEmail() {
        repository.save(new User("seller@hakku.dev", "판매자", Role.SELLER));

        assertTrue(repository.existsByEmail("seller@hakku.dev"));
        assertFalse(repository.existsByEmail("nobody@hakku.dev"));
    }

    @Test
    @DisplayName("이메일은 유니크 제약 — 중복 저장 시 예외")
    void emailIsUnique() {
        repository.saveAndFlush(new User("dup@hakku.dev", "first", Role.NORMAL));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(new User("dup@hakku.dev", "second", Role.NORMAL)));
    }
}
