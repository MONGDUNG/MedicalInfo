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

@Controller // ✅ 뷰 반환을 위해 @Controller 사용
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {
	
	private final ReviewRepository reviewRepository;
	private final MemberRepository memberRepository;
	
	// ✅ 리뷰 작성 페이지 (병원 코드 입력을 받을 수 있도록 수정!)
	@GetMapping("/write/{hospitalCode}")
	public String reviewWritePage(@PathVariable String hospitalCode, Model model) {
		model.addAttribute("hospitalCode", hospitalCode);
		return "reviewWrite"; // reviewWrite.html 페이지 반환
	}

	// ✅ 리뷰 저장 처리 (병원 코드도 함께 받아서 설정하도록 변경!)
	@PostMapping("/save")
	public String saveReview(
			@RequestParam Long memberId,
			@RequestParam String hospitalCode, // ✅ 병원 코드 입력 받음
			@ModelAttribute ReviewEntity review,
			Model model) {
		
		MemberEntity member = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다. ID: " + memberId));

		review.setMember(member); // ✅ 회원 정보 설정
		review.setHospitalCode(hospitalCode); // ✅ 병원 코드 설정 (수정된 부분)

		reviewRepository.save(review); // ✅ JPA가 reviewDate 자동 입력

		model.addAttribute("message", "리뷰가 성공적으로 저장되었습니다.");
		return "redirect:/review/list/" + hospitalCode; // ✅ 해당 병원의 리뷰 목록으로 이동
	}

	// ✅ 특정 병원의 리뷰 목록 페이지 (예외 처리 추가)
	@GetMapping("/list/{hospitalCode}")
	public String getReviews(@PathVariable String hospitalCode, Model model) {
		List<ReviewEntity> reviews = reviewRepository.findByHospitalCode(hospitalCode);

		// ✅ 리뷰가 없을 경우 메시지 추가
		if (reviews.isEmpty()) {
			model.addAttribute("message", "아직 작성된 리뷰가 없습니다.");
		}

		model.addAttribute("reviews", reviews);
		model.addAttribute("hospitalCode", hospitalCode); // ✅ 병원 코드도 함께 전달
		return "reviewList"; // ✅ reviewList.html 페이지 반환
	}
}
