package com.global.map.controller;

import com.global.map.dto.PharmacyDTO;
import com.global.map.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pharmacy")
public class PharmacyController {

    private final MapService mapService;

    @GetMapping("/list")
    public void printPharmacy() {
        List<PharmacyDTO> pharmacies = mapService.getNearbyPharmacies();
        
        // 약국 데이터 콘솔 출력
        pharmacies.forEach(System.out::println);
    }
}
