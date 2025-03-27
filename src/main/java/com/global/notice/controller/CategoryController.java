package com.global.notice.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.global.notice.dto.CategoryDTO;
import com.global.notice.service.CategoryService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private static final int BLOCK_SIZE = 10; // 페이지 그룹 크기 (10개씩)

    // 전체 데이터 정렬, 이름과 효능 모두 검색 가능
    @GetMapping("/all")
    public String getAllCategories(@RequestParam(value = "page", defaultValue = "1") int page,
                                   @RequestParam(value = "name", required = false) String name,
                                   @RequestParam(value = "indications", required = false) String indications,
                                   Model model) {
        int pageIndex = page - 1; // 0-based index 변환
        List<CategoryDTO> categoryDTOList = categoryService.searchCategories(name, indications, pageIndex);

        addPaginationDetails(page, model);
        model.addAttribute("list", categoryDTOList);
        model.addAttribute("searchTerm", name);
        model.addAttribute("indications", indications);  // indications 값도 모델에 추가

        return "category/category_list";
    }

    // 이름 및 효과 검색
    @GetMapping("/search")
    public String searchCategories(@RequestParam(value = "name") String name,
                                   @RequestParam(value = "page", defaultValue = "1") int page,
                                   @RequestParam(value = "indications", required = false) String indications,
                                   Model model) {
        return getAllCategories(page, name, indications, model);
    }

    // 효과 검색
    @GetMapping("/filter")
    public String filterByIndications(@RequestParam(value = "indications") String indications,
                                       @RequestParam(value = "page", defaultValue = "1") int page,
                                       Model model) {
        return getAllCategories(page, "", indications, model);
    }

    // 카테고리 상세 페이지 조회
    @GetMapping("/detail/{id}")
    public String getCategoryDetail(@PathVariable("id") Long id, Model model, Principal principal) {
        // 로그인한 사용자의 정보도 함께 모델에 추가
        model.addAttribute("principal", principal);
        
        // 카테고리 정보 가져오기
        CategoryDTO categoryDTO = categoryService.getCategoryDetail(id);
        model.addAttribute("categoryDTO", categoryDTO);

        return "category/category_detail";
    }

    // 답변 추가
    @PostMapping("/answer/create/{categoryId}")
    public String addAnswer(@PathVariable Long categoryId, @RequestParam String content) {
        categoryService.addAnswer(categoryId, content);
        return "redirect:/category/detail/" + categoryId;
    }

    // 페이지네이션 세부사항 추가
    private void addPaginationDetails(int currentPage, Model model) {
        int totalPages = categoryService.getTotalPages();
        int startPage = ((currentPage - 1) / BLOCK_SIZE) * BLOCK_SIZE + 1;
        int endPage = Math.min(startPage + BLOCK_SIZE - 1, totalPages);

        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
    }
}