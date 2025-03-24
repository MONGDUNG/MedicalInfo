package com.global.disease.entity;

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
@Table(name = "disease")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiseaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;  //칼럼 추가
	
	private String category; //대분류
	
	@Column(nullable = false)
	private String subCategory;  //소분류
	
	@Column(nullable = false)
	private String gender; //성별
		
	@Column(nullable = false)
	private String diseaseName; //질병명
	
	@Column(length = 1000)
	private String symptom;  //증상
	
	@Column(length = 1000)
	private String related;  //관련질환
	
	@Column(length = 1000)
	private String mediDepart;  //진료과
	
	@Column(length = 1000)
	private String synonyms;  //동의어
	
	private int code1;   //진료과코드1
	
	private int code2;   //진료과코드2
	
	private int code3;   //진료과코드3
	
	private int code4;   //진료과코드4
	
	private int code5;   //진료과코드5
	
	
	
}
