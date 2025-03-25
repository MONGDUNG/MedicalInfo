package com.global.map.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
    public String getHospitalCode(@RequestParam("name") String name, @RequestParam("address") String address) {
        return mapService.findHCdByHNmAndAdr(name, address);
    }

    // ✅ 리뷰 작성 페이지
    @GetMapping("/write/{hospitalCode}")
    public String reviewWritePage(@PathVariable("hospitalCode") String hospitalCode, Model model) {
        model.addAttribute("hospitalCode", hospitalCode);
        return "map/reviewWrite";
    }

    // ✅ 리뷰 저장 처리
    @PostMapping("/save")
    public String saveReview(
            @RequestParam("memberId") Integer memberId,
            @RequestParam("hospitalCode") String hospitalCode,
            @RequestParam("reviewerName") String reviewerName, // 멤버나오면 지워
            @RequestParam("reviewName") String reviewName, // 멤버나오면 지워
            @ModelAttribute ReviewEntity review,
            @RequestParam("name") String name,
            @RequestParam("address") String address,
            @RequestParam("phone") String phone,
            @RequestParam("lat") String lat,
            @RequestParam("lng") String lng,
            @RequestParam("category") String category,
            Model model) {

        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다. ID: " + memberId));

        review.setMember(member);
        review.setHospitalCode(hospitalCode);
        review.setReviewDate(LocalDateTime.now());
        review.setReviewerName(reviewerName); // 멤버나오면 지워
        review.setReviewName(reviewName);

        reviewRepository.save(review);

        model.addAttribute("message", "리뷰가 성공적으로 저장되었습니다.");
        
        return "redirect:/map/hospitaldetail?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8)
        + "&address=" + URLEncoder.encode(address, StandardCharsets.UTF_8)
        + "&phone=" + URLEncoder.encode(phone, StandardCharsets.UTF_8)
        + "&lat=" + URLEncoder.encode(lat, StandardCharsets.UTF_8)
        + "&lng=" + URLEncoder.encode(lng, StandardCharsets.UTF_8)
        + "&category=" + URLEncoder.encode(category, StandardCharsets.UTF_8);
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
        return "map/reviewWrite";
    }

    @GetMapping("/list")
    @ResponseBody
    public List<ReviewDTO> getReviews(
        @RequestParam("hospitalName") String hospitalName,
        @RequestParam("address") String address) {

        String hospitalCode = mapService.findHCdByHNmAndAdr(hospitalName, address);
        
        return reviewRepository.findByHospitalCode(hospitalCode).stream()
                .map(ReviewDTO::new)
                .toList();
    }
    
    @GetMapping("/hospital/detail/{hospitalCode}")
    public String showHospitalDetail(@PathVariable("hospitalCode") String hospitalCode, Model model) {
        List<ReviewEntity> reviews = reviewRepository.findByHospitalCode(hospitalCode);
        model.addAttribute("reviews", reviews);
        model.addAttribute("hospitalCode", hospitalCode);
        return "map/hospitalDetail";
    }
}
