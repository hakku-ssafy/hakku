# 학꾸(Hakku) 설계 문서

AI 퍼스널컬러 기반 꾸미기 아이템 추천 **커머스·커뮤니티 플랫폼 「학꾸(Hakku)」** 의 설계 산출물 모음입니다.
모든 문서는 실제 코드(Flyway 마이그레이션, Vue 라우터·뷰, REST 컨트롤러, git 이력)에서 도출되었습니다.

## 통합 문서 (권장)

**[hakku-design-document.html](./hakku-design-document.html)** — 6개 설계 문서를 하나로 묶은 **자립형 단일 HTML**.

- 브라우저로 바로 열기 — 인터넷 불필요(모든 다이어그램이 SVG로 인라인 임베드).
- **인쇄 → PDF 저장** 시 표지·단계별 페이지 분할이 최적화되어 있습니다.
- 퍼스널컬러 4계절(봄·여름·가을·겨울) 팔레트로 6개 섹션을 색상 코딩한 에디토리얼 레이아웃.

## 개별 문서

| # | 문서 | 파일 | 핵심 다이어그램 |
|---|------|------|-----------------|
| 01 | 요구사항 정의서 | [01-requirements-spec.md](./01-requirements-spec.md) | 아키텍처 구성도 · AI 진단 상태머신 |
| 02 | 유스케이스 다이어그램 | [02-usecase-diagram.md](./02-usecase-diagram.md) | UML 유스케이스(PlantUML) + 명세서 |
| 03 | ER 다이어그램 | [03-er-diagram.md](./03-er-diagram.md) | ERD(20개 테이블) + 데이터 사전 |
| 04 | WBS | [04-wbs.md](./04-wbs.md) | 작업분해구조 트리 |
| 05 | Gantt Chart | [05-gantt-chart.md](./05-gantt-chart.md) | 개발 일정 간트(2026-06-02~06-25) |
| 06 | 화면설계서 | [06-screen-design.md](./06-screen-design.md) | 화면 흐름도 + 22개 화면 명세 |

> 다이어그램은 **D2**(ERD·WBS·아키텍처·흐름도)·**PlantUML**(유스케이스 UML)·**Mermaid**(간트)로 작성됩니다. 통합 HTML이 모두 SVG로 인라인 렌더링하며, GitHub 미리보기는 Mermaid(간트)만 지원합니다.
> 문서 간 ID(FR/NFR/UC/SCR/테이블/W)는 동일 스킴으로 상호 참조됩니다.

## 개별 다이어그램 이미지 (`assets/`)

발표 자료·보고서에 바로 붙여 쓸 수 있는 standalone SVG: `usecase.svg` · `er-member.svg` · `er-commerce.svg` · `er-payment.svg` · `wbs.svg` · `architecture.svg` · `journey.svg` · `gantt.svg`.

## 메타

- 프로젝트: **학꾸(Hakku)** · 팀: 천창현 · 김해찬 (2인) · 기간: 2026.06.02 – 06.25
- 문서 버전 1.0 (2026-06-25)
