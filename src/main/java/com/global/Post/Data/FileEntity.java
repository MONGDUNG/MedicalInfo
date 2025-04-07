package com.global.Post.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.global.member.entity.MemberEntity;

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
@Table(name="File")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class FileEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String filename;
	private String path;
	@ManyToOne
	private PostEntity post;

}
