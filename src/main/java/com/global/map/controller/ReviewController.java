package com.global.map.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.global.map.entity.ReviewEntity;
import com.global.map.repository.ReviewRepository;
import com.global.member.entity.MemberEntity;
import com.global.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;


@Controller
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {
	
	private final ReviewRepository reviewRepository;
	private final MemberRepository memberRepository;
	
	@PostMapping
	public ResponseEntity<ReviewEntity> saveReview(@RequestParam Long memberId, @RequestBody ReviewEntity review){
		MemberEntity member = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다. id="));
		review.setMember(member); // ✅ member 설정
        review.setReviewDate(java.time.LocalDateTime.now()); // 리뷰 날짜 자동 입력

        ReviewEntity savedReview = reviewRepository.save(review);
        return ResponseEntity.ok(savedReview);
    }
}
