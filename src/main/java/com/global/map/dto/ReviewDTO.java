package com.global.map.dto;

import java.time.LocalDateTime;

import com.global.map.entity.ReviewEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ReviewDTO {
	private Long id;
    private String hospitalCode;
    private LocalDateTime reviewDate;
    private int rating;
    private String reviewText;
    private String reviewerName;
    private String reviewName;

    // 생성자: Entity → DTO 변환용
    public ReviewDTO(ReviewEntity entity) {
    	this.id = entity.getId(); // id가 null되서 추가함
        this.hospitalCode = entity.getHospitalCode();
        this.reviewDate = entity.getReviewDate();
        this.rating = entity.getRating();
        this.reviewText = entity.getReviewText();
        this.reviewerName = entity.getReviewerName(); // 멤버 넣으면 삭제
    }
}