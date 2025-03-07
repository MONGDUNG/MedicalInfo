package com.global.map.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/map")
@RequiredArgsConstructor
public class HospitalController {
	
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
}
