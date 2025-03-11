package com.global.map.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.global.map.dto.ItemDTO;
import com.global.map.dto.MedinstDTO;
import com.global.map.dto.PharmacyDTO;
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

    public String mapMain(Model model) throws JsonProcessingException {
        List<ItemDTO> hospitals = mapService.getNearbyHospitals();

        String hospitalsJson = objectMapper.writeValueAsString(hospitals);  // JSON 직렬화
        model.addAttribute("hospitals", hospitalsJson);
        System.out.println(hospitals.size());
        return "map/kakaoMapTest";  // Thymeleaf 뷰 반환
    }     
    
    @GetMapping("nearbyHospitals")
    @ResponseBody

	public List<ItemDTO> getNearbyHospitals() {
		return mapService.getNearbyHospitals();


	}
    @GetMapping("nearbyPharmacies")
    @ResponseBody
    public List<ItemDTO> getNearbyPharmacies() {
    	return mapService.getNearbyPharmacies();
    	        	
    }

}
