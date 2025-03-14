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
	            SELECT * FROM EMERGENCY
	            WHERE (6371 * 1000 * acos(
	                cos(radians(:centerLat)) * cos(radians(CAST(LATITUDE AS DOUBLE))) * 
	                cos(radians(CAST(LONGITUDE AS DOUBLE)) - radians(:centerLng)) + 
	                sin(radians(:centerLat)) * sin(radians(CAST(LATITUDE AS DOUBLE)))
	            )) <= :radius
	            ORDER BY RAND()
	 			FETCH FIRST 200 ROWS ONLY
	            """, nativeQuery = true)
	    List<EmergencyEntity> findNearbyEmergencys(@Param("centerLat") double centerLat, @Param("centerLng") double centerLng,
	    		@Param("radius") double radius);
	 @Query("SELECT e FROM EmergencyEntity e WHERE REPLACE(e.hospitalName, ' ', '') = REPLACE(:hospitalName, ' ', '')")
	 EmergencyEntity findByHospitalName(@Param("hospitalName") String hospitalName);
	    
}
