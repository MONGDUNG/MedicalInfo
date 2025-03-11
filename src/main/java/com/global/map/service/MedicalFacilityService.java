package com.global.map.service;


import java.util.ArrayList;
import java.util.List;


import org.springframework.stereotype.Service;

import com.global.map.etc.MedicalFacility;
import com.global.map.repository.MedicalFacilityRepository;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class MedicalFacilityService {

    private final MedicalFacilityRepository repository;

    public void printAllData() {
        Iterable<MedicalFacility> facilitiesIterable = repository.findAll();
        List<MedicalFacility> facilities = new ArrayList<>();
        facilitiesIterable.forEach(facilities::add);
        facilities.forEach(System.out::println);
    }
}



