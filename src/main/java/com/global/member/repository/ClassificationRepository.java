package com.global.member.repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.global.member.entity.ClassificationEntity;

public interface ClassificationRepository extends JpaRepository<ClassificationEntity, Integer> {
	
	Optional<ClassificationEntity> findByUsername(String username);
	Optional<ClassificationEntity> findByBandate(Date bandate);
	
}
