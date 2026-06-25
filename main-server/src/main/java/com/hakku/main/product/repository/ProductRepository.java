package com.hakku.main.product.repository;

import com.hakku.main.product.domain.Product;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 상품 MyBatis 매퍼. SQL 은 {@code mapper/product/ProductMapper.xml} 에 정의한다.
 *
 * <p>{@code colors}/{@code styles} 는 @ElementCollection(보조 테이블)이므로 읽기는 resultMap 의
 * 중첩 collection(보조 select)으로, 쓰기는 상품 행 저장 후 컬렉션 행을 별도 insert(갱신 시
 * delete 후 재삽입)로 처리한다(Hibernate 동작 미러링).
 */
@Mapper
public interface ProductRepository {

    /** 최신 상품이 먼저 오도록 id 내림차순으로 조회한다(공개 목록 — 활성 상품만). */
    List<Product> findAllByOrderByIdDesc();

    /** 어드민 커서 페이지네이션(활성/비활성 모두). cursorId 미지정 시 최신부터, id 내림차순으로 limit 개. */
    List<Product> findForAdminCursor(@Param("cursorId") Long cursorId, @Param("limit") int limit);

    boolean existsById(@Param("id") Long id);

    Product selectById(@Param("id") Long id);

    void insertProduct(Product product);

    void updateProduct(Product product);

    void insertColors(@Param("productId") Long productId, @Param("colors") Set<String> colors);

    void insertStyles(@Param("productId") Long productId, @Param("styles") Set<String> styles);

    void deleteColors(@Param("productId") Long productId);

    void deleteStyles(@Param("productId") Long productId);

    void deleteById(@Param("id") Long id);

    /** id 로 단건 조회한다. 서비스가 .orElseThrow()/.map() 로 쓰므로 Optional 로 감싼다. */
    default Optional<Product> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    /**
     * 엔티티를 삭제한다(JPA delete(entity) 의미 보존). 컬렉션 보조 테이블(@ElementCollection)을
     * 먼저 지운 뒤 상품 행을 삭제한다. H2 테스트 스키마의 FK 에는 ON DELETE CASCADE 가 없어
     * (Hibernate ddl 생성), Hibernate 의 컬렉션 선삭제 동작을 명시적으로 미러링한다.
     */
    default void delete(Product product) {
        deleteColors(product.getId());
        deleteStyles(product.getId());
        deleteById(product.getId());
    }

    /** 상품을 저장한다. id 가 없으면 신규(insert), 있으면 갱신(update). 컬렉션도 함께 영속한다. JPA save() 의미 보존. */
    default Product save(Product product) {
        if (product.getId() == null) {
            product.assignCreationTime(Instant.now());
            insertProduct(product);
            insertCollections(product);
        } else {
            product.assignUpdateTime(Instant.now());
            updateProduct(product);
            // Hibernate @ElementCollection 갱신 동작 미러링: 기존 컬렉션 삭제 후 재삽입.
            deleteColors(product.getId());
            deleteStyles(product.getId());
            insertCollections(product);
        }
        return product;
    }

    /** 빈 컬렉션은 foreach SQL 이 깨지므로 건너뛴다. */
    private void insertCollections(Product product) {
        Set<String> colors = product.getColors();
        if (colors != null && !colors.isEmpty()) {
            insertColors(product.getId(), colors);
        }
        Set<String> styles = product.getStyles();
        if (styles != null && !styles.isEmpty()) {
            insertStyles(product.getId(), styles);
        }
    }
}
