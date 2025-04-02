package com.global.redis.service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import com.global.map.dto.MedinstDTO;
import com.global.map.entity.MedinstEntity;
import com.global.map.repository.MedinstRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisService {
	
	private final RedisTemplate<String, Object> redisTemplate; 
    private final RedisTemplate<String, List<MedinstDTO>> dtoRedisTemplate;
    private final MedinstRepository medinstRepository;
	
	public void increaseClick (String hospitalCode) {
		if (hospitalCode == null || hospitalCode.isBlank()) return;
	    redisTemplate.opsForZSet().incrementScore("hospital:clicks", hospitalCode, 1);
	}
	
	public Map<String, Integer> getAllClickCounts() {
	    Set<ZSetOperations.TypedTuple<Object>> data = redisTemplate.opsForZSet()
	        .reverseRangeWithScores("hospital:clicks", 0, -1);

	    Map<String, Integer> result = new LinkedHashMap<>();
	    if (data != null) {
	        for (ZSetOperations.TypedTuple<Object> entry : data) {
	            String hospitalCode = entry.getValue().toString();
	            int score = entry.getScore().intValue();
	            result.put(hospitalCode, score);
	        }
	    }
	    return result;
	}
	
	public void cacheTopHospitals(List<MedinstDTO> topList) {
		dtoRedisTemplate.opsForValue().set("hospital:top-clicked", topList, Duration.ofHours(1));
	}
    public List<MedinstDTO> getTopHospitals(int limit) {
        Set<Object> codeSet = redisTemplate.opsForZSet().reverseRange("hospital:clicks", 0, limit - 1);
        if (codeSet == null || codeSet.isEmpty()) return List.of();

        List<String> codeList = codeSet.stream()
            .map(Object::toString)
            .collect(Collectors.toList());

        List<MedinstEntity> entities = medinstRepository.findByHospitalCodeIn(codeList);

        return entities.stream()
            .map(MedinstDTO::new)
            .collect(Collectors.toList());
    }
}
