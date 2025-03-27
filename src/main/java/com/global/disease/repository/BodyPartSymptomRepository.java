package com.global.disease.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.global.disease.entity.BodyPartSymptom;

public interface BodyPartSymptomRepository extends JpaRepository<BodyPartSymptom, Long> {

    // 특정 부위에 연결된 증상 목록을 가져오는 메서드
    List<BodyPartSymptom> findByBodyPart_Id(Long bodyPartId);
}
