package com.global.member.entity;


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
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Table(name="tier")
public class MemberTierEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id; //id 번호
	private String tier;
	private String tiername;	
}
