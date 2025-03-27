package com.global.disease.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.global.disease.entity.Disease;
import com.global.disease.entity.Search;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface DiseaseRepository extends JpaRepository<Search, Long>{
	
	Page<Search> findAll(Specification<Search> spec, Pageable pageable);

	long count(Specification<Search> spec);
	
	
	  // 해당 부위 + 증상 ID 목록과 연관된 질병 조회 (중복 제거)
    @Query("SELECT DISTINCT d FROM Disease d " +
           "JOIN DiseaseBodyPart dbp ON d.id = dbp.disease.id " +
           "JOIN DiseaseSymptom ds ON d.id = ds.disease.id " +
           "WHERE dbp.bodyPart.id = :bodyPartId AND ds.symptom.id IN :symptomIds")
    List<Disease> findDiseasesByBodyPartAndSymptoms(@Param("bodyPartId") Long bodyPartId,
                                                    @Param("symptomIds") List<Long> symptomIds);
}
