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

    @GetMapping("main")

    public String mapMain(Model model) throws JsonProcessingException {

        return "map/kakaoMapTest";  // Thymeleaf 뷰 반환
    }     
    

    @GetMapping("nearbyHospitals")
    @ResponseBody
    public List<ItemDTO> getNearbyHospitals(@RequestParam("lat") double lat, @RequestParam("lng") double lng, @RequestParam("level") int level, @RequestParam("category") String category) {
        return mapService.getNearbyHospitals(lat, lng, level, category);
    }
    @GetMapping("nearbyPharmacies")
    @ResponseBody
    public List<ItemDTO> getNearbyPharmacies(@RequestParam("lat") double lat, @RequestParam("lng") double lng, @RequestParam("level") int level) {
    	return mapService.getNearbyPharmacies(lat, lng, level);
    	        	
    }
    @GetMapping("nearbyEmergencies")
    @ResponseBody
	public List<ItemDTO> getNearbyEmergencies(@RequestParam("lat") double lat, @RequestParam("lng") double lng, @RequestParam("level") int level) {
    	
		return mapService.getNearbyEmergencies(lat, lng, level);
	}

}
