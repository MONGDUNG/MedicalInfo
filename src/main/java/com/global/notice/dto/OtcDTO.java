package com.global.notice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtcDTO {
	private Integer id;						// 번호
	private String medicineType;    		// 구분
    private String medicineName;     		// 품목명
    private String packageUnit;        		// 포장단위
    private String img; 					// 이미지
    private String indications;				// 효능
    private String dosageInstructions;		// 복용방법
    private String precautions;				// 주의사항
    private String manufacturer;			// 제조사
}
