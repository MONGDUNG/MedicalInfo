package com.global.disease.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import com.global.disease.entity.Search;

public interface SearchRepository extends JpaRepository<Search, Long>{
	Page<Search> findAll(Specification<Search> spec, Pageable pageable);

	long count(Specification<Search> spec);
	
	List<Search> findByDiseaseNameStartingWith(String name);
}
