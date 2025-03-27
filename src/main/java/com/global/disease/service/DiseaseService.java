package com.global.disease.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.global.disease.entity.Disease;
import com.global.disease.entity.Search;
import com.global.disease.repository.DiseaseRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import lombok.RequiredArgsConstructor;

@Service
public class DiseaseService {

	private final DiseaseRepository diseaseRepository;
	
    public DiseaseService(DiseaseRepository diseaseRepository) {
        this.diseaseRepository = diseaseRepository;
    }

	
	//페이징, 검색
	public Page<Search> diseasePage(Integer page, String keyword){
		List<Sort.Order>sorts = new ArrayList<>();
		sorts.add((Sort.Order.asc("id")));
		//괄호가 겹겹인 이유: 객체 생성을 먼저하고 메서드 호출을 하기 때문에
		
		Pageable pageable = PageRequest.of(page, 20, Sort.by(sorts)); //한 페이지에 20개의 질병 내용을 보여줌
		
		Specification<Search> spec = search(keyword);
		Page<Search> pages = diseaseRepository.findAll(spec, pageable);
				
		return pages;
	}
	
	//전체 페이지
	public long totalCount(String keyword) {
		if(!keyword.trim().isEmpty()) {
			Specification<Search> spec = search(keyword);
			return diseaseRepository.count(spec);
		}
		return diseaseRepository.count();
	}
	
	//검색 처리
	private Specification<Search> search(String keyword){
		System.out.println("-----------------------------" + keyword);
		return (root, query,criteriaBuilder) -> criteriaBuilder.or( //Specification은 메서드가 하나만 있어서 람다식으로 바로 표현이 가능하다.(메서드 구현부분 생략 가능)
			criteriaBuilder.like(root.get("diseaseName"), "%" + keyword + "%"),  //같은 클래스(criteriaBuilder)안의 다른 메서드를 호출하여 나온 결과 값을 매개변수로 사용 가능
			criteriaBuilder.like(root.get("symptom"), "%" + keyword + "%"),
			criteriaBuilder.like(root.get("related"), "%" + keyword + "%")
		);
	}
	

    public List<Disease> recommendDiseases(Long bodyPartId, List<Long> symptomIds) {
        return diseaseRepository.findDiseasesByBodyPartAndSymptoms(bodyPartId, symptomIds);
    }
	
}
	
