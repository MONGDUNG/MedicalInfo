package com.global.map.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.global.map.dto.MedInfoDTO;
import com.global.map.entity.MedInfoEntity;
import com.global.map.repository.MedInfoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MedInfoService {
	
	private final MedInfoRepository medInfoRepository;
	
	public MedInfoDTO getHospitalOperatingHours(String hospitalName) {
		Optional<MedInfoEntity> entity = medInfoRepository.findByHospitalName(hospitalName);
		
		if(entity.isPresent()) {
			MedInfoEntity hospital = entity.get();
			return MedInfoDTO.builder()
					.hospitalCode(hospital.getHospitalCode())
					.hospitalName(hospital.getHospitalName())
					.mon(hospital.getMon() != null ? hospital.getMon() : "정보 없음")
                    .tues(hospital.getTues() != null ? hospital.getTues() : "정보 없음")
                    .wednes(hospital.getWednes() != null ? hospital.getWednes() : "정보 없음")
                    .thurs(hospital.getThurs() != null ? hospital.getThurs() : "정보 없음")
                    .fri(hospital.getFri() != null ? hospital.getFri() : "정보 없음")
                    .satur(hospital.getSatur() != null ? hospital.getSatur() : "정보 없음")
                    .sun(hospital.getSun() != null ? hospital.getSun() : "정보 없음")
                    .build();
        }
        return null;
	}
}
