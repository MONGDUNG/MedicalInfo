package com.global.map.service;

import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.global.map.dto.MedinstDTO;
import com.global.redis.service.RedisService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PopularHospitalScheduler {
	
	private final RedisService redisService;
	private final MapService mapService;
	
	@Scheduled(fixedRate = 1000 * 60 * 60)
	public void updateTopHospitals() {
		Map<String, Integer> counts = redisService.getAllClickCounts();
		
		List<String> topCodes = counts.entrySet().stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				.limit(10)
				.map(Map.Entry::getKey)
	            .toList();
		
		List<MedinstDTO> topHospitals = topCodes.stream()
				.map(mapService::getHospitalInfo)
				.toList();
		
		redisService.cacheTopHospitals(topHospitals);
	}
}
