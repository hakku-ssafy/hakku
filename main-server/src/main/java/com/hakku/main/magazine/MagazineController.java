package com.hakku.main.magazine;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 매거진 공개 조회 API. 메인 캐러셀(발행 목록)과 매거진 상세(단건)에서 쓴다.
 * GET 은 SecurityConfig 에서 공개(permitAll)로 허용된다.
 */
@RestController
@RequestMapping("/api/magazines")
public class MagazineController {

    private final MagazineService magazineService;

    public MagazineController(MagazineService magazineService) {
        this.magazineService = magazineService;
    }

    /** 메인 캐러셀용 — 발행된 매거진만 정렬 순서대로. */
    @GetMapping
    public List<MagazineResponse> list() {
        return magazineService.listPublished();
    }

    /** 매거진 상세용 — 마크다운 본문 단건. */
    @GetMapping("/{id}")
    public MagazineResponse get(@PathVariable Long id) {
        return magazineService.get(id);
    }
}
