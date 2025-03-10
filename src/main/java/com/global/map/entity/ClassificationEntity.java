package com.global.map.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CLASSIFICATION")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ClassificationEntity {
	
	@Id
	@Column(name = "CATEGORY_CODE", length = 50, nullable = false)
    private Integer CategoryCode;
	
	@Column(name = "CATEGORY_NAME", length = 50, nullable = false)
    private String CategoryName;

}
