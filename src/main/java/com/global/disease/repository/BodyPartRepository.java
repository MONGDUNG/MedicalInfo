package com.global.disease.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.global.disease.entity.BodyPart;


public interface BodyPartRepository extends JpaRepository<BodyPart, Long> {
}
