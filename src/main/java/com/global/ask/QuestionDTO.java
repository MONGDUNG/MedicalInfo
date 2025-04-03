package com.global.ask;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDTO {
	private Integer id; //질문의 고유 식별자
	private String content; //질문의 내용
	private LocalDateTime createDate; //날짜와 시간
	private String filename; //파일이름
	private LocalDateTime modifyDate; //수정된 날짜와 시간
	private String subject; //제목
	private Integer site_user_id; //유저의 고유id(권한)
	private String visibility; //질문 공개 여부
	private String username; //id (이름은 보통 name으로 씀)
	private List<AnswerDTO2> answerList; //질문에 달린 답변들
	private Integer voter;
}
