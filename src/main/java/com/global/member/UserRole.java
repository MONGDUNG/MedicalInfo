package com.global.member;

import lombok.Getter;



@Getter
public enum UserRole {  // enum (열거형)나열한다.
	
	ADMIN("ROLE_ADMIN"),
	DOCTOR("ROLE_DOCTOR"),
	PM("ROLE_PM"),  //PM 약사
	USER("ROLE_USER");
		
	private String value;
	UserRole(String value){
		this.value=value;
	}
}


