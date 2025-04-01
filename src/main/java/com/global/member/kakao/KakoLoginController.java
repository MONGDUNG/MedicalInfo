package com.global.member.kakao;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class KakoLoginController {
	
	private final KakaoService kakaoService;

	 @GetMapping("/login/kakao/callback") //카카오 계정에서 정보가져오기
	    public String callback(@RequestParam("code") String code , Model model) throws IOException {	     
		 KakaoTokenResponseDTO tokenDTO = kakaoService.getAccessTokenFromKakao(code);
		 
		 // email 가져오기 , 패스워드 2 번 강제 입력
		 model.addAttribute("username",kakaoService.getUserInfo(tokenDTO.getAccessToken()).getKakaoAccount().getEmail()+"&kakao");
		 model.addAttribute("password" , 2);
		    return "/member/kakao/go";
	    }
}
