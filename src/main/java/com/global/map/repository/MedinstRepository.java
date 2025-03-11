package com.global.map.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.global.map.entity.ConvenienceStoreEntity;
import com.global.map.entity.EmergencyEntity;
import com.global.map.entity.MedinstEntity;
import com.global.map.entity.PharmacyEntity;

@Repository
public interface MedinstRepository extends JpaRepository<MedinstEntity, Long> {
    @Query(value = """
            SELECT * FROM MEDINST 
            WHERE (6371 * 1000 * acos(
                cos(radians(:centerLat)) * cos(radians(CAST(LATITUDE AS DOUBLE))) * 
                cos(radians(CAST(LONGITUDE AS DOUBLE)) - radians(:centerLng)) + 
                sin(radians(:centerLat)) * sin(radians(CAST(LATITUDE AS DOUBLE)))
            )) <= 1000
            """, nativeQuery = true)
    List<MedinstEntity> findNearbyHospitals(@Param("centerLat") double centerLat, @Param("centerLng") double centerLng);
    
    @Query("SELECT m FROM MedinstEntity m WHERE m.categoryCode = : code") // 특정 종별코드만 조회하는 쿼리임.
    List<MedinstEntity> findByCategoryCode(@Param("code") int code);
}


