package com.global.map.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.global.map.entity.EmergencyEntity;

@Repository
public interface MedDeptRepository extends JpaRepository<EmergencyEntity, Long> {
	@Query(value = """
                SELECT DEPT_CODE FROM MED_DEPT
                WHERE DEPT_NAME = :deptName
                """, nativeQuery = true)
	Integer findDeptCodeByDeptName(String deptName);
}
