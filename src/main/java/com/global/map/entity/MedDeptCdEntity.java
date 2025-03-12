package com.global.map.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "MEDDEPT_CD")
@NoArgsConstructor 
@AllArgsConstructor
@Builder
@Data
public class MedDeptCdEntity {

	@Id
	@Column(name = "DEPARTMENT_CODE", length = 50, nullable = false)
	private Integer DeptCd;

	@Column(name = "DEPARTMENT_NAME", length = 50, nullable = false)
	private String DeptName;
}
