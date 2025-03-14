package com.global.map.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedInfoDTO {
	private String hospitalCode;  // 병원 코드
    private String hospitalName;  // 병원명
    private String mon;           // 월요일 운영 시간
    private String tues;          // 화요일 운영 시간
    private String wednes;        // 수요일 운영 시간
    private String thurs;         // 목요일 운영 시간
    private String fri;           // 금요일 운영 시간
    private String satur;         // 토요일 운영 시간
    private String sun;           // 일요일 운영 시간
}
