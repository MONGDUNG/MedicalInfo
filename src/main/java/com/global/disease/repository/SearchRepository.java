package com.global.disease.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.global.disease.entity.Search;


public interface SearchRepository extends JpaRepository<Search, Long>{
		
	Page<Search> findAll(Specification<Search> spec, Pageable pageable);

	long count(Specification<Search> spec);
	
	List<Search> findByDiseaseNameStartingWith(String name);
	
	// 신체부위 데이터를 검색 후, 증상 리스트에 해당하는 값이 포함된 데이터 검색
	@Query("SELECT s FROM Search s WHERE s.bodyPart LIKE %:bodyPart% AND(" +
			"s.symptom LIKE %:symptom1% OR " +
			"s.symptom LIKE %:symptom2% OR " +
			"s.symptom LIKE %:symptom3% OR " +
			"s.symptom LIKE %:symptom4% OR " +
			"s.symptom LIKE %:symptom5%)")
	List<Search> findByBodyPartContainingAndSymptom(
			@Param("bodyPart") String bodyPart,
			@Param("symptom1") String symptom1,
			@Param("symptom2") String symptom2,
			@Param("symptom3") String symptom3,
			@Param("symptom4") String symptom4,
			@Param("symptom5") String symptom5);

	List<Search> findByBodyPart(String bodyPart);
}
