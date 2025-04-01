package com.global.map.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.global.map.dto.ItemDTO;
import com.global.map.dto.MedinstDTO;
import com.global.map.service.MapService;
import com.global.member.dto.MemberDTO;
import com.global.member.service.MemberService;
import com.global.redis.service.RedisService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/map")
@RequiredArgsConstructor
public class MapController {
	@Autowired
	private MemberService ms;
    private final MapService mapService;
    private final RedisService redisService;

    @GetMapping("main")
    public String mapMain(Model model, Principal principal) throws JsonProcessingException {
        if (principal != null) {
            model.addAttribute("dto", ms.readUser(principal.getName()));
            model.addAttribute("isLoggedIn", true);
        } else {
            model.addAttribute("isLoggedIn", false);
        } 
        return "map/kakaoMapTest";  // Thymeleaf 뷰 반환
    }    

    @GetMapping("nearbyHospitals")
    @ResponseBody
    public List<ItemDTO> getNearbyHospitals(@RequestParam("lat") double lat, @RequestParam("lng") double lng, @RequestParam("level") int level, @RequestParam("category") String category) {
        return mapService.getNearbyHospitals(lat, lng, level, category);
    }

    @GetMapping("medicalFacilityByDept")
    @ResponseBody
	public List<ItemDTO> getMedicalFacilityByDept(@RequestParam("lat") double lat, @RequestParam("lng") double lng, @RequestParam("level") int level,@RequestParam("category") String category, @RequestParam("dept") String dept) {
    	return mapService.getNearbyMedicalFacilitiesByDept(lat, lng, level, category, dept);
	}
    //모달 상세보기에서 별점, 리뷰수 가져오는 메소드
    @GetMapping("getReviewInfo")
    @ResponseBody
    public Map<String, Object> getReviewInfo(@RequestParam("hospitalName") String hospitalName, @RequestParam("address") String address) {
		String hospitalCode = mapService.findHCdByHNmAndAdr(hospitalName, address);
	    redisService.increaseClick(hospitalCode); // 누르는 시점에 클릭 수 증가
		Map<String, Object> map = new HashMap<>();
		
		map.put("avgRating", mapService.getAvgRating(hospitalCode));
		map.put("reviewCount", mapService.getReviewCount(hospitalCode));
		return map;                                                                                                                                                                                            
	}
    @GetMapping("/popular")
    @ResponseBody
    public List<MedinstDTO> getPopularHospitals(){
    	return redisService.getTopHospitals(5);
    }        
}
