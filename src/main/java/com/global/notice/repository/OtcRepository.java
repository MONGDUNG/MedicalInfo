package com.global.notice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.global.notice.entity.OtcEntity;

@Repository
public interface OtcRepository extends JpaRepository<OtcEntity, Integer>{

}
