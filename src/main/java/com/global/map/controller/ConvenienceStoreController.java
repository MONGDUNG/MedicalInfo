package com.global.map.controller;

import com.global.map.dto.ConvenienceStoreDTO;
import com.global.map.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/conveniencestore")
public class ConvenienceStoreController {

    private final MapService mapService;

    @GetMapping("/list")
    public void printConvenienceStores() {
        List<ConvenienceStoreDTO> stores = mapService.getNearbyConvenienceStores();
        
        // 편의점 데이터 콘솔 출력
        stores.forEach(System.out::println);
    }
}