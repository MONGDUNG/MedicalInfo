package com.global.map.repository;

import com.global.map.entity.ReviewEntity;
import com.global.member.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    List<ReviewEntity> findByHospitalCode(String hospitalCode);
    List<ReviewEntity> findByMember(MemberEntity member);
}
