package com.global.member;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpSession;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    @Autowired
    private HttpSession session; //인증번호 저장할 객체

   

    public String generateRandomCode() { // 랜덤코드 생성
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999 범위
        return String.valueOf(code);
    }

    // 이메일로 인증 코드 전송 & 세션에 저장
    public void sendEmailWithCode(String to) {
        String randomCode = generateRandomCode();
        
        // 세션에 저장
        session.setAttribute(to, randomCode);
        
        // 이메일 전송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("이메일 인증 코드");
        message.setText("인증 코드: " + randomCode);
        message.setFrom("ehdrb438438@naver.com");

        mailSender.send(message);
    }
    
    // 인증 코드 검증
    public boolean verifyCode(String to, String userCode) {
        String storedCode = (String) session.getAttribute(to); // 세션에서 코드 가져오기
        System.out.println(storedCode);
        System.out.println(userCode);
        return storedCode != null && storedCode.equals(userCode);        
    }
    
    
    // 이메일 전송 로직
    /*
    private void sendMail(String email, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(text, true);
            helper.setFrom("ehdrb438438@naver.com"); // 네이버 계정과 동일해야 함

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 전송 실패", e);
        }
    }
   */
}
