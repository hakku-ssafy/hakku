package com.hakku.main.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 어드민 상품 관리 API. 전체 경로가 {@code /api/admin/**} 이므로 SecurityConfig 에서
 * ADMIN 역할만 접근하도록 강제된다(백엔드 1차 방어선).
 *
 * <p>한 페이지에서 상품 목록(활성/비활성 모두)을 보며 인라인 수정(활성화 여부·태그·이름)하고,
 * 커서 페이지네이션({@code cursorId}/{@code limit})으로 무한 스크롤한다.
 */
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private static final String DEFAULT_PAGE_SIZE = "20";

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    /** 커서 이후(id &lt; cursorId)의 상품을 최신순 limit 개 + 다음 커서로 반환한다. */
    @GetMapping
    public AdminProductPageResponse list(@RequestParam(required = false) Long cursorId,
                                         @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int limit) {
        return productService.listForAdmin(cursorId, limit);
    }

    /** 인라인 수정 — 이름·카테고리·태그·활성화 여부만 갱신한다. */
    @PatchMapping("/{id}")
    public ProductResponse edit(@PathVariable Long id,
                                @Valid @RequestBody AdminProductEditRequest request) {
        return productService.editByAdmin(id, request.name(), request.category(),
                request.styles(), request.active());
    }

    /** 어드민 인라인 수정 요청 — 편집 가능한 필드만 담는다. */
    public record AdminProductEditRequest(
            @NotBlank String name,
            String category,
            Set<String> styles,
            boolean active) {
    }
}
