package com.global.map.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.global.map.dto.MedinstDTO;
import com.global.map.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;
    private final ObjectMapper objectMapper;

    @GetMapping("main")
    public String mapMain(@RequestParam(value = "lat", required = false, defaultValue = "37.5665") double lat,
                          @RequestParam(value = "lng", required = false, defaultValue = "126.9780") double lng,
                          Model model) throws JsonProcessingException {
        List<MedinstDTO> hospitals = mapService.getNearbyHospitals(lat, lng);
        String hospitalsJson = objectMapper.writeValueAsString(hospitals);  // JSON 직렬화
        model.addAttribute("hospitals", hospitalsJson);
        return "map/kakaoMapTest";  // Thymeleaf 뷰 반환
    }     
    
    @GetMapping("nearbyHospitals")
    @ResponseBody
	public List<MedinstDTO> getNearbyHospitals(@PathVariable double lat, @PathVariable double lng) {
		return mapService.getNearbyHospitals(lat, lng);
	}

}
