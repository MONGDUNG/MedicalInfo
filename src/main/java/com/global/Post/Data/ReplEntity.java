package com.global.Post.Data;

import java.time.LocalDateTime;

import com.global.member.entity.MemberEntity;
import com.global.member.entity.MemberTierEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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
@Table(name="Repl")
public class ReplEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id; 
	private String content;
	private LocalDateTime createdate;
	/*
	 * @ManyToOne private MemberTierEntity Tier;
	 */
	@ManyToOne
	private MemberEntity member;
	@ManyToOne
	private PostEntity post;
	

}
