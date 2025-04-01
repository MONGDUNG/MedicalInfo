package com.global.member.naver;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MsgEntity<T> {
	
	private String message; // 응답 메시지
    private T data;         // 데이터 (제네릭 타입)


}