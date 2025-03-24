package com.global.map.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.global.map.dto.ReviewDTO;
import com.global.map.entity.ReviewEntity;
import com.global.map.repository.ReviewRepository;
import com.global.map.service.MapService;
import com.global.member.entity.MemberEntity;
import com.global.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final MapService mapService;

    // ✅ 병원명 + 주소로 병원 코드 조회 (AJAX용)
    @GetMapping("/code")
    @ResponseBody
    public String getHospitalCode(@RequestParam String name, @RequestParam String address) {
        return mapService.findHCdByHNmAndAdr(name, address);
    }

    // ✅ 리뷰 작성 페이지
    @GetMapping("/write/{hospitalCode}")
    public String reviewWritePage(@PathVariable String hospitalCode, Model model) {
        model.addAttribute("hospitalCode", hospitalCode);
        return "reviewWrite";
    }

    // ✅ 리뷰 저장 처리
    @PostMapping("/save")
    public String saveReview(
            @RequestParam Integer memberId,
            @RequestParam String hospitalCode,
            @ModelAttribute ReviewEntity review,
            Model model) {

        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다. ID: " + memberId));

        review.setMember(member);
        review.setHospitalCode(hospitalCode);

        reviewRepository.save(review);

        model.addAttribute("message", "리뷰가 성공적으로 저장되었습니다.");
        return "redirect:/review/list/" + hospitalCode;
    }

    // ✅ 특정 병원의 리뷰 목록 페이지
    @GetMapping("/list/{hospitalCode}")
    public String getReviews(@PathVariable String hospitalCode, Model model) {
        List<ReviewEntity> reviews = reviewRepository.findByHospitalCode(hospitalCode);

        if (reviews.isEmpty()) {
            model.addAttribute("message", "아직 작성된 리뷰가 없습니다.");
        }

        model.addAttribute("reviews", reviews);
        model.addAttribute("hospitalCode", hospitalCode);
        return "reviewList";
    }

    @GetMapping("/list")
    @ResponseBody
    public List<ReviewDTO> getReviews(
        @RequestParam("hospitalName") String hospitalName,
        @RequestParam("address") String address) {

        String hospitalCode = mapService.findHCdByHNmAndAdr(hospitalName, address);
        System.out.println("✅ 조회된 hospitalCode: " + hospitalCode);

        return reviewRepository.findByHospitalCode(hospitalCode).stream()
                .map(ReviewDTO::new)
                .toList();
    }
}
