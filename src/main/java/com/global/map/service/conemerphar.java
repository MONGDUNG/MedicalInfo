package com.global.map.service; //여기서 약국,편의점,응급실 entity 모두 정리한후 mapservice로 복사붙이기 할 예정

import java.util.List;

import com.global.map.dto.MedinstDTO;
import com.global.map.entity.ConvenienceStoreEntity;
import com.global.map.entity.MedinstEntity;
import com.global.map.repository.MedinstRepository;

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
	    
	
	
	private ConvenienceStoreDTO ConvenienceStoreDTO(ConvenienceStoreEntity entity, double distance) {
	    return ConvenienceStoreDTO.builder()
	            .id(entity.getId())
	            .storeCode(entity.getStoreCode())
	            .licenseDate(entity.getlicenseDate())
	            .phoneNumber(entity.getPhoneNumber())
	            .address(entity.getAddress())
	            .postalCode(entity.getPostalCode())
	            
	            .li(entity.getHospitalName())
	            .categoryCode(entity.getCategoryCode())
	            
	            
	            
	            .website(entity.getWebsite())
	            .establishedDate(entity.getEstablishedDate() != null ? entity.getEstablishedDate().toString() : null)
	            .totalDoc(entity.getTotalDoc())
	            .latitude(Double.parseDouble(entity.getLatitude()))
	            .longitude(Double.parseDouble(entity.getLongitude()))
	            .distance(distance)  // 계산된 거리 추가
	            .build();
	}
	private Long id;               		// 번호
	private String storeCode;     		// 관리번호
	private String licenseDate;  	  	// 인허가날짜 (LocalDate 대신 문자열로 변환해서 보낼 수도 있음)
	private String phoneNumber;     	// 전화번호
	private String address;         	// 주소
    private Integer postalCode;     	// 우편번호
    private String storeName;		 	// 개설일자
    private double latitude;       	 	// 위도 (double로 변환 추천)
    private double longitude;       	// 경도 (double로 변환 추천)
    private double distance;        	// 현재 위치(서울시청 등)와의 거리 (추가 필드)
    
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
