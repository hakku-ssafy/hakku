package com.hakku.main.magazine.repository;

import com.hakku.main.magazine.domain.Magazine;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 매거진 MyBatis 매퍼. SQL 은 {@code mapper/magazine/MagazineMapper.xml} 에 정의한다.
 */
@Mapper
public interface MagazineRepository {

    /** 전체 매거진을 정렬 순서대로 조회한다(어드민용). */
    List<Magazine> findAllOrdered();

    /** 발행(published)된 매거진만 정렬 순서대로 조회한다(공개 캐러셀/목록용). */
    List<Magazine> findPublishedOrdered();

    Magazine selectById(@Param("id") Long id);

    void insertMagazine(Magazine magazine);

    void updateMagazine(Magazine magazine);

    void deleteById(@Param("id") Long id);

    /** id 로 단건 조회. 서비스가 .orElseThrow() 로 쓰므로 Optional 로 감싼다. */
    default Optional<Magazine> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    /** 저장한다. id 가 없으면 신규(insert), 있으면 갱신(update). */
    default Magazine save(Magazine magazine) {
        if (magazine.getId() == null) {
            magazine.assignCreationTime(Instant.now());
            insertMagazine(magazine);
        } else {
            magazine.assignUpdateTime(Instant.now());
            updateMagazine(magazine);
        }
        return magazine;
    }

    default void delete(Magazine magazine) {
        deleteById(magazine.getId());
    }
}
