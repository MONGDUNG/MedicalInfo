package com.global.disease.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Disease {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;               // 질병 이름
    private String description;        // 질병 설명
    private int severity;              // 심각도 (1~5)
    private String treatmentSummary;   // 치료 요약
    private String medicalDepartment;  // 진료과
    private String recommendation;     // 조언
}