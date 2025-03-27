package com.global.member.entity;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Table(name="member")
public class MemberEntity {

	
		
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id; //id 번호
						
		@Column(unique = true)
		private String username; //id
		private String password; //비번
		private String name; //이름
		private String address; //주소
		private String detailaddress; //상세주소
		
		//문동규추가
		private Double latitude; // 위도
	    private Double longitude; // 경도
		
		private String postcode; //우편번호
		private Date birth; //생일
		@Column(unique = true) 
		private String email;	//e메일로 id , password 조회 중복시 에러 
		private String sex; //성별
		private String underlyingcondition; //기저질환
		private LocalDateTime lastlogindate; //마지막 로그인		
		private Integer signuppath; //가입경로
		private String memberstatus;   // 10 일반, 20,21 의사,약사 40 관리자
		
		
		
		@OneToOne(cascade= CascadeType.REMOVE)
		private ClassificationEntity classiId; 
		
		@ManyToOne 
		private MemberTierEntity tierId;
		
		
		

}


