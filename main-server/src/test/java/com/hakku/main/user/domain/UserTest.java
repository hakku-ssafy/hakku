package com.hakku.main.user.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hakku.main.personalcolor.domain.PersonalColorType;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    @DisplayName("신규 회원은 지정한 역할을 가지며 퍼스널컬러는 미진단(null)")
    void newUserDefaults() {
        var user = new User("tester@hakku.dev", "테스터", Role.NORMAL);

        assertEquals("tester@hakku.dev", user.getEmail());
        assertEquals("테스터", user.getNickname());
        assertEquals(Role.NORMAL, user.getRole());
        assertNull(user.getPersonalColor());
        assertTrue(user.getPreferredStyles().isEmpty());
    }

    @Test
    @DisplayName("AI 진단 결과로 퍼스널컬러를 부여한다")
    void assignPersonalColor() {
        var user = new User("tester@hakku.dev", "테스터", Role.NORMAL);

        user.assignPersonalColor(PersonalColorType.TRUE_WINTER);

        assertEquals(PersonalColorType.TRUE_WINTER, user.getPersonalColor());
    }

    @Test
    @DisplayName("프로필 수정은 닉네임/이미지/선호스타일을 갱신한다")
    void updateProfile() {
        var user = new User("tester@hakku.dev", "테스터", Role.NORMAL);

        user.updateProfile("새닉네임", "https://cdn/img.png", Set.of("cute", "pastel"));

        assertEquals("새닉네임", user.getNickname());
        assertEquals("https://cdn/img.png", user.getProfileImageUrl());
        assertEquals(Set.of("cute", "pastel"), user.getPreferredStyles());
    }
}
