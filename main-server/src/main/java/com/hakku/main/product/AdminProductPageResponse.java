package com.hakku.main.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 어드민 상품 목록의 커서 페이지. {@code nextCursor} 가 null(마지막 페이지)이면 직렬화에서 생략된다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminProductPageResponse(
        List<ProductResponse> items,
        Long nextCursor,
        boolean hasMore) {
}
