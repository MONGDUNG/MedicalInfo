package com.global.disease.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.global.disease.entity.SymptomsEntity;


public interface SymptomsRepository extends JpaRepository<SymptomsEntity, Integer> {

	List<SymptomsEntity> findByBodyPart(String bodyPart);
}
