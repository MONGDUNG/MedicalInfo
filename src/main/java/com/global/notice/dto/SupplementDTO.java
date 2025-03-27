package com.global.notice.dto;

import java.util.List;
import java.util.Set;

import com.global.member.entity.MemberEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplementDTO {
	 private Long id;						// 번호
	 private Long prdlstReportNo;			// 품목제조번호
	 private String prdtShapCdNm;			// 제품형태
	 private Long lcnsNo; 					// 인허가번호
	 private String prdlstNm;				// 품목명
	 private String iftknAtntMatrCn;		// 섭취시주의사항
	 private String bsshNm;					// 업체명
	 private String etcRawmtrlNm;			// 기타 원재료
	 private String primaryFnclty;			// 주된기능성
	 private String cstdyMthd;				// 보관방법
	 private String ntkMthd;				// 섭취방법
	 private String capRawmtrlNm;			// 캡슐 원재료
	 private String stdrStnd;				// 기준규격
	 private String pogDaycnt;				// 소비기한 일수
	 private String indivRawmtrlNm;			// 기능성 원재료
	 private String rawmtrlNm;				// 품목유형(기능지표성분)
	 private List<AnswerDTO> answerList;	// 댓글
	 private Integer voter;
}
