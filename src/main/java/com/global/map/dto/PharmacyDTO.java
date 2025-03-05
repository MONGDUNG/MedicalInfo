package com.global.map.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PharmacyDTO {
	private Long id;               		// 번호
    private String pharmacyName;    	// 약국이름
    private String address;         	// 약국주소
    private String phoneNumber;     	// 약국 전화번호
    private Integer postalCode;     	// 우편번호
    private double latitude;       	 	// 위도 (double로 변환 추천)
    private double longitude;       	// 경도 (double로 변환 추천)
    private String establishedDate; 	// 개설일자
    private double distance;       		// 현재 위치(서울시청 등)와의 거리 (추가 필드)
}
