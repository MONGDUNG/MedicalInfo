
package com.global.map.service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.global.map.etc.MedicalFacility;
import com.global.map.repository.MedicalFacilityRepository;

import lombok.RequiredArgsConstructor;
import java.io.File;
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
