package com.global.map.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.global.map.dto.MedinstDTO;
import com.global.map.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;
    private final ObjectMapper objectMapper;

    @GetMapping("kakaoMap")
    public String kakaoMap(Model model) throws JsonProcessingException {
        List<MedinstDTO> hospitals = mapService.getNearbyHospitals();
        System.out.println(hospitals.size());
        String hospitalsJson = objectMapper.writeValueAsString(hospitals);  // JSON 직렬화
        model.addAttribute("hospitals", hospitalsJson);
        return "map/kakaoMapTest";  // Thymeleaf 뷰 반환
    }
}
