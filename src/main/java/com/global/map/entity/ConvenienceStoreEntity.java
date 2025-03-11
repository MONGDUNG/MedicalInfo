package com.global.map.entity;

import java.time.LocalDate;

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
@Table(name = "CONVENIENCESTORE")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ConvenienceStoreEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID는 AUTO_INCREMENT니까 이거 맞춰줌
	
	// 번호
    @Column(name = "ID")
    private Long id;
    
	// 관리번호
    @Column(name = "STORE_CODE", length = 50, nullable = false)
    private String storeCode;
    
    // 인허가날짜
    @Column(name = "LICENSE_DATE", nullable = false)
    private LocalDate licenseDate;
    
    // 전화번호
    @Column(name = "PHONE_NUMBER", length = 20)
    private String phoneNumber;
    
    // 주소
    @Column(name = "ADDRESS", length = 200, nullable = false)
    private String address;
    
    // 우편번호
    @Column(name = "POSTAL_CODE", nullable = false)
    private Integer postalCode;

    // 개설일자
    @Column(name = "STORE_NAME", length = 100, nullable = false)
    private String storeName;

    //위도
    @Column(name = "LATITUDE", length = 20, nullable = false)
    private String latitude;
    
    //경도
    @Column(name = "LONGITUDE", length = 20, nullable = false)
    private String longitude;
}
