package com.global.map.repository;

import java.util.List;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import com.global.map.etc.MedicalFacility;

@Repository
public interface MedicalFacilityRepository extends ElasticsearchRepository<MedicalFacility, Integer> {
    List<MedicalFacility> findByName(String name);
    List<MedicalFacility> findByDeptName(String dept);
    List<MedicalFacility> findByAddressContaining(String address);
    List<MedicalFacility> findByNameAndLatAndLng(String name, Double lat, Double lng);
    
    @Query("SELECT m FROM MedicalFacility m WHERE m.hospitalName = :hospitalName")
    MedicalFacility findByHospitalName(@Param("hospitalName") String hospitalName);
    
}
