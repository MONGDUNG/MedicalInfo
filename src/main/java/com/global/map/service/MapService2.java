package com.global.map.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.global.map.dto.ConvenienceStoreDTO;
import com.global.map.entity.ConvenienceStoreEntity;
import com.global.map.repository.ConvenienceStoreRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;


@Builder
@Service
@RequiredArgsConstructor // final이 붙은 필드를 파라미터로 받는 생성자를 만들어줌
public class MapService2 {
	
	 private final ConvenienceStoreRepository conveniencestorerepository;
		
	    private final double centerLat = 37.4785;  // 관악구청 (회원 주소로 변경 가능)
	    private final double centerLng = 126.9520;

	    
	    public List<ConvenienceStoreDTO> getNearbyConvenienceStores() {
	        List<ConvenienceStoreEntity> conveniencestores = conveniencestorerepository.findNearbyConvenienceStores(centerLat, centerLng);
	        System.out.println("조회된 편의점 개수: " + conveniencestores.size());
	        return conveniencestores.stream()
	                .map(entity -> convenienceStoreToDTO(entity, calculateDistance(
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
	//push 확인용
}
