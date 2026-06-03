package com.hakku.main.product.repository;

import com.hakku.main.product.domain.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** 최신 상품이 먼저 오도록 id 내림차순으로 조회한다. */
    List<Product> findAllByOrderByIdDesc();
}
