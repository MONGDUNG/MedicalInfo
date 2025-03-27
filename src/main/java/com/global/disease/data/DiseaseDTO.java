package com.global.disease.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiseaseDTO {
	
	private long id;  //칼럼 추가
	private String category; //대분류
	private String subCategory;  //소분류
	private String gender; //성별
	private String diseaseName; //질병명
	private String symptom;  //증상
	private String related;  //관련질환
	private String mediDepart;  //진료과
	private String synonyms;  //동의어
	private int code1;   //진료과코드1
	private int code2;   //진료과코드2
	private int code3;   //진료과코드3
	private int code4;   //진료과코드4
	private int code5;   //진료과코드5
}
