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
public class CategoryDTO {
	private Long id;               			// 번호
    private String itemStandardCode;    	// 품목기준코드
    private String productName;     		// 제품명
    private String companyName;         	// 업체명
    private String activeIngredient; 		// 주성분
    private String additives; 				// 첨가제
    private String manufacturingType; 		// 제조/수입
    private String standardCode; 			// 표준코드명
    private String drugDetailCode; 			// 약 상세정보코드
    private String countryOfImport; 		// 수입제조국
    private String indications; 			// 효능/효과
    private List<AnswerDTO> answerList;		// 댓글
}