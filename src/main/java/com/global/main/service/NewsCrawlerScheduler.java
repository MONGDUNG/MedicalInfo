package com.global.main.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NewsCrawlerScheduler {
	
	@Scheduled(cron = "0 0 12 * * *") // 3분마다 실행
	public void runPythonCrawler() {
	    try {
	        System.out.println("✅ 보건복지부 뉴스 크롤링 시작...");

	        // .py 실행 (경로 주의)
	        Process process = Runtime.getRuntime().exec("python C:/Users/14A/News_crawler.py");

	        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
	        String line;
	        while ((line = reader.readLine()) != null) {
	            System.out.println("[크롤링] " + line);
	        }

	        int exitCode = process.waitFor();
	        System.out.println("✅ 크롤링 완료 (종료코드: " + exitCode + ")");

	    } catch (Exception e) {
	        System.err.println("❌ 크롤링 중 에러 발생:");
	        e.printStackTrace();
	    }
    }
}
