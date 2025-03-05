package com.global.map.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"Emergency\"")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class EmergencyEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID는 AUTO_INCREMENT니까 이거 맞춰줌
	
	// 번호
    @Column(name = "ER_ID")
    private Long erId;
	
	// 기관명
    @Column(name = "HOSPITAL_NAME", length = 200, nullable = false)
    private String hospitalName;

    // 전화
    @Column(name = "PHONE_NUMBER", length = 50, nullable = false)
    private String phoneNumber;
    
    // 주소
    @Column(name = "ADDRESS", length = 300, nullable = false)
    private String address;

    // 월요일 진료
    @Column(name = "MON", length = 50, nullable = false)
    private String mon;
 
    // 화요일 진료
    @Column(name = "TUES", length = 50, nullable = false)
    private String tues;
    
    // 수요일 진료    
    @Column(name = "WEDNES", length = 50, nullable = false)
    private String wednes;
    
    // 목요일 진료    
    @Column(name = "THURS", length = 50, nullable = false)
    private String thurs;
    
    // 금요일 진료    
    @Column(name = "FRI", length = 50, nullable = false)
    private String fri;
    
    // 토요일 진료    
    @Column(name = "SATUR", length = 50, nullable = false)
    private String satur;
    
    // 일요일 진료    
    @Column(name = "SUN", length = 50, nullable = false)
    private String sun;
    
    // 공휴일 진료
    @Column(name = "HOLI", length = 50, nullable = false)
    private String holi;
    
    //위도
    @Column(name = "LATITUDE", length = 20, nullable = false)
    private String latitude;
    
    //경도
    @Column(name = "LONGITUDE", length = 20, nullable = false)
    private String longitude;
}
