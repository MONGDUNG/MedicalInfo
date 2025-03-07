package com.global.map.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.global.map.dto.MedinstDTO;
import com.global.map.dto.ConvenienceStoreDTO;
import com.global.map.entity.ConvenienceStoreEntity;
import com.global.map.entity.MedinstEntity;
import com.global.map.repository.MedinstRepository;

import lombok.RequiredArgsConstructor;

@Service  // Service 어노테이션 추가 (Spring에서 관리)
@RequiredArgsConstructor  // final 필드를 자동으로 생성자에서 초기화
public class conemerphar {

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
                .limit(1000)  // 최대 갯수
                .toList();
    }

    // 편의점 DTO 변환 메서드
    private ConvenienceStoreDTO convertToConvenienceStoreDTO(ConvenienceStoreEntity entity, double distance) {
        return ConvenienceStoreDTO.builder()
                .id(entity.getId())
                .storeCode(entity.getStoreCode())
                .phoneNumber(entity.getPhoneNumber())
                .address(entity.getAddress())
                .postalCode(entity.getPostalCode())
                .storeName(entity.getStoreName())  // `li()` 잘못된 부분 수정
                .latitude(Double.parseDouble(entity.getLatitude()))
                .longitude(Double.parseDouble(entity.getLongitude()))
                .distance(distance)  // 계산된 거리 추가
                .build();
    }

    // 병원 DTO 변환 메서드
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

    // 거리 계산 메서드 추가 (기존 코드에서 빠져있었음!)
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371 * 1000; // 지구 반지름 (미터)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
