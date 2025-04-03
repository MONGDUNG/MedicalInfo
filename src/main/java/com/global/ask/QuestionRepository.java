package com.global.ask;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.global.ask.QuestionEntity;

@Repository //Repository 어노테이션은 이미 jpa에 포함이 돼있기 때문에 안써줘도 되지만 편의상 작성함
public interface QuestionRepository 
			extends JpaRepository<QuestionEntity, Integer>{
	
	
	List<QuestionEntity> findByContent(String content);
	List<QuestionEntity> findByContentLike(String content);	
	Page<QuestionEntity> findAll(Specification<QuestionEntity> spec, Pageable pageable);
	
	// visibility가 "PUBLIC"인 질문만 조회하는 메서드 추가
    List<QuestionEntity> findByVisibility(String visibility);

    // 특정 사용자의 질문을 조회하는 메서드 추가 (작성자 기준)
    List<QuestionEntity> findBySiteUser_Username(String username);

    // 특정 사용자의 공개 질문만 조회하는 메서드 추가 (작성자 기준 + visibility가 "PUBLIC"인 질문만)
    List<QuestionEntity> findBySiteUser_UsernameAndVisibility(String username, String visibility);

    // 페이징 처리와 함께 특정 visibility로 필터링하는 메서드 추가
    Page<QuestionEntity> findByVisibility(String visibility, Pageable pageable);

    // 사용자와 visibility를 기준으로 페이징 처리된 질문 조회 (public/private 필터링 + 사용자 필터링)
    Page<QuestionEntity> findBySiteUser_UsernameAndVisibility(String username, String visibility, Pageable pageable);
}
