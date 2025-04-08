package com.global.notice.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.global.notice.entity.CategoryEntity;
import com.global.notice.entity.SupplementEntity;
import com.global.notice.repository.CategoryRepository;
import com.global.notice.repository.SupplementRepository;
import com.global.notice.service.AnswerService;

@Controller
@RequestMapping("/answer/")
public class AnswerController {
	
	@Autowired
	private AnswerService answerService;
	
	 @Autowired
	    private CategoryRepository categoryRepository;
	    
	    @Autowired
	    private SupplementRepository supplementRepository;
	 
	// 의약품 댓글 작성
	@PostMapping("/create/category/{id}")
	public String createCategoryAnswer(@PathVariable("id") Long id, 
						 @RequestParam("content") String content,
						 Principal principal,
						 Model model) {
		
		if (principal == null) {
			return "redirect:/member/login?error=not_authenticated";  // 로그인 페이지로 이동
		}
        String username = principal.getName();
        
        answerService.answerCreateCategory(id, content, username);
        
        // 의약품 answerList에 답변 추가
        CategoryEntity category = categoryRepository.findById(id).get();
        model.addAttribute("categoryDTO", category);
        return "redirect:/category/detail/" + id;
    }
	
	// 영양제 댓글 작성
	@PostMapping("/create/supplement/{id}")
    public String createSupplementAnswer(@PathVariable("id") Long id, 
                             @RequestParam("content") String content,
                             Principal principal,
                             Model model) {
        
        if (principal == null) {
            return "redirect:/member/login?error=not_authenticated";  // 로그인 페이지로 이동
        }
        String username = principal.getName();
        
        answerService.answerCreateSupplement(id, content, username);
        
        // 영양제 answerList에 답변 추가
        SupplementEntity supplement = supplementRepository.findById(id).get();
        model.addAttribute("supplementDTO", supplement); // 여기에서 DTO 변환을 필요하면 처리
        
        return "redirect:/supplement/detail/" + id;
    }
	
	// 댓글 삭제
	@GetMapping("/delete/{id}")
	public String deleteAnswer(@PathVariable("id") Integer id,
	                           @RequestParam("redirectUrl") String redirectUrl,
	                           Principal principal) {

	    if (principal == null) {
	        return "redirect:/member/login?error=not_authenticated";
	    }

	    answerService.deleteAnswer(id, principal.getName());
	    return "redirect:" + redirectUrl;
	}
}