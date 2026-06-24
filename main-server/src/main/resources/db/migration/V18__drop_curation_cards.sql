-- 큐레이션 카드는 매거진(V17)으로 대체되었으므로 제거한다.
-- 홈 메인 캐러셀 슬라이드와 매거진 상세는 이제 magazines 테이블이 담당한다.
DROP TABLE IF EXISTS curation_cards;
