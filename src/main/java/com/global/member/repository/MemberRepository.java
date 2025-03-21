package com.global.member.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Repository;

import com.global.member.entity.MemberEntity;



@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Integer>{
	
	

	Optional<MemberEntity> findByUsername(String username);
	Optional<MemberEntity> findByUsernameAndPassword(String username , String password);
	
	List<MemberEntity> findByLastlogindate(LocalDateTime date); // 최종접속시간으로 찾기
	
	Page<MemberEntity> findAll(Pageable pageable);
	
	void deleteByUsername(String username); 
	
	Optional<MemberEntity> findByEmail(String email); //메일 찾기
	
	boolean existsByEmail(String email); //이메일 중복 체크
	
	boolean existsByUsername(String username); // 아이디 중복 체크

}
