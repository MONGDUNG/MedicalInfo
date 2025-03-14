package com.global.map.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "MEDINFO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedInfoEntity {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 자동 증가 ID

    @Column(name = "HOSPITAL_CODE", length = 200, nullable = false)
    private String hospitalCode;

    @Column(name = "HOSPITAL_NAME", length = 200, nullable = false)
    private String hospitalName;

    @Column(name = "MON", length = 50)
    private String mon;

    @Column(name = "TUES", length = 50)
    private String tues;

    @Column(name = "WEDNES", length = 50)
    private String wednes;

    @Column(name = "THURS", length = 50)
    private String thurs;

    @Column(name = "FRI", length = 50)
    private String fri;

    @Column(name = "SATUR", length = 50)
    private String satur;

    @Column(name = "SUN", length = 50)
    private String sun;
}
