package com.global.notice.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.global.notice.entity.AnswerEntity;

@Repository
public interface AnswerRepository 
			extends JpaRepository<AnswerEntity, Integer>{
	
}
