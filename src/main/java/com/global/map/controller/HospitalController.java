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
import com.global.map.dto.MedinstDTO;
import com.global.map.service.EmergencyService;
import com.global.map.service.MapService;
import com.global.map.service.MedInfoService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/map")
@RequiredArgsConstructor
public class HospitalController {
	
	private final EmergencyService emergencyService;
	private final MedInfoService medInfoService;
	private final MapService mapService;
	@GetMapping("hospitaldetail")
	public String hospitalDetail(@RequestParam("name") String name, 
	                             @RequestParam("address") String address,
	                             @RequestParam("phone") String phone,
	                             @RequestParam("lat") String latitude, 
                                 @RequestParam("lng") String longitude,
	                             @RequestParam("category") String category,
	                             Model model) {
	    model.addAttribute("name", name);
	    model.addAttribute("address", address);
	    model.addAttribute("phone", phone);
	    model.addAttribute("category", category);
	    model.addAttribute("latitude", latitude);
        model.addAttribute("longitude", longitude);

	    if (!category.equals("응급실") && !category.equals("약국")) {
	    	System.out.println("category : " + category);
	    	System.out.println("address : " + address);
	    	System.out.println("name : " + name);
	        String hospitalCode = mapService.findHCdByHNmAndAdr(name, address);
	        MedinstDTO hospitalInfo = mapService.getHospitalInfo(hospitalCode);
	        model.addAttribute("hospitalInfo", hospitalInfo);
	        
	    }
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
    public MedInfoDTO getHospitalHours(@RequestParam("name") String hospitalName, @RequestParam("address") String address) {
    	String hospitalCode = mapService.findHCdByHNmAndAdr(hospitalName, address);
    	System.out.println("hospitalCode : " + hospitalCode);
        return medInfoService.getHospitalOperatingHours(hospitalCode);
    }
}
