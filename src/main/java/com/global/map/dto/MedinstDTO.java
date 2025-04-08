package com.global.map.dto;

import java.io.Serializable;

import com.global.map.entity.MedinstEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedinstDTO implements Serializable{
   
   private static final long serialVersionUID = 1L;

    private Long id;               // 번호
    private String hospitalCode;    // 요양기호
    private String hospitalName;    // 기관명
    private String categoryCode;    // 종별코드
    private Integer postalCode;     // 우편번호
    private String address;         // 주소
    private String phoneNumber;     // 전화번호
    private String website;         // 홈페이지
    private String establishedDate; // 개설일자 (LocalDate 대신 문자열로 변환해서 보낼 수도 있음)
    private Integer totalDoc;       // 총의사수
    private double latitude;        // 위도 (double로 변환 추천)
    private double longitude;       // 경도 (double로 변환 추천)
    private double distance;        // 현재 위치(서울시청 등)와의 거리 (추가 필드)
    
    
    public MedinstDTO(MedinstEntity entity) {
        this.hospitalCode = entity.getHospitalCode();
        this.hospitalName = entity.getHospitalName();
        this.address = entity.getAddress();
        this.phoneNumber = entity.getPhoneNumber();
        this.latitude = Double.parseDouble(entity.getLatitude());
        this.longitude = Double.parseDouble(entity.getLongitude());
    }
}

