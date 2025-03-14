package com.global.map.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.global.map.dto.MedInfoDTO;
import com.global.map.service.EmergencyService;
import com.global.map.service.MedInfoService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/map")
@RequiredArgsConstructor
public class HospitalController {
	
	private final EmergencyService emergencyService;
	private final MedInfoService medInfoService;
	
	@GetMapping("hospitaldetail")
	    public String hospitalDetail(@RequestParam("name") String name, 
	                                 @RequestParam("address") String address, 
	                                 @RequestParam("lat") String latitude, 
	                                 @RequestParam("lng") String longitude,
	                                 @RequestParam("phone") String phoneNumber,
	                                 Model model) {
	        model.addAttribute("hospitalName", name);
	        model.addAttribute("address", address);
	        model.addAttribute("latitude", latitude);
	        model.addAttribute("longitude", longitude);
	        model.addAttribute("phoneNumber", phoneNumber);
	        return "map/hospitalDetail";
	}
	 // 응급실 운영 시간 조회 API
    @GetMapping("emergency-hours")
    @ResponseBody
    public Map<String, String> getEmergencyHours(@RequestParam("name") String hospitalName) {
        return emergencyService.getEmergencyHours(hospitalName);
    }
    @GetMapping("/hospital-hours")
    @ResponseBody
    public MedInfoDTO getHospitalHours(@RequestParam("name") String hospitalName) {
        return medInfoService.getHospitalOperatingHours(hospitalName);
    }
}
