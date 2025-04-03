package com.global.ask;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.global.member.entity.MemberEntity;
import com.global.member.repository.MemberRepository;

@Service
public class AnswerService2 {
	
	@Autowired
	private AnswerRepository2 answerRepository;
	
	@Autowired
	private QuestionRepository questionRepository;
	
	@Autowired
	private MemberRepository siteUserRepository;
	
	public void answerCreate(Integer questionId , String content , String username) {
		QuestionEntity qe = questionRepository.findById(questionId).get();
		MemberEntity se = siteUserRepository.findByUsername(username).get();
		AnswerEntity2 ae = AnswerEntity2.builder()
				.content(content)
				.createDate(LocalDateTime.now())
				.question(qe)
				.siteUser(se)
				.build();
		answerRepository.save(ae);
	}
}
