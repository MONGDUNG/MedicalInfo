package com.global.notice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;

import com.global.notice.dto.AnswerDTO;
import com.global.notice.dto.CategoryDTO;
import com.global.notice.entity.AnswerEntity;
import com.global.notice.entity.CategoryEntity;
import com.global.notice.repository.AnswerRepository;
import com.global.notice.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AnswerRepository answerRepository;

    private static final int PAGE_SIZE = 15;  // 한 페이지에 표시할 항목 수
    private static final int BLOCK_SIZE = 10; // 페이지 그룹 (10개씩 보여주기)

    // 전체 데이터 조회 (페이지네이션 적용)
    public List<CategoryDTO> getAllCategories(int page) {
        Page<CategoryEntity> categories = categoryRepository.findAll(PageRequest.of(page, PAGE_SIZE));
        return categories.stream()
                .map(this::categoryToDTO)
                .collect(Collectors.toList());
    }

    // 이름과 효능 검색
    public List<CategoryDTO> searchCategories(String name, String indications, int page) {
        Specification<CategoryEntity> spec = searchByNameAndIndications(name, indications);
        Page<CategoryEntity> categories = categoryRepository.findAll(spec, PageRequest.of(page, PAGE_SIZE));
        return categories.stream()
                .map(this::categoryToDTO)
                .collect(Collectors.toList());
    }

    // 전체 페이지 수 (전체 페이지 그룹을 계산하여 반환)
    public int getTotalPages() {
        long totalCount = categoryRepository.count();
        return (int) Math.ceil((double) totalCount / PAGE_SIZE);
    }

    // 페이지 그룹 계산
    public Map<String, Integer> getPaginationDetails(int currentPage) {
        int totalPages = getTotalPages();
        int startPage = (currentPage / BLOCK_SIZE) * BLOCK_SIZE + 1;
        int endPage = Math.min(startPage + BLOCK_SIZE - 1, totalPages);

        Map<String, Integer> paginationDetails = new HashMap<>();
        paginationDetails.put("startPage", startPage);
        paginationDetails.put("endPage", endPage);
        paginationDetails.put("totalPages", totalPages);

        return paginationDetails;
    }

    // 검색 조건: 이름과 효능으로 검색
    private Specification<CategoryEntity> searchByNameAndIndications(String name, String indications) {
        return (root, query, criteriaBuilder) -> {
            if (name != null && indications != null) {
                return criteriaBuilder.and(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%" + name.toLowerCase() + "%"),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("indications")), "%" + indications.toLowerCase() + "%")
                );
            } else if (name != null) {
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%" + name.toLowerCase() + "%");
            } else if (indications != null) {
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("indications")), "%" + indications.toLowerCase() + "%");
            }
            return criteriaBuilder.conjunction(); // 아무 조건 없이 모든 결과를 반환
        };
    }

    // CategoryEntity -> CategoryDTO 변환
    private CategoryDTO categoryToDTO(CategoryEntity category) {
        List<AnswerDTO> answerDTOList = category.getAnswerList().stream()
                .map(this::answerToAnswerDTO)
                .collect(Collectors.toList());

        return CategoryDTO.builder()
                .id(category.getId())
                .productName(category.getProductName())
                .companyName(category.getCompanyName())
                .activeIngredient(category.getActiveIngredient())
                .indications(category.getIndications())
                .answerList(answerDTOList)
                .build();
    }

    // 카테고리 상세 페이지 조회
    public CategoryDTO getCategoryDetail(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return categoryToDTO(category);
    }

    // 답변 추가
    public void addAnswer(Long categoryId, String content) {
        CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        AnswerEntity answer = new AnswerEntity();
        answer.setContent(content);
        answer.setCategory(category); // 댓글이 어떤 카테고리에 속하는지 설정
        answer.setCreateDate(LocalDateTime.now());
        
        answerRepository.save(answer);
    }

    // AnswerEntity -> AnswerDTO 변환
    private AnswerDTO answerToAnswerDTO(AnswerEntity answer) {
        return AnswerDTO.builder()
                .id(answer.getId())
                .content(answer.getContent())
                .createDate(answer.getCreateDate())
                .modifyDate(answer.getModifyDate())
                .memberDTO(
                    answer.getMember() != null ? 
                    com.global.member.dto.MemberDTO.builder()
                        .username(answer.getMember().getUsername())
                        .build() 
                    : null
                )
                .build();
    }
    
}