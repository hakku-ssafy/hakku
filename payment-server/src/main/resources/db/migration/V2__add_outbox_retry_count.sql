-- M-2: poison 이벤트 격리용 재시도 횟수. 발행이 반복 실패하면 retry_count 가 증가하고,
-- 한계(OutboxEvent.MAX_PUBLISH_RETRIES) 도달 시 status 가 DEAD 로 전이되어 릴레이가 다음 이벤트로 진행한다.
-- prod 는 ddl-auto=validate 이므로 엔티티의 retry_count 컬럼과 lockstep 을 맞춘다.
ALTER TABLE payment_outbox ADD COLUMN retry_count INT NOT NULL DEFAULT 0;
