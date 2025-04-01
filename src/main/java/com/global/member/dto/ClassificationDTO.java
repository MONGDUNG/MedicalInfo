package com.global.member.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ClassificationDTO {

	
	private Integer id; //id 번호
	private Integer memberstatus; // 상태
	private LocalDateTime bandate; //제재 받은 날짜
	private LocalDateTime unbandate; // 제재 풀린 날짜
	private String username;
	private MemberDTO memberId;
	private MemberTierDTO tierId;

}

