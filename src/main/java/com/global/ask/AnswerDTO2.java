package com.global.ask;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerDTO2 {
	private Integer id; //글번호
	private String content; //내용
	private LocalDateTime createDate; //작성날짜
	private LocalDateTime modifyDate; //수정날짜. 보통 수정날짜를 기록하는게 보편적이라 이걸로 비번 변경한지 30일 지났습니다. 같은 알림이 뜸
	private Integer question_id;
	private Integer site_user_id;
}
