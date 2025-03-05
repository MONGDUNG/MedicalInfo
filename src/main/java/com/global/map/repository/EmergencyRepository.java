package com.global.map.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.global.map.entity.EmergencyEntity;

@Repository
public interface EmergencyRepository extends JpaRepository<EmergencyEntity, Long> {
	 @Query(value = """
	            SELECT * FROM Emergency
	            WHERE (6371 * 1000 * acos(
	                cos(radians(:centerLat)) * cos(radians(CAST(LATITUDE AS DOUBLE))) * 
	                cos(radians(CAST(LONGITUDE AS DOUBLE)) - radians(:centerLng)) + 
	                sin(radians(:centerLat)) * sin(radians(CAST(LATITUDE AS DOUBLE)))
	            )) <= 1000
	            """, nativeQuery = true)
	    List<EmergencyEntity> findNearbyEmergencys(@Param("centerLat") double centerLat, @Param("centerLng") double centerLng);
	    
}
