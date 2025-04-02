package com.global.disease.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.global.disease.service.DiseaseService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/disease/")
@RequiredArgsConstructor
public class DiseaseController {

	private final DiseaseService diseaseService;
	
	@GetMapping("list")
	public String DList(Model model, @RequestParam(value="page", defaultValue="0") Integer page,
									 @RequestParam(value="keyword", defaultValue="") String keyword){
			model.addAttribute("list", diseaseService.diseasePage(page, keyword));
			model.addAttribute("count", diseaseService.totalCount(keyword));
			model.addAttribute("keyword", keyword);
		return "disease/search/disease_list";
	}
	
	@GetMapping("self_diagnosis")
	public String diagnosis() {
		return "disease/search/self_diagnosis";
	}
}
