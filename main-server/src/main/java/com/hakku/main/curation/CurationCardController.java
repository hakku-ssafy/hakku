package com.hakku.main.curation;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 큐레이션 카드 공개 조회 API. 메인 캐러셀(활성 목록)과 매거진 상세(단건)에서 쓴다.
 * GET 은 SecurityConfig 에서 공개(permitAll)로 허용된다.
 */
@RestController
@RequestMapping("/api/curation-cards")
public class CurationCardController {

    private final CurationCardService curationCardService;

    public CurationCardController(CurationCardService curationCardService) {
        this.curationCardService = curationCardService;
    }

    /** 메인 캐러셀용 — 활성 카드만 정렬 순서대로. */
    @GetMapping
    public List<CurationCardResponse> list() {
        return curationCardService.listActive();
    }

    /** 매거진 상세용 — 카드 본문 단건. */
    @GetMapping("/{id}")
    public CurationCardResponse get(@PathVariable Long id) {
        return curationCardService.get(id);
    }
}
