package com.global.map.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.global.map.dto.MedinstDTO;
import com.global.map.entity.MedinstEntity;
import com.global.map.repository.MedinstRepository;

import lombok.Builder;
import lombok.RequiredArgsConstructor;


@Builder
@Service
@RequiredArgsConstructor // final이 붙은 필드를 파라미터로 받는 생성자를 만들어줌
public class MapService {
	
	 private final MedinstRepository repository;

	    private final double centerLat = 37.5665;  // 서울시청 (회원 주소로 변경 가능)
	    private final double centerLng = 126.9780;

	    public List<MedinstDTO> getNearbyHospitals() {
	        List<MedinstEntity> hospitals = repository.findNearbyHospitals(centerLat, centerLng);

	        return hospitals.stream()
	                .map(entity -> medinstToDTO(entity, calculateDistance(
	                        centerLat, centerLng,
	                        Double.parseDouble(entity.getLatitude()),
	                        Double.parseDouble(entity.getLongitude())
	                )))
	                .limit(1000)  // 최대 100개
	                .toList();
	    }
	    
	    //거리 계산
	    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
	        double R = 6371 * 1000; // 지구 반지름 (m)
	        double dLat = Math.toRadians(lat2 - lat1);
	        double dLon = Math.toRadians(lon2 - lon1);
	        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
	                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
	                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
	        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	        return R * c;
	    }
	
	
	private MedinstDTO medinstToDTO(MedinstEntity entity, double distance) {
	    return MedinstDTO.builder()
	            .id(entity.getId())
	            .hospitalCode(entity.getHospitalCode())
	            .hospitalName(entity.getHospitalName())
	            .categoryCode(entity.getCategoryCode())
	            .postalCode(entity.getPostalCode())
	            .address(entity.getAddress())
	            .phoneNumber(entity.getPhoneNumber())
	            .website(entity.getWebsite())
	            .establishedDate(entity.getEstablishedDate() != null ? entity.getEstablishedDate().toString() : null)
	            .totalDoc(entity.getTotalDoc())
	            .latitude(Double.parseDouble(entity.getLatitude()))
	            .longitude(Double.parseDouble(entity.getLongitude()))
	            .distance(distance)  // 계산된 거리 추가
	            .build();
	}
}
