package com.global.notice.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.global.notice.dto.AnswerDTO;
import com.global.notice.entity.AnswerEntity;
import com.global.notice.entity.CategoryEntity;
import com.global.notice.entity.SupplementEntity;
import com.global.member.dto.MemberDTO;
import com.global.member.entity.MemberEntity;
import com.global.member.repository.MemberRepository;
import com.global.notice.repository.AnswerRepository;
import com.global.notice.repository.CategoryRepository;
import com.global.notice.repository.SupplementRepository;

@Service
public class AnswerService {

	@Autowired
	private AnswerRepository answerRepository;
	
	@Autowired
	private SupplementRepository supplementRepository;
	
	@Autowired
	private MemberRepository memberRepository;
	
	@Autowired
	private CategoryRepository categoryRepository;
	
	public void answerCreateCategory(Long categoryId, String content , String username) {
		if (content == null || content.trim().isEmpty()) {
			return; 
		}
		CategoryEntity ce = categoryRepository.findById(categoryId).get();
		MemberEntity me = memberRepository.findByUsername(username).get();
		AnswerEntity ae = AnswerEntity.builder()
				.content(content)
				.createDate(LocalDateTime.now())
				.category(ce)
				.supplement(null)
				.member(me)
				.build();
		
        answerRepository.save(ae);
        ce.getAnswerList().add(ae);
        categoryRepository.save(ce);
    }
	
	public void answerCreateSupplement(Long supplementId, String content , String username) {
		if (content == null || content.trim().isEmpty()) {
			return; 
		}
		SupplementEntity se = supplementRepository.findById(supplementId).get();
		MemberEntity me = memberRepository.findByUsername(username).get();
		AnswerEntity ae = AnswerEntity.builder()
				.content(content)
				.createDate(LocalDateTime.now())
				.category(null)
				.supplement(se)
				.member(me)
				.build();
		 
        answerRepository.save(ae); 
        se.getAnswerList().add(ae); 
        supplementRepository.save(se);
    }
	
	public void deleteAnswer(Integer answerId, String username) {
	    AnswerEntity answer = answerRepository.findById(answerId)
	            .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

	    if (!answer.getMember().getUsername().equals(username)) {
	        throw new RuntimeException("삭제 권한이 없습니다.");
	    }

	    answerRepository.delete(answer);
	}
	
	public AnswerDTO convertToDTO(AnswerEntity entity) {
	    MemberDTO memberDTO = null;

	    if (entity.getMember() != null) {
	        memberDTO = MemberDTO.builder()
	            .username(entity.getMember().getUsername())
	            .build();
	    }

	    return AnswerDTO.builder()
	        .id(entity.getId())
	        .content(entity.getContent())
	        .createDate(entity.getCreateDate())
	        .modifyDate(entity.getModifyDate())
	        .memberDTO(memberDTO)
	        .build();
	}
}