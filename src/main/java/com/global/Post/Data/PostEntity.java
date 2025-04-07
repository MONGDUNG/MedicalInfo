package com.global.Post.Data;

import java.lang.reflect.Member;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.global.member.entity.MemberEntity;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name="Post")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class PostEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id; 
	private String subject; 
	@Column(length = 4000)
	private String content; 	
	private LocalDateTime createDate; 	 
	
	
	@OneToMany(mappedBy = "post",cascade= CascadeType.REMOVE)
	private List<FileEntity> FileList;
	
	@OneToMany(mappedBy = "post",cascade= CascadeType.REMOVE)  
	private List<ReplEntity> ReplList; 
	
	@ManyToOne  		//1명이 질문 여러개 가능
	private MemberEntity member;
	private String category;
	
}
