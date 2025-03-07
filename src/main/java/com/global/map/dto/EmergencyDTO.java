package com.global.map.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyDTO {
	private Long erId;               	// 번호
    private String hospitalName;    	// 기관명
    private String phoneNumber;     	// 전화
    private String address;         	// 주소
    private String mon; 				// 월요일 진료
    private String tues; 				// 화요일 진료
    private String wednes; 				// 수요일 진료
    private String thurs; 				// 목요일 진료
    private String fri; 				// 금요일 진료
    private String satur; 				// 토요일 진료
    private String sun; 				// 일요일 진료
    private String holi; 				// 공휴일 진료
    private double latitude;       	 	// 위도 (double로 변환 추천)
    private double longitude;       	// 경도 (double로 변환 추천)
    private double distance;
    
}
