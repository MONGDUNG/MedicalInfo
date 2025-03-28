package com.global.map.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.global.map.entity.MedinstEntity;

@Repository
public interface MedinstRepository extends JpaRepository<MedinstEntity, Long> {
    @Query(value = """
            SELECT * FROM MEDINST 
				WHERE (6371 * 1000 * acos(
				    cos(radians(:centerLat)) * cos(radians(CAST(LATITUDE AS DOUBLE))) * 
				    cos(radians(CAST(LONGITUDE AS DOUBLE)) - radians(:centerLng)) + 
				    sin(radians(:centerLat)) * sin(radians(CAST(LATITUDE AS DOUBLE)))
				)) <= :radius
				AND category_code = :category_code 
				ORDER BY RAND()
				FETCH FIRST 200 ROWS ONLY;
            """, nativeQuery = true)
    List<MedinstEntity> findNearbyHospitals(@Param("centerLat") double centerLat, @Param("centerLng") double centerLng,
    		@Param("radius") double radius, @Param("category_code") int category_code);
    
    @Query("SELECT m FROM MedinstEntity m WHERE m.categoryCode = : code") // 특정 종별코드만 조회하는 쿼리임.
    List<MedinstEntity> findByCategoryCode(@Param("code") int code);
    
    @Query("SELECT m.hospitalCode FROM MedinstEntity m WHERE m.hospitalName = :hospitalName AND m.address = :address")
    Optional<String> findHCdByHNmAndAdr(@Param("hospitalName") String hospitalName, @Param("address") String address);
    
    MedinstEntity findByHospitalCode(String hospitalCode);
    
    //hospitalCode로 병원을 찾아 리뷰수를 1 증가시키는 쿼리
    @Transactional
    @Modifying
    @Query("UPDATE MedinstEntity m SET m.reviewCount = m.reviewCount + 1 WHERE m.hospitalCode = :hospitalCode")
    void updateReviewCount(@Param("hospitalCode") String hospitalCode);
    //hospitalCode로 병원을 찾아 리뷰수를 1 감소시키는 쿼리
    @Transactional
    @Modifying
    @Query("UPDATE MedinstEntity m SET m.reviewCount = m.reviewCount - 1 WHERE m.hospitalCode = :hospitalCode")
    void decreaseReviewCount(@Param("hospitalCode") String hospitalCode);
    
    //hospitalCode로 병원을 찾아 평균 별점을 업데이트하는 쿼리
    @Transactional
    @Modifying
    @Query("UPDATE MedinstEntity m SET m.ratingAvg = :avgRating WHERE m.hospitalCode = :hospitalCode")
    void updateRating(@Param("hospitalCode") String hospitalCode,@Param("avgRating") double avgRating);
    
    @Query("SELECT m.reviewCount FROM MedinstEntity m WHERE m.hospitalCode = :hospitalCode")
    public int findReviewCountByHospitalCode(@Param("hospitalCode") String hospitalCode);
    @Query("SELECT m.ratingAvg FROM MedinstEntity m WHERE m.hospitalCode = :hospitalCode")
    public Double findAvgRatingByHospitalCode(@Param("hospitalCode") String hospitalCode);
}


