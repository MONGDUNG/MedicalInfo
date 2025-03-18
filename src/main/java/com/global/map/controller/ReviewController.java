package com.global.map.controller;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.global.map.entity.ReviewEntity;
import com.global.map.repository.ReviewRepository;
import com.global.member.entity.MemberEntity;
import com.global.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;

@Controller // ✅ 뷰 반환을 위해 @RestController → @Controller 변경!
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {
	
	private final ReviewRepository reviewRepository;
	private final MemberRepository memberRepository;
	
	
	@GetMapping("/write")
	public String reviewWritePage() { // ✅ 리뷰 저장 페이지 (리뷰 작성 폼)
		return "reviewWrite"; // reviewWrite.html 페이지 반환
	}

	
	@PostMapping("/save")
	public String saveReview(@RequestParam Long memberId, @ModelAttribute ReviewEntity review, Model model) { // ✅ 리뷰 저장 처리 (폼 데이터 받기)
		MemberEntity member = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다. ID: " + memberId));

		review.setMember(member);
        review.setReviewDate(java.time.LocalDateTime.now());

        reviewRepository.save(review);
        
        model.addAttribute("message", "리뷰가 성공적으로 저장되었습니다.");
        return "redirect:/review/list"; // 리뷰 목록 페이지로 리다이렉트
    }

	// ✅ 특정 병원의 리뷰 목록 페이지
	@GetMapping("/list/{hospitalCode}")
	public String getReviews(@PathVariable String hospitalCode, Model model) {
		List<ReviewEntity> reviews = reviewRepository.findByHospitalCode(hospitalCode);
		model.addAttribute("reviews", reviews);
		return "reviewList"; // reviewList.html 페이지 반환
	}
}
