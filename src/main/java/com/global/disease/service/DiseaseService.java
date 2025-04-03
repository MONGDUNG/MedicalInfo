package com.global.disease.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.global.disease.entity.Search;
import com.global.disease.repository.SearchRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


@Service
public class DiseaseService {

	
    
    @Autowired
    private SearchRepository searchRepo;

	
	//페이징, 검색
	public Page<Search> diseasePage(Integer page, String keyword){
		List<Sort.Order>sorts = new ArrayList<>();
		sorts.add((Sort.Order.asc("id")));
		//괄호가 겹겹인 이유: 객체 생성을 먼저하고 메서드 호출을 하기 때문에
		
		Pageable pageable = PageRequest.of(page, 20, Sort.by(sorts)); // 한 페이지에 20개의 질병 내용을 보여줌
		
		Specification<Search> spec = search(keyword);
		Page<Search> pages = searchRepo.findAll(spec, pageable);
				
		return pages;
	}
	
	//전체 페이지
	public long totalCount(String keyword) {
		if(!keyword.trim().isEmpty()) {
			Specification<Search> spec = search(keyword);
			return searchRepo.count(spec);
		}
		return searchRepo.count();
	}
	
	//검색 처리
	private Specification<Search> search(String keyword){
		System.out.println("-----------------------------" + keyword);
		return (root, query,criteriaBuilder) -> criteriaBuilder.or( // Specification 은 메서드가 하나만 있어서 람다식으로 바로 표현이 가능하다.(메서드 구현부분 생략 가능)
			criteriaBuilder.like(root.get("diseaseName"), "%" + keyword + "%"),  //같은 클래스(criteriaBuilder)안의 다른 메서드를 호출하여 나온 결과 값을 매개변수로 사용 가능
			criteriaBuilder.like(root.get("symptom"), "%" + keyword + "%"),
			criteriaBuilder.like(root.get("related"), "%" + keyword + "%")
		);
	}
	
    
    public List<Search> picked(String name){
    	List<Search> pick = searchRepo.findByDiseaseNameStartingWith(name);
    	return pick.stream().distinct().collect(Collectors.toList());
    } 
    
    public List<Search> diagnosis(String bodyPart){
    	
    	List<Search> result = searchRepo.findByBodyPart(bodyPart);
    	
    	return result;
    }
    
    public List<Search> diagnosis(String bodyPart, List<String> symptom){
    	
    	String symptom1 = symptom.size() > 0 ? symptom.get(0) : "없다";
    	String symptom2 = symptom.size() > 1 ? symptom.get(1) : "없다";
    	String symptom3 = symptom.size() > 2 ? symptom.get(2) : "없다";
    	String symptom4 = symptom.size() > 3 ? symptom.get(3) : "없다";
    	String symptom5 = symptom.size() > 4 ? symptom.get(4) : "없다";
    	
    	List<Search> result = searchRepo.findByBodyPartContainingAndSymptom(bodyPart, symptom1, symptom2, symptom3, symptom4, symptom5);
    	return result;
    }
    
}
	