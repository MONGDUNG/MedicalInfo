package com.global.map.dto;

import java.time.LocalDateTime;

import com.global.map.entity.ReviewEntity;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class ReviewDTO {

    private String hospitalCode;
    private LocalDateTime reviewDate;
    private int rating;
    private String reviewText;
    private String reviewerName;

    // 생성자: Entity → DTO 변환용
    public ReviewDTO(ReviewEntity entity) {
        this.hospitalCode = entity.getHospitalCode();
        this.reviewDate = entity.getReviewDate();
        this.rating = entity.getRating();
        this.reviewText = entity.getReviewText();
        this.reviewerName = entity.getReviewerName(); // 멤버 넣으면 삭제
    }
}