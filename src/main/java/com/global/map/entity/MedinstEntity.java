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
@Table(name = "MEDINST")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MedinstEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID는 AUTO_INCREMENT니까 이거 맞춰줌
    @Column(name = "ID")
    private Long id;
	
    @Column(name = "HOSPITAL_CODE", length = 100, nullable = false)
    private String hospitalCode;

    @Column(name = "HOSPITAL_NAME", length = 200, nullable = false)
    private String hospitalName;

    @Column(name = "CATEGORY_CODE", length = 50, nullable = false)
    private String categoryCode;

    @Column(name = "POSTAL_CODE", nullable = false)
    private Integer postalCode;

    @Column(name = "ADDRESS", length = 300, nullable = false)
    private String address;

    @Column(name = "PHONE_NUMBER", length = 50, nullable = false)
    private String phoneNumber;

    @Column(name = "WEBSITE", length = 200)
    private String website;

    @Column(name = "ESTABLISHED_DATE")
    private LocalDate establishedDate;

    @Column(name = "TOTAL_DOC")
    private Integer totalDoc;

    //위도
    @Column(name = "LATITUDE", length = 20, nullable = false)
    private String latitude;
    
    //경도
    @Column(name = "LONGITUDE", length = 20, nullable = false)
    private String longitude;
	
    //default 0 으로 설정
    
    @Column(name = "REVIEW_COUNT", columnDefinition = "INT default 0")
    private Integer reviewCount;
    
    @Column(name = "RATING_AVG", columnDefinition = "DOUBLE default 0.0")
    private Double ratingAvg;

}
