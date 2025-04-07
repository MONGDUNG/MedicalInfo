package com.global.Post.Repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import com.global.Post.Data.PostEntity;

public interface PostRepository extends JpaRepository<PostEntity, Integer> {

	Page<PostEntity> findAll(Pageable pageable);    
	   
	Page<PostEntity>findAll(Specification<PostEntity> spec ,Pageable pageable);
	   
	Page<PostEntity> findByCategory(String category, Pageable pageable);

	Integer countByCategory(String category);


	
}
