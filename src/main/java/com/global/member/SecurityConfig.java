package com.global.member;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity // 모든 URL요청이 시큐리티의 제어를 받도록 설정
@Configuration  // 환경설정 클래스
public class SecurityConfig {
	
	private final CustomAuthenticationFailureHandler failureHandler;

    @Autowired
    public SecurityConfig(CustomAuthenticationFailureHandler failureHandler) {
        this.failureHandler = failureHandler;
    }
	
	@Bean // 싱글톤(객체 생성을 단 한 번만 하고 계속 재사용함. 생성자의 접근 제한자를 private으로 걸은 경우) 객체로 생성.
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception { 
	    // (매개변수=들어오려는 손님) 예외 처리를 안 하고 싶을 시 throws 
	    System.out.println("----------security filterchain-----------"); // 시큐리티 작동을 콘솔에 확인.
	    http.authorizeHttpRequests( // URL 권한 확인(권한 없을 시 403 에러)
	        (ah) -> ah.requestMatchers( // 요청 URL 패턴에 대한 규칙(보안 규칙 설정)
	            new AntPathRequestMatcher("/member/admin")).hasRole("ADMIN") // 관리자페이지 어드민만
	            .requestMatchers(new AntPathRequestMatcher("/**")).permitAll()   // URL경로 설정 /**모든(URL)권한
	           
	    )
	    .csrf((csrf) -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2/**"), 
	    		new AntPathRequestMatcher("/member/**"))) 
	        // 해당 경로(h2) 제외 설정. ( ?/ 파라미터 관련 보안 설정)
	    .headers((header) -> header.addHeaderWriter(new XFrameOptionsHeaderWriter(
	        XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN))) // H2 콘솔 페이지 출력 셋팅
	    .formLogin((fl) -> fl.loginPage("/member/login")
	    					 .failureHandler(failureHandler) // 로그인실패시 핸들러
	    					 .defaultSuccessUrl("/member/time")) 
	    				
	    
	        // 로그인 설정(로그인 시 어디로 가는지) 로그인 시 최종 로그인 기록을 위해 time으로 이동 
	    .logout((logout) -> logout
	        .logoutRequestMatcher(new AntPathRequestMatcher("/member/logout"))
	        .logoutSuccessUrl("/member/login")
	        .invalidateHttpSession(true)); // 로그아웃 시 세션 삭제

	    /* .requestMatchers(new AntPathRequestMatcher("/admin/**")).hasRole("ADMIN") // admin 페이지 권한을 가진 사용자만, 임의의 아이디
	       .requestMatchers(new AntPathRequestMatcher("/user/**")).authenticated() // user 페이지 인증된(로그인) 사용자만
	       .requestMatchers("/main/**").permitAll() // main 주소 모든 사용자 접근 허용
	       .anyRequest().denyAll() // 3개의 URL 제외 나머지 모두 차단 */ 
	    
	    return http.build();
	}

	@Bean // 비밀번호 형식 변경
	PasswordEncoder passwordEncoder() {
	    return new BCryptPasswordEncoder();
	}

	// 인증(로그인), 권한
	// AuthenticationManager - 인증을 처리하는 인터페이스 
	// 전달받은 username, password 유효한지 확인
	// 성공되면 사용자 정보가 저장된 AuthenticationManager 객체를 리턴
	// UserDetailService, PasswordEncoder
	AuthenticationManager authenticationManager(AuthenticationConfiguration ac) throws Exception {            
	    return ac.getAuthenticationManager();
	}
}