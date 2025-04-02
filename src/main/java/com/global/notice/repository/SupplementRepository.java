package com.global.notice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import com.global.notice.entity.SupplementEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface SupplementRepository extends JpaRepository<SupplementEntity, Long> {

    // 기존 페이지네이션 방식은 그대로 유지
    Page<SupplementEntity> findAll(Specification<SupplementEntity> spec, Pageable pageable);

    // 이름 검색
    List<SupplementEntity> findByPrdlstNmContaining(String prdlstNm);
    Page<SupplementEntity> findByPrdlstNmContaining(String prdlstNm, Pageable pageable);

    // 효능 검색
    List<SupplementEntity> findByPrimaryFncltyContaining(String primaryFnclty);
    Page<SupplementEntity> findByPrimaryFncltyContaining(String primaryFnclty, Pageable pageable);

    // 이름과 효능을 동시에 검색하는 메서드
    Page<SupplementEntity> findByPrdlstNmContainingAndPrimaryFncltyContaining(
        String prdlstNm, String primaryFnclty, Pageable pageable);
    
 // 좋아요 순으로 정렬 (내림차순)
    Page<SupplementEntity> findAllByOrderByVoterDesc(Pageable pageable);

    // 좋아요 순으로 정렬 (오름차순)
    Page<SupplementEntity> findAllByOrderByVoterAsc(Pageable pageable);
}