package com.global.ask;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/answer2/*")
public class AnswerController2 {
	
	@Autowired
	private AnswerService2 answerService;
	
	@PostMapping("create/{id}")
	public String create(@PathVariable("id") Integer id
						,@RequestParam("content") String content
						,Principal principal) {
		String username = principal.getName();

		answerService.answerCreate(id, content, username);

		return "redirect:/question/detail/"+id;
	}
}