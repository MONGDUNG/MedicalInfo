package com.global.ask;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.global.ask.QuestionDTO;
import com.global.ask.QuestionService;

@Controller
@RequestMapping("/question/")
public class QuestionController {
	
	
	@Autowired
	private QuestionService questionService;
	
	@GetMapping("list")
	public String list(Model model , 
			@RequestParam(value="page" , defaultValue="0") int page , //defaultValue는 파라미터가 안넘어왔을떄 보여줄 값. 0부터 시작하는 페이지이므로 첫페이지인 0을 보여줌
			@RequestParam(value="kw" , defaultValue="")String kw) {
		
		model.addAttribute("list" , questionService.questionPage(page,kw));
		model.addAttribute("count", questionService.totalCount());
		return "/ask/question_list";
	}
	
	@GetMapping("create")
	public String create(QuestionDTO questionDTO) {
		return "/ask/question_form";
	}
	
	@PostMapping("create")
	public String create(QuestionDTO dto , 
						@RequestParam("save") MultipartFile mf , 
						Principal principal){
		String username = principal.getName(); //로그인된 id를 꺼내줌
		//dto.setVisibility("PUBLIC"); // visibility를 기본적으로 "PUBLIC"으로 설정 (비공개일 경우 "PRIVATE"으로 설정 가능)
		questionService.questionCreate(dto, username, mf);
		return "redirect:/question/list";
	}
	
	@GetMapping("detail/{id}") //id자리만 다르게 하는 와일드카드 맵핑 이라고도 한다. 질문 상세보기
	public String detail(@PathVariable("id") Integer id , Model model ,  Principal principal) {
		QuestionDTO questionDTO = questionService.questionId(id); // 질문 정보 가져오기
				
		// 비공개 글에 대한 접근 제어
		if (questionDTO.getVisibility().equals("PRIVATE")) { 
			String username = "doctor";
			// 작성자와 관리자만 접근 가능하도록 설정
			if (!username.equals(questionDTO.getUsername()) && !isAdmin(principal)) {
				return "redirect:/"; // 접근 제한된 경우 홈으로 리디렉션
			}
		}
		
		model.addAttribute("questionDTO",questionDTO);
		
		return "/ask/question_detail";
	}
	
	@GetMapping("delete/{id}")
	public String delete(@PathVariable("id") Integer id) {
	    // 삭제할 질문 정보 가져오기
	    QuestionDTO questionDTO = questionService.questionId(id);
	    
	    // 조건 없이 바로 삭제
	    questionService.questionDelete(id); // 질문 삭제
	    
	    return "redirect:/"; // 홈으로 리디렉션
	}
	
	@GetMapping("modify/{id}")
	public String modify(@PathVariable("id") Integer id, Model model ) {
		
		model.addAttribute("questionDTO", questionService.questionId(id)); //questionDTO가 있으면 수정 없으면 글쓰기 DTO가 있는데 글을 써버리면 DTO가 비어있게 되기 때문
		
		return "/ask/question_form"; // 수정 폼으로 이동
	}
	
	@PostMapping("modify/{id}")
	public String modifyPro(@PathVariable("id") Integer id, 
							QuestionDTO dto , 
							@RequestParam("save")MultipartFile mf ,
							Model model) {
		questionService.questionModify(id, dto, mf); // 질문 수정
		return "redirect:/question/detail/"+id; // 수정된 질문 상세 보기로 리디렉션
	}
	
	// 관리자 권한을 확인하는 메소드 추가
	private boolean isAdmin(Principal principal) {
		// 관리자는 권한이 4인 사용자로 설정
		// 여기서는 간단히 예시로 true/false로 반환하지만 실제로는 서비스에서 권한 체크를 해야 합니다.
		// 예: return userService.getUserRole(principal.getName()).equals("ADMIN");
		return true; // 임시로 true를 반환
	}
}
