package com.global.member;

import java.io.IOException;


import org.springframework.security.core.AuthenticationException;

import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
	
	
	@Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,         
                                        AuthenticationException exception) throws IOException, ServletException {
				
		 // 예외 메시지 가져오기 
		String errorMessage = "아이디 또는 비밀번호가 일치하지 않습니다.";
		System.out.println(exception.getClass()+"=====예외처리");
        // 특정 예외 메시지 처리
		 if (exception instanceof Exception) {
	            errorMessage = exception.getMessage(); // 예외 메시지를 그대로 가져오기
	        }
       


        // 메시지를 리다이렉트 URL에 포함
        request.getSession().setAttribute("errorMessage", errorMessage);
        response.sendRedirect("/member/login?error");
    }
}

