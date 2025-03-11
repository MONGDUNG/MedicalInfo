package com.global.map.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConvenienceStoreDTO {
	private Long id;               		// 번호
	private String storeCode;     		// 관리번호
	private String licenseDate;  	  	// 인허가날짜 (LocalDate 대신 문자열로 변환해서 보낼 수도 있음)
	private String phoneNumber;     	// 전화번호
	private String address;         	// 주소
    private Integer postalCode;     	// 우편번호
    private String storeName;		 	// 상호명
    private double latitude;       	 	// 위도 (double로 변환 추천)
    private double longitude;       	// 경도 (double로 변환 추천)
    private double distance;        	// 현재 위치(서울시청 등)와의 거리 (추가 필드)
}
