package com.global.map.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.global.map.entity.PharmacyEntity;

@Repository
public interface PharmacyRepository extends JpaRepository<PharmacyEntity, Long>{
	 @Query(value = """
	            SELECT * FROM PHARMACY
	            WHERE (6371 * 1000 * acos(
	                cos(radians(:centerLat)) * cos(radians(CAST(LATITUDE AS DOUBLE))) * 
	                cos(radians(CAST(LONGITUDE AS DOUBLE)) - radians(:centerLng)) + 
	                sin(radians(:centerLat)) * sin(radians(CAST(LATITUDE AS DOUBLE)))
	            )) <= :radius
	            """, nativeQuery = true)
	    List<PharmacyEntity> findNearbyPharmacies(@Param("centerLat") double centerLat, @Param("centerLng") double centerLng,
	    		@Param("radius") double radius);
	    
}
