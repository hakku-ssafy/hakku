-- 학생증 자랑 게시판: 게시판 종류(board)와 공유 이미지 URL 컬럼 추가 (PRD §3.4 확장)
ALTER TABLE posts ADD COLUMN board VARCHAR(32) NOT NULL DEFAULT 'GENERAL';
ALTER TABLE posts ADD COLUMN image_url TEXT;

-- 게시판별 최신순 조회 최적화
CREATE INDEX idx_posts_board_id ON posts (board, id DESC);
