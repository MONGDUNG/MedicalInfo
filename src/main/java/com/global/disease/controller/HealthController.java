package com.global.disease.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;

import com.global.disease.entity.Search;
import com.global.disease.entity.SymptomsEntity;
import com.global.disease.repository.BodyPartRepository;
import com.global.disease.repository.SymptomsRepository;
import com.global.disease.service.DiseaseService;

@Controller
@RequestMapping("/diagnosis/")
@RequiredArgsConstructor
public class HealthController {

    private final BodyPartRepository bodyPartRepo;
    private final SymptomsRepository symptomsRepo;
    private final DiseaseService diseaseService;


    // 1단계: 부위 선택
    @GetMapping("body-parts")
    public String showBodyParts(Model model) {
        model.addAttribute("bodyParts", bodyPartRepo.findAll());
        return "health/select-body-part";
    }

    // 2단계: 증상 선택
    @GetMapping("symptoms")
    public String showSymptoms(@RequestParam("bodyPart") String bodyPart, Model model) {
        List<SymptomsEntity> symptoms= symptomsRepo.findByBodyPart(bodyPart);
        model.addAttribute("symptoms", symptoms);
        model.addAttribute("bodyPart", bodyPart);
        return "health/select-symptoms";
    }

    // 3단계: 질병 추천 결과
    @GetMapping("diseases")
    public String showDiseases(@RequestParam("bodyPart") String bodyPart,
                                @RequestParam(value = "symptom", required = false) List<String> symptom,
                                Model model) {
    	// symptom 이 null 이면 빈 리스트로 초기화
    	if(symptom == null) {
    		symptom = new ArrayList<>();
    		List<Search> diseases = diseaseService.diagnosis(bodyPart);
    		model.addAttribute("diseases", diseases);
    		return "health/disease-results";
    	}
        List<Search> diseases = diseaseService.diagnosis(bodyPart, symptom);
        model.addAttribute("diseases", diseases);
                
        return "health/disease-results";
    }   
}

