package com.global.notice.dto;

import java.time.LocalDateTime;

import com.global.member.dto.MemberDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class AnswerDTO {
	private Integer id;
	private String content; 
	private LocalDateTime createDate;
	private LocalDateTime modifyDate;
	private SupplementDTO supplementDTO;
    private CategoryDTO categoryDTO;
}