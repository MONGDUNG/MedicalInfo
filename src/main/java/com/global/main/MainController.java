package com.global.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.global.member.dto.MemberDTO;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
public class MainController {
	
	@GetMapping("/")
	public String main(Model model) {
		MemberDTO dto = new MemberDTO();
		dto.setAddress("비로그인");
		model.addAttribute("dto", dto);
		
		return "main";
	}
	@GetMapping("/api/news")
	@ResponseBody
	public ResponseEntity<?> getNewsJson() {
		try {
			// Windows 절대경로: 로컬에 저장된 최신 JSON 읽기
			File file = new File("C:/Users/14A/mohw_news.json");
			String json = Files.readString(file.toPath());
			return ResponseEntity.ok().body(json);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			                     .body("❌ JSON 파일 읽기 실패");
		}
}
}