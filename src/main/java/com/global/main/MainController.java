package com.global.main;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
}
  