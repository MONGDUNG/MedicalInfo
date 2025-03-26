package com.global.disease.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.global.disease.entity.Symptom;

public interface SymptomRepository extends JpaRepository<Symptom, Long> {
}
