package com.global.map.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.security.Principal;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import com.global.map.dto.ReviewDTO;

import com.global.map.service.MapService;
import com.global.map.service.ReviewService;
import com.global.member.entity.MemberEntity;
import com.global.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final MemberRepository memberRepository;
    private final MapService mapService;
    private final ReviewService reviewService;


    // ✅ 리뷰 작성 페이지
    @GetMapping("/write/{hospitalCode}")

    public String reviewWritePage(@PathVariable("hospitalCode") String hospitalCode, Model model, Principal principal) {

        model.addAttribute("hospitalCode", hospitalCode);
        
        if (principal == null) {
        	return "member/login";
        	
		}
        return "map/reviewWrite";

    }

    // ✅ 리뷰 저장 처리
    @PostMapping("/save")
    public String saveReview(
            Principal principal,
            ReviewDTO reviewDTO,
            @RequestParam("name") String name,
            @RequestParam("address") String address,
            @RequestParam("phone") String phone,
            @RequestParam("lat") String lat,
            @RequestParam("lng") String lng,
            @RequestParam("category") String category,
            Model model) {

        reviewDTO.setReviewDate(LocalDateTime.now());
        String username = principal.getName();
        MemberEntity loginMember = memberRepository.findByUsername(username).orElseThrow();

        boolean alreadyReviewed = reviewService.hasReviewed(reviewDTO.getHospitalCode(), loginMember);
        if (alreadyReviewed) {
            // 병원 상세 정보도 다시 넘겨줘야 form이 재구성됨
            model.addAttribute("message", "이미 이 병원에 대한 리뷰를 작성하셨습니다.");
            model.addAttribute("name", name);
            model.addAttribute("address", address);
            model.addAttribute("phone", phone);
            model.addAttribute("lat", lat);
            model.addAttribute("lng", lng);
            model.addAttribute("category", category);
            return "map/reviewWrite";
        }

        reviewService.saveReview(reviewDTO, loginMember);

        return "redirect:/map/hospitaldetail?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8)
                + "&address=" + URLEncoder.encode(address, StandardCharsets.UTF_8)
                + "&phone=" + URLEncoder.encode(phone, StandardCharsets.UTF_8)
                + "&lat=" + URLEncoder.encode(lat, StandardCharsets.UTF_8)
                + "&lng=" + URLEncoder.encode(lng, StandardCharsets.UTF_8)
                + "&category=" + URLEncoder.encode(category, StandardCharsets.UTF_8);
    }

    // ✅ 특정 병원의 리뷰 목록 페이지
    @GetMapping("/list/{hospitalCode}")
    public String getReviews(@RequestParam("name") String name, @RequestParam("address") String address, Model model) {
        List<ReviewDTO> reviews = reviewService.getReviewList(name, address);
        String hospitalCode = mapService.findHCdByHNmAndAdr(name, address);
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
        
        List<ReviewDTO> reviews = reviewService.getReviewList(hospitalName, address);
        
        return reviews;
    }
    
    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public void deleteReview(@PathVariable("id") Long id) {
      reviewService.deleteReview(id);
   }
    
    @PutMapping("/update")
    @ResponseBody
    public void updateReview(@RequestBody ReviewDTO reviewDTO) {
       reviewService.updateReview(reviewDTO);
    }

}
