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
@Table(name = "\"PHARMACY\"")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class PharmacyEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	
	// 번호
    @Column(name = "ID")
	private Long id;
	
	// 약국이름
	@Column(name = "PHARMACY_NAME", length = 100, nullable = false)
	private String pharmacyName;
	
	// 약국주소
	@Column(name = "ADDRESS", length = 200, nullable = false)
	private String address;
	
	// 약국 전화번호
	@Column(name = "PHONE_NUMBER", length = 20)
	private String phoneNumber;
	
	// 우편번호
	@Column(name = "POSTAL_CODE", nullable = false)
	private Integer postalcode;
	
	//위도
	@Column(name = "LATITUDE", length = 20, nullable = false)
	private String latitide;
	
	// 경도
	@Column(name = "LONGITUDE", length = 20, nullable = false)
	private String longitude;
	
	// 개설일자
	@Column(name = "ESTABLISHED_DATE", length = 20, nullable = false)
    private String establishedDate;
}
