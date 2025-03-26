package com.global.disease.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.global.disease.entity.BodyPartSymptom;
import com.global.disease.entity.Disease;
import com.global.disease.entity.Symptom;

import com.global.disease.repository.BodyPartSymptomRepository;
import com.global.disease.service.DiseaseService;
import com.global.disease.repository.BodyPartRepository;

@Controller
@RequestMapping("/diagnosis/")
public class HealthController {

    private final BodyPartRepository bodyPartRepo;
    private final BodyPartSymptomRepository bodyPartSymptomRepo;
    private final DiseaseService diseaseService;

    public HealthController(BodyPartRepository bodyPartRepo,
                            BodyPartSymptomRepository bodyPartSymptomRepo,
                            DiseaseService diseaseService) {
        this.bodyPartRepo = bodyPartRepo;
        this.bodyPartSymptomRepo = bodyPartSymptomRepo;
        this.diseaseService = diseaseService;
    }

    // 1단계: 부위 선택
    @GetMapping("body-parts")
    public String showBodyParts(Model model) {
        model.addAttribute("bodyParts", bodyPartRepo.findAll());
        return "health/select-body-part";
    }

    // 2단계: 증상 선택
    @GetMapping("symptoms")
    public String showSymptoms(@RequestParam("bodyPartId") Long bodyPartId, Model model) {
        List<BodyPartSymptom> relations = bodyPartSymptomRepo.findByBodyPart_Id(bodyPartId);
        List<Symptom> symptoms = relations.stream().map(BodyPartSymptom::getSymptom).toList();

        model.addAttribute("symptoms", symptoms);
        model.addAttribute("bodyPartId", bodyPartId);
        return "health/select-symptoms";
    }

    // 3단계: 질병 추천 결과
    @GetMapping("diseases")
    public String showDiseases(@RequestParam("bodyPartId") Long bodyPartId,
                                @RequestParam("symptomIds") List<Long> symptomIds,
                                Model model) {
        List<Disease> diseases = diseaseService.recommendDiseases(bodyPartId, symptomIds);
        model.addAttribute("diseases", diseases);
        return "health/disease-results";
    }
}

