package com.global.Post.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.global.member.dto.MemberDTO;

import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class FileDTO {
	
	private Integer id;
	private String filename;
	private String path;
	private PostDTO post;

}
