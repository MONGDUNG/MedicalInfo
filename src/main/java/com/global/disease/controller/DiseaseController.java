package com.global.disease.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
		return "search/disease_list";
	}
	
	@GetMapping("self_diagnosis")
	public String diagnosis() {
		return "search/self_diagnosis";
	}
	
	@GetMapping("self_diagnosis/{orderNum}")
	public String common(@PathVariable("orderNum") int orderNum, Model model , @RequestParam(value="txt",defaultValue="" ,required = false) String txt) {
		//@RequestParam(required=false) 뜻: 이것의 기본값은 null 로 정한다는 뜻이다.
		//현재 질문 가져오기
		model.addAttribute("orderNum", orderNum);
		
		return "search/common";
	}
}
