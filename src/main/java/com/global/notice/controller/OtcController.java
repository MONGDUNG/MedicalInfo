package com.global.notice.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.global.notice.dto.OtcDTO;
import com.global.notice.entity.OtcEntity;
import com.global.notice.service.OtcService;

@Controller
public class OtcController {

    private final OtcService otcService;

    public OtcController(OtcService otcService) {
        this.otcService = otcService;
    }

    @GetMapping("/otc/list")
    public String getOtcList(Model model) {
        List<OtcEntity> otc = otcService.getAllOtc();

        // 원하는 약 종류 순서 정의
        List<String> typeOrder = List.of("해열진통제", "감기약", "소화제", "파스");

        // 약 종류별 그룹화
        Map<String, List<OtcEntity>> otcByType = otc.stream()
                .collect(Collectors.groupingBy(OtcEntity::getMedicineType));

        // LinkedHashMap을 사용하여 순서 보장
        Map<String, List<OtcEntity>> sortedOtcByType = new LinkedHashMap<>();
        for (String type : typeOrder) {
            if (otcByType.containsKey(type)) {
                sortedOtcByType.put(type, otcByType.get(type));
            }
        }

        model.addAttribute("otcByType", sortedOtcByType);
        return "otc/otc_list";
    }

 // 카테고리 상세 페이지 조회
    @GetMapping("/otc/detail/{id}")
    public String getOtcDetail(@PathVariable("id") Integer id, Model model) {
        
        // 카테고리 정보 가져오기
        OtcDTO otc = otcService.getOtcDetail(id);
        model.addAttribute("otc", otc);

        return "otc/otc_detail";
    }
}