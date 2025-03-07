package com.global.map.repository;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.global.map.etc.MedicalFacility;

@Repository
public interface MedicalFacilityRepository extends ElasticsearchRepository<MedicalFacility, Integer> {
    List<MedicalFacility> findByName(String name);
    List<MedicalFacility> findByDeptName(String dept);
    List<MedicalFacility> findByAddressContaining(String address);
}
