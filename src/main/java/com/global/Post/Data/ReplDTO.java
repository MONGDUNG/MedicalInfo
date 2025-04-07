package com.global.Post.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.global.member.dto.MemberDTO;
import com.global.member.dto.MemberTierDTO;
import com.global.member.entity.MemberEntity;
import com.global.member.entity.MemberTierEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ReplDTO {
	
	private Integer id; 
	private String content;
	private LocalDateTime createdate;	
	private MemberDTO member;
	private PostDTO post;

}
