package com.global.map.service;

import com.global.map.entity.EmergencyEntity;
import com.global.map.repository.EmergencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmergencyService {

    private final EmergencyRepository emergencyRepository;

    public Map<String, String> getEmergencyHours(String hospitalName) {

        EmergencyEntity facility = emergencyRepository.findByHospitalName(hospitalName);

        if (facility == null) {
            return new HashMap<>(); // 병원 정보가 없으면 빈 데이터 반환
        }


        Map<String, String> hours = new HashMap<>();
        hours.put("mon", facility.getMon() != null ? facility.getMon() : "정보 없음");
        hours.put("tues", facility.getTues() != null ? facility.getTues() : "정보 없음");
        hours.put("wednes", facility.getWednes() != null ? facility.getWednes() : "정보 없음");
        hours.put("thurs", facility.getThurs() != null ? facility.getThurs() : "정보 없음");
        hours.put("fri", facility.getFri() != null ? facility.getFri() : "정보 없음");
        hours.put("satur", facility.getSatur() != null ? facility.getSatur() : "정보 없음");
        hours.put("sun", facility.getSun() != null ? facility.getSun() : "정보 없음");
        hours.put("holi", facility.getHoli() != null ? facility.getHoli() : "정보 없음");
        
        return hours;
    }
}
