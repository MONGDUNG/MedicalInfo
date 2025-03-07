package com.global.map.controller;

import com.global.map.dto.EmergencyDTO;
import com.global.map.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/emergency")
public class EmergencyController {

    private final MapService mapService;

    @GetMapping("/list")
    public void printEmergency() {
        List<EmergencyDTO> Emergencys = mapService.getNearbyEmergencys();
        
        // 응급실데이터 콘솔 출력
        Emergencys.forEach(System.out::println);
    }
}

