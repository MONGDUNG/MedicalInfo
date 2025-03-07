package com.global.map.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.global.map.dto.ConvenienceStoreDTO;
import com.global.map.dto.EmergencyDTO;
import com.global.map.dto.MedinstDTO;
import com.global.map.dto.PharmacyDTO;
import com.global.map.entity.ConvenienceStoreEntity;
import com.global.map.entity.EmergencyEntity;
import com.global.map.entity.MedinstEntity;
import com.global.map.entity.PharmacyEntity;
import com.global.map.repository.ConvenienceStoreRepository;
import com.global.map.repository.EmergencyRepository;
import com.global.map.repository.MedinstRepository;
import com.global.map.repository.PharmacyRepository;

import lombok.Builder;
import lombok.RequiredArgsConstructor;


@Builder
@Service
@RequiredArgsConstructor // final이 붙은 필드를 파라미터로 받는 생성자를 만들어줌
public class MapService {
   
    private final MedinstRepository medinstRepository;
    private final PharmacyRepository pharmacyRepository;
    private final EmergencyRepository emergencyRepository;
    private final ConvenienceStoreRepository storeRepository;

    private final double centerLat = 37.5665;  // 서울시청 (회원 주소로 변경 가능)
    private final double centerLng = 126.9780;
    
    public List<MedinstDTO> getNearbyHospitals() {
        List<MedinstEntity> hospitals = medinstRepository.findNearbyHospitals(centerLat, centerLng);

        return hospitals.stream()
                .map(entity -> medinstToDTO(entity, calculateDistance(
                        centerLat, centerLng,
                        Double.parseDouble(entity.getLatitude()),
                        Double.parseDouble(entity.getLongitude())
                )))
                .limit(1000)  // 최대 갯수
                .toList();
    }

    public List<Object> getNearbyPlaces() {
        List<Object> result = new ArrayList<>();

        List<MedinstEntity> hospitals = medinstRepository.findNearbyHospitals(centerLat, centerLng);
        result.addAll(hospitals.stream()
                .map(entity -> medinstToDTO(entity, calculateDistance(centerLat, centerLng, 
                        Double.parseDouble(entity.getLatitude()), 
                        Double.parseDouble(entity.getLongitude()))))
                .limit(1000)
                .toList());

        List<PharmacyEntity> pharmacies = pharmacyRepository.findNearbyPharmacies(centerLat, centerLng);
        result.addAll(pharmacies.stream()
                .map(entity -> pharmacyToDTO(entity, calculateDistance(centerLat, centerLng, 
                        Double.parseDouble(entity.getLatitude()), 
                        Double.parseDouble(entity.getLongitude()))))
                .limit(1000)
                .toList());

        List<EmergencyEntity> emergencies = emergencyRepository.findNearbyEmergencys(centerLat, centerLng);
        result.addAll(emergencies.stream()
                .map(entity -> emergencyToDTO(entity, calculateDistance(centerLat, centerLng, 
                        Double.parseDouble(entity.getLatitude()), 
                        Double.parseDouble(entity.getLongitude()))))
                .limit(1000)
                .toList());

        List<ConvenienceStoreEntity> stores = storeRepository.findNearbyConvenienceStores(centerLat, centerLng);
        result.addAll(stores.stream()
                .map(entity -> storeToDTO(entity, calculateDistance(centerLat, centerLng, 
                        Double.parseDouble(entity.getLatitude()), 
                        Double.parseDouble(entity.getLongitude()))))
                .limit(1000)
                .toList());

        return result;
    }
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
                .distance(distance)
                .build();
    }
    private PharmacyDTO pharmacyToDTO(PharmacyEntity entity, double distance) {
        return PharmacyDTO.builder()
                .id(entity.getId())
                .pharmacyName(entity.getPharmacyName())
                .address(entity.getAddress())
                .latitude(Double.parseDouble(entity.getLatitude()))
                .longitude(Double.parseDouble(entity.getLongitude()))
                .distance(distance)
                .build();
    }
    private EmergencyDTO emergencyToDTO(EmergencyEntity entity, double distance) {
        return EmergencyDTO.builder()
                .erId(entity.getErId())
                .hospitalName(entity.getHospitalName())
                .address(entity.getAddress())
                .latitude(Double.parseDouble(entity.getLatitude()))
                .longitude(Double.parseDouble(entity.getLongitude()))
                .distance(distance)
                .build();
    }


	        return emergencys.stream()
	                .map(entity -> emergencyToDTO(entity, calculateDistance(
	                        centerLat, centerLng,
	                        Double.parseDouble(entity.getLatitude()),
	                        Double.parseDouble(entity.getLongitude())
	                )))
	                .limit(1000)  // 최대 갯수
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
	
	private PharmacyDTO pharmacyToDTO(PharmacyEntity entity, double distance) {
	    return PharmacyDTO.builder()
	            .id(entity.getId())
	            .pharmacyName(entity.getPharmacyName())
	            .address(entity.getAddress())
	            .phoneNumber(entity.getPhoneNumber())
	            .postalCode(entity.getPostalcode())
	            .latitude(Double.parseDouble(entity.getLatitude()))
	            .longitude(Double.parseDouble(entity.getLongitude()))	            
	            .establishedDate(entity.getEstablishedDate() != null ? entity.getEstablishedDate().toString() : null)
	            .distance(distance)  // 계산된 거리 추가
	            .build();
	}
	
	private ConvenienceStoreDTO convenienceStoreToDTO(ConvenienceStoreEntity entity, double distance) {
	    return ConvenienceStoreDTO.builder()
	            .id(entity.getId())
	            .storeCode(entity.getStoreCode())
	            .licenseDate(entity.getLicenseDate()!= null ? entity.getLicenseDate().toString() : null)
	            .phoneNumber(entity.getPhoneNumber())
	            .address(entity.getAddress())
	            .postalCode(entity.getPostalCode())
	            .storeName(entity.getStoreName())
	            .latitude(Double.parseDouble(entity.getLatitude()))
	            .longitude(Double.parseDouble(entity.getLongitude()))
	            .distance(distance)  // 계산된 거리 추가
	            .build();
	}   
	
	private EmergencyDTO emergencyToDTO(EmergencyEntity entity, double distance) {
	    return EmergencyDTO.builder()
	            .erId(entity.getErId())
	            .hospitalName(entity.getHospitalName())
	            .phoneNumber(entity.getPhoneNumber())
	            .address(entity.getAddress())
	            .mon(entity.getMon())
	            .tues(entity.getTues())
	            .wednes(entity.getWednes())
	            .thurs(entity.getThurs())
	            .fri(entity.getFri())
	            .satur(entity.getSatur())
	            .sun(entity.getSun())
	            .holi(entity.getHoli())
	            .latitude(Double.parseDouble(entity.getLatitude()))
	            .longitude(Double.parseDouble(entity.getLongitude()))
	            .build();
	}
	//push 확인
}

