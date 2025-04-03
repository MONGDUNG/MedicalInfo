package com.global.ask;

import java.time.LocalDateTime;

import com.global.member.entity.MemberEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="answer2")
@NoArgsConstructor//
@AllArgsConstructor//이거 두개는 밑에 내용이 암것도 없으면 오류뜬다.
@Data
@Builder
public class AnswerEntity2 {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id; //글번호
	
	@Column(length = 4000)
	private String content; //내용
	
	private LocalDateTime createDate; //작성날짜
	private LocalDateTime modifyDate; //수정날짜. 보통 수정날짜를 기록하는게 보편적이라 이걸로 비번 변경한지 30일 지났습니다. 같은 알림이 뜸
	
	@ManyToOne //질문은 한갠데 답변은 여러개. 참조키(외래키 - FK) Question의 Id를 참조한단뜻. Id의 값으로 지정돼있는것만 참조해서 지정가능하다.
	private QuestionEntity question; //OneToMany의 question과 연결
	
	@ManyToOne //답변을 누가 쓰는지 넣어줌
	private MemberEntity siteUser;
	
}