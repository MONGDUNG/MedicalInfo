package com.global.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.global.member.entity.MemberTierEntity;

public interface MemberTierRepository extends JpaRepository<MemberTierEntity, Integer>{


	Optional<MemberTierEntity> findByTier(String tier);
	
}
