package com.global.map.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.global.map.dto.ReviewDTO;
import com.global.map.entity.ReviewEntity;
import com.global.map.repository.ReviewRepository;
import com.global.member.entity.MemberEntity;
import com.global.member.repository.MemberRepository;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder
@Service
@RequiredArgsConstructor // final이 붙은 필드를 파라미터로 받는 생성자를 만들어줌
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final MapService mapService;
	private final MemberRepository memberRepository;
	
	public void saveReview(ReviewDTO reviewDTO, MemberEntity member) {

		ReviewEntity reviewEntity = ReviewEntity.builder()
				.hospitalCode(reviewDTO.getHospitalCode())
				.reviewDate(reviewDTO.getReviewDate())
				.rating(reviewDTO.getRating())
				.reviewText(reviewDTO.getReviewText())
				.reviewerName(reviewDTO.getReviewerName())
				.reviewName(reviewDTO.getReviewName())
				.member(member)
				.build();
		reviewRepository.save(reviewEntity);
	}
	public void deleteReview(Long id) {
		reviewRepository.deleteById(id);
	}
	@Transactional
	public void updateReview(ReviewDTO reviewDTO) {
	    // 1. 기존 리뷰 찾기 (예외 처리 포함)
	    ReviewEntity reviewEntity = reviewRepository.findById(reviewDTO.getId())
	        .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다. ID: " + reviewDTO.getId()));

	    // 2. 값 변경
	    reviewEntity.update(
	        reviewDTO.getHospitalCode(),
	        reviewDTO.getReviewDate(),
	        reviewDTO.getRating(),
	        reviewDTO.getReviewText(),
	        reviewDTO.getReviewerName()
	    );

	    // 3. save 호출 생략 가능 (JPA가 dirty checking으로 자동 반영)
	}
	public List<ReviewDTO> getReviewList(String hospitalName, String address) {
		String hospitalCode = mapService.findHCdByHNmAndAdr(hospitalName, address);
		List<ReviewEntity> reviewEntityList = reviewRepository.findByHospitalCode(hospitalCode);
		List<ReviewDTO> dtoList = reviewEntityList.stream()
			    .map(ReviewDTO::new) // 생성자 매핑
			    .collect(Collectors.toList());
		return dtoList;
	}
}
