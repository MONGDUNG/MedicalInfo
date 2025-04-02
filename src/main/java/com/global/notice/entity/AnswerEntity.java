package com.global.notice.entity;

import java.time.LocalDateTime;
import java.util.Set;

import com.global.member.entity.MemberEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="answer")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class AnswerEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;			//글번호
	
	@Column(length = 4000)
	private String content;		//내용
	
	private	LocalDateTime createDate;	//작성날짜
	private LocalDateTime modifyDate;	//수정날짜
	
	@ManyToOne
	private MemberEntity member;	// 작성자

	@ManyToOne(optional = true) // category가 null일 수도 있음
	private CategoryEntity category;

	@ManyToOne(optional = true) // supplement가 null일 수도 있음
	private SupplementEntity supplement;

	@ManyToMany
	private Set<MemberEntity> voter;
	
}