package com.hakku.main.product;

import com.hakku.main.product.exception.ProductAccessDeniedException;
import com.hakku.main.user.domain.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상품 API. 조회(GET)는 공개, 등록은 판매자만, 수정/삭제는 등록한 판매자 본인만.
 * 인증 주체(principal)는 JWT subject = 회원 id 문자열, 권한은 ROLE_* 권한으로 전달된다.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final String ROLE_PREFIX = "ROLE_";

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@AuthenticationPrincipal String userId,
                                  Authentication authentication,
                                  @Valid @RequestBody ProductRequest request) {
        return productService.create(Long.valueOf(userId), roleOf(authentication), request.toCommand());
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productService.list();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.get(id);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@AuthenticationPrincipal String userId,
                                  @PathVariable Long id,
                                  @Valid @RequestBody ProductRequest request) {
        return productService.update(id, Long.valueOf(userId), request.toCommand());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String userId, @PathVariable Long id) {
        productService.delete(id, Long.valueOf(userId));
    }

    private Role roleOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .map(Role::valueOf)
                .findFirst()
                .orElseThrow(() -> new ProductAccessDeniedException("권한 정보가 없습니다."));
    }

    public record ProductRequest(
            @NotBlank String name,
            String description,
            @PositiveOrZero long price,
            String keyColor,
            String subColor,
            Set<String> styles,
            String imageUrl) {

        ProductCommand toCommand() {
            return new ProductCommand(name, description, price, keyColor, subColor, styles, imageUrl);
        }
    }
}
