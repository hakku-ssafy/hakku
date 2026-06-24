package com.hakku.main.curation.repository;

import com.hakku.main.curation.domain.CurationCard;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 큐레이션 카드 MyBatis 매퍼. SQL 은 {@code mapper/curation/CurationCardMapper.xml} 에 정의한다.
 */
@Mapper
public interface CurationCardRepository {

    /** 전체 카드를 정렬 순서대로 조회한다(어드민용). */
    List<CurationCard> findAllOrdered();

    /** 활성 카드만 정렬 순서대로 조회한다(공개 캐러셀용). */
    List<CurationCard> findActiveOrdered();

    CurationCard selectById(@Param("id") Long id);

    void insertCard(CurationCard card);

    void updateCard(CurationCard card);

    void deleteById(@Param("id") Long id);

    /** id 로 단건 조회. 서비스가 .orElseThrow() 로 쓰므로 Optional 로 감싼다. */
    default Optional<CurationCard> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    /** 저장한다. id 가 없으면 신규(insert), 있으면 갱신(update). */
    default CurationCard save(CurationCard card) {
        if (card.getId() == null) {
            card.assignCreationTime(Instant.now());
            insertCard(card);
        } else {
            card.assignUpdateTime(Instant.now());
            updateCard(card);
        }
        return card;
    }

    default void delete(CurationCard card) {
        deleteById(card.getId());
    }
}
