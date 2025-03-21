package com.global.member.entity;

import java.util.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
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
@Table(name="class")
public class ClassificationEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id; //id 번호
	
	@OneToOne
	private MemberEntity memberId; // 상태

	@OneToOne
	private MemberTierEntity tierId;
	
	private LocalDateTime bandate;
	private LocalDateTime unbandate;
	private String username;
}
