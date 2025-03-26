package com.global.disease.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.global.disease.entity.BodyPartSymptom;
import com.global.disease.entity.Symptom;
import com.global.disease.repository.BodyPartSymptomRepository;

@Service
public class SymptomService {

    private final BodyPartSymptomRepository bodyPartSymptomRepository;

    public SymptomService(BodyPartSymptomRepository bodyPartSymptomRepository) {
        this.bodyPartSymptomRepository = bodyPartSymptomRepository;
    }

    public List<Symptom> getSymptomsByBodyPart(Long bodyPartId) {
        List<BodyPartSymptom> relations = bodyPartSymptomRepository.findByBodyPart_Id(bodyPartId);
        return relations.stream()
                        .map(BodyPartSymptom::getSymptom)
                        .collect(Collectors.toList());
    }
}