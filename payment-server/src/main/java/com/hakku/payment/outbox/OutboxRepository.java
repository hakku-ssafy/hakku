package com.hakku.payment.outbox;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 아웃박스 레코드 저장소. 릴레이는 PENDING 레코드를 생성순으로 배치 조회해 발행한다.
 */
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    /** 발행 대기(PENDING) 레코드를 생성순으로 최대 100건 조회한다(애그리거트별 순서 보존). */
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
