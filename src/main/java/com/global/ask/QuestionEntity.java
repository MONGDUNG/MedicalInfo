package com.global.ask;

import java.time.LocalDateTime;
import java.util.List;
import com.global.member.entity.MemberEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="question")
@NoArgsConstructor//
@AllArgsConstructor//이거 두개는 밑에 내용이 암것도 없으면 오류뜬다.
@Data
@Builder
public class QuestionEntity {
	@Id //PK. Answer의 ManyToOne이 이것을 참조한다.
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id; //글번호
	
	private String subject; //제목
	
	@Column(length = 4000)
	private String content; //내용
	
	
	private String filename; //파라미터명이 save로 돼있어서 이건 save로 선언하면 안됨
	
	private LocalDateTime createDate; //작성날짜
	private LocalDateTime modifyDate; //수정날짜. 보통 수정날짜를 기록하는게 보편적이라 이걸로 비번 변경한지 30일 지났습니다. 같은 알림이 뜸
	
	@OneToMany(mappedBy="question" , cascade = CascadeType.REMOVE) //mappedBy는 어떤 컬럼하고 연결할지를 정하는것. cascade = CascadeType.REMOVE 이 코드로 인해 질문이 삭제될시 답변들도 전부 삭제
			  //ManyToOne의 question과 연결
	private List<AnswerEntity2> answerList;
	
	@ManyToOne //한명의 사용자가 여러개의 질문 글을 쓸수 있어야함
	private MemberEntity siteUser;
	
	// 추가된 부분: visibility 컬럼 (PUBLIC, PRIVATE 값 저장)
	@Column(nullable = false, length = 8) // null값을 가질 수 없고 글자 수 제한은 8임
	private String visibility; // 공개/비공개 설정
	
	private String username; // 사용자의 아이디
	
}