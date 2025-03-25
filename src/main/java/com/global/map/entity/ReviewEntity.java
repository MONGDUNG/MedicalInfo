package com.global.map.entity;

import java.time.LocalDateTime;

import com.global.member.entity.MemberEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hospital_review")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ReviewEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	
	
	private Long id;
	
	@Column(nullable = false)
	private String hospitalCode;
	
	@Column(nullable = false, updatable = false)
	private LocalDateTime reviewDate = LocalDateTime.now();
	
	@Column(nullable = false)
	private int rating;
	
	@Column(columnDefinition = "TEXT")
	private String reviewText;
	
	@Column(nullable = false)
	private String reviewerName;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private MemberEntity member;
	
	@Column(nullable = false)
	private String reviewName;
	
}
