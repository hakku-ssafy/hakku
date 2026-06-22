package com.hakku.main.user.repository;

import com.hakku.main.user.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 회원 MyBatis 매퍼. SQL 은 {@code mapper/user/UserMapper.xml} 에 정의한다.
 *
 * <p>{@code preferredStyles}/{@code preferredColors} 는 @ElementCollection(보조 테이블)이므로
 * 읽기는 resultMap 의 중첩 collection(보조 select)으로, 쓰기는 회원 행 저장 후 컬렉션 행을
 * 별도 insert(갱신 시 delete 후 재삽입)로 처리한다(Hibernate 동작 미러링).
 */
@Mapper
public interface UserRepository {

    boolean existsByEmail(@Param("email") String email);

    boolean existsById(@Param("id") Long id);

    User selectByEmail(@Param("email") String email);

    User selectById(@Param("id") Long id);

    /** 여러 id 로 회원을 조회한다(팔로워/팔로잉 목록 변환용). */
    List<User> selectByIds(@Param("ids") List<Long> ids);

    void insertUser(User user);

    void updateUser(User user);

    void insertPreferredStyles(@Param("userId") Long userId, @Param("styles") Set<String> styles);

    void insertPreferredColors(@Param("userId") Long userId, @Param("colors") Set<String> colors);

    void deletePreferredStyles(@Param("userId") Long userId);

    void deletePreferredColors(@Param("userId") Long userId);

    /** 이메일로 단건 조회한다. 서비스가 .orElseThrow() 로 쓰므로 Optional 로 감싼다. */
    default Optional<User> findByEmail(String email) {
        return Optional.ofNullable(selectByEmail(email));
    }

    /** id 로 단건 조회한다. 서비스가 .orElseThrow()/.map() 로 쓰므로 Optional 로 감싼다. */
    default Optional<User> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    /** 여러 id 로 회원을 조회한다(JPA findAllById 의미 보존). 빈 입력은 빈 목록. */
    default List<User> findAllById(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return selectByIds(ids);
    }

    /** 회원을 저장한다. id 가 없으면 신규(insert), 있으면 갱신(update). 컬렉션도 함께 영속한다. JPA save() 의미 보존. */
    default User save(User user) {
        if (user.getId() == null) {
            user.assignCreationTime(Instant.now());
            insertUser(user);
            insertCollections(user);
        } else {
            updateUser(user);
            // Hibernate @ElementCollection 갱신 동작 미러링: 기존 컬렉션 삭제 후 재삽입.
            deletePreferredStyles(user.getId());
            deletePreferredColors(user.getId());
            insertCollections(user);
        }
        return user;
    }

    /** 빈 컬렉션은 foreach SQL 이 깨지므로 건너뛴다. */
    private void insertCollections(User user) {
        Set<String> styles = user.getPreferredStyles();
        if (styles != null && !styles.isEmpty()) {
            insertPreferredStyles(user.getId(), styles);
        }
        Set<String> colors = user.getPreferredColors();
        if (colors != null && !colors.isEmpty()) {
            insertPreferredColors(user.getId(), colors);
        }
    }
}
