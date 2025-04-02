package com.global.member;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.global.member.entity.ClassificationEntity;
import com.global.member.entity.MemberEntity;
import com.global.member.entity.MemberTierEntity;
import com.global.member.repository.ClassificationRepository;
import com.global.member.repository.MemberRepository;
import com.global.member.repository.MemberTierRepository;

import lombok.RequiredArgsConstructor;

//로그인 및 아이디 체크를 수행하는 클래스
@RequiredArgsConstructor // Lombok 어노테이션: 생성자를 자동으로 생성
@Service // 스프링 서비스 클래스 어노테이션
public class MemberSecurityService implements UserDetailsService {

 // MemberRepository 주입 (생성자 주입 방식)
 private final MemberRepository memberRepository;
 @Autowired
 MemberTierRepository mt;
 @Autowired
 ClassificationRepository cf;
 @Autowired
 private PasswordEncoder passwordEncoder;
 
 
 
 @Override
 // 유저 아이디로 유저 정보를 로드하는 메서드
 public UserDetails loadUserByUsername(String username) {
     // 유저 아이디로 유저를 찾음
	 
	 Optional<MemberEntity> op = memberRepository.findByUsername(username); // 유저 체크 
	 
	 if (username.contains("&")) {	 //  & 가 있을시 api 로그인       	    		 
		 String email = username.split("&")[0];
		 	// test@gmail.com&kakao  
		 String api = username.split("&")[1];
	 		  
		 int si = 2; //카카오 로그인시 
		 String na = "카카오 사용자";
		 	if(op.isEmpty()) {  		 		 
		 		if(api.equals("naver")) {
		 			si = 3; // 네이버로그인시
		 			na = "네이버 사용자";
		 		}else if(api.equals("google")){
		 			si = 4;
		 			na = "구글 사용자";
		 		}
    		 MemberEntity me = MemberEntity.builder()
    			 			.username(username)
    			 			.signuppath(si) 
    			 			.memberstatus("general")
    			 			.email(email)
    			 			.password(passwordEncoder.encode("2"))
    			 			.name(na)
    			 			.sex("man")   
    			 			.underlyingcondition("없음")
    			 			.build();
    			 			memberRepository.save(me);
    			 			
    			 			Optional<MemberTierEntity> mteOptional = mt.findByTier(me.getMemberstatus());		
    					    MemberTierEntity mte = mteOptional.get();
    					    
    					    ClassificationEntity cfe = ClassificationEntity.builder() 
    					    		.username(me.getUsername())
    					            .memberId(me)
    					            .tierId(mte) 
    					            .build();       
    					    cfe = cf.save(cfe);
    					
    					
    					    me = memberRepository.findByUsername(me.getUsername()).get();
    					    me.setTierId(mte);
    				    	me.setClassiId(cfe);	    
    				    	memberRepository.save(me);
    			 			
    	 
		 	}
	 }
     
     if (username == null || username.isEmpty()) {
    	    throw new BadCredentialsException("카카오 로그인에서 유효하지 않은 username이 전달되었습니다.");
    	}
    	System.out.println("카카오에서 전달된 username: " + username);

     
     
     // 유저가 존재하지 않으면 예외 발생
     op = memberRepository.findByUsername(username);
     if (op.isEmpty()) {
         throw new BadCredentialsException("사용자를 찾을 수 없습니다.");         
     }
          
     MemberEntity se = op.get();
     if(("withdrawn".equals(se.getMemberstatus()))){     	 
    	throw new BadCredentialsException("탈퇴된 회원 입니다"); // 에러처리
     }else if(("dormant".equals(se.getMemberstatus()))){     	 
     	throw new BadCredentialsException("휴면 회원 입니다"); // 에러처리	
     }else {
    	if("sanctioned".equals(se.getMemberstatus())) {	 
    	   throw new BadCredentialsException("정지된 회원 입니다");
    	}
     }    
     List<GrantedAuthority> auth = new ArrayList<>();
     String s = se.getMemberstatus();
     // 아이디가 "admin"이면 관리자 권한 부여
     if (s.equals("admin")) { // 관리자 일시 (권한 테이블 따로 "관리자"일시 원 투 매니)
         auth.add(new SimpleGrantedAuthority(UserRole.ADMIN.getValue())); 
         auth.add(new SimpleGrantedAuthority(UserRole.DOCTOR.getValue())); //의사
         auth.add(new SimpleGrantedAuthority(UserRole.PM.getValue()));  // 약사
         auth.add(new SimpleGrantedAuthority(UserRole.USER.getValue()));  
     } else if("Pharmacist" == s) { 
    	 auth.add(new SimpleGrantedAuthority(UserRole.PM.getValue()));
         auth.add(new SimpleGrantedAuthority(UserRole.USER.getValue())); 
     } else if ("Doctor" == s) {
    	 auth.add(new SimpleGrantedAuthority(UserRole.DOCTOR.getValue()));
     	 auth.add(new SimpleGrantedAuthority(UserRole.USER.getValue()));
     } else {
    	 auth.add(new SimpleGrantedAuthority(UserRole.USER.getValue()));
     }
       return new User(se.getUsername(), se.getPassword(), auth);
 }
}


