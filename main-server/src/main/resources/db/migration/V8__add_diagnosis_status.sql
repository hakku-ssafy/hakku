-- 진단 요청 상태 추가 (NONE=미요청, PENDING=진행중, COMPLETED=완료)
ALTER TABLE users
    ADD COLUMN diagnosis_status VARCHAR(20) NOT NULL DEFAULT 'NONE';
