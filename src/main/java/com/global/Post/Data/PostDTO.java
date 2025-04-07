package com.global.Post.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.global.member.dto.MemberDTO;
import com.global.member.entity.MemberEntity;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class PostDTO {
	
	
	private Integer id; //글번호
	private String subject; //제목
	private String content; //내용	
	private LocalDateTime createDate; //작성날자		
	private List<FileDTO> FileList;
	private List<ReplDTO> ReplList; 	 		
	private MemberDTO member;
	private String category;

}
