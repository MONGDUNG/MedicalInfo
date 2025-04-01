package com.global.member.dto;

import java.sql.Date;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class MemberDTO {
	
		private Integer id;
		private String username;
		private String password;		 
		private String name; //이름
		private String address; //주소
		private String detailaddress; //상세주소
		
		//문동규추가
		private Double latitude; // 위도
	    private Double longitude; // 경도
		
		private String postcode; //우편번호
		private Date birth; //생일
		private String sex; //성별
		private String email;
		private String underlyingcondition; //기저질환
		private LocalDateTime lastlogindate; //마지막 로그인		
		private Integer signuppath; //가입경로
		private String memberstatus;
		private String tier;
		private String tiername;
		
		private MemberTierDTO TierId;
		private ClassificationDTO classiId;
}


