package com.global.member.naver;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/naver")
public class NaverController {
	
	private final NaverService naverService;

    @GetMapping("/callback")
    public String callback(HttpServletRequest request , Model model) throws Exception {
        NaverDTO naverInfo = naverService.getNaverInfo(request.getParameter("code"));
        model.addAttribute("username", naverInfo.getEmail()+"&naver"); //가입경로 구별을 위해 _naver 추가
        model.addAttribute("password", "2");
        return "/member/kakao/go";
    }

}
