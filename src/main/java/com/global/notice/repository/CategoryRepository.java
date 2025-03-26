package com.global.notice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import com.global.notice.entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    Page<CategoryEntity> findAll(Specification<CategoryEntity> spec, Pageable pageable);

    List<CategoryEntity> findByProductNameContaining(String productName);						// 이름
    Page<CategoryEntity> findByProductNameContaining(String productName, Pageable pageable);	// 이름 페이지
    List<CategoryEntity> findByIndicationsContaining(String indications);						// 효능,효과
    Page<CategoryEntity> findByIndicationsContaining(String indications, Pageable pageable);	// 효능,효과 페이지
    Page<CategoryEntity> findAllByOrderByProductNameAsc(Pageable pageable);						// 이름 기준 정렬
 
    Page<CategoryEntity> findByProductNameContainingAndIndicationsContaining(					
        String productName, String indications, Pageable pageable
    );																							// 이름과 효능 검색을 동시에 처리하는 메서드
}