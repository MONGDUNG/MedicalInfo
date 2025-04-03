package com.global.notice.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.global.notice.dto.SupplementDTO;
import com.global.notice.service.SupplementService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/supplement")
@RequiredArgsConstructor
public class SupplementController {

    private final SupplementService supplementService;
    private static final int BLOCK_SIZE = 10; // 페이지 그룹 크기 (10개씩)

    // 전체 데이터 정렬, 이름과 효능 모두 검색 가능
    @GetMapping("/all")
    public String getAllSupplements(@RequestParam(value = "page", defaultValue = "1") int page,
                                    @RequestParam(value = "name", required = false) String name,
                                    @RequestParam(value = "primaryFnclty", required = false) String primaryFnclty,
                                    Model model) {
        int pageIndex = page - 1; // 0-based index 변환
        List<SupplementDTO> supplementDTOList = supplementService.searchSupplements(name, primaryFnclty, pageIndex);

        addPaginationDetails(page, model);
        model.addAttribute("list", supplementDTOList);
        model.addAttribute("searchTerm", name);
        model.addAttribute("primaryFnclty", primaryFnclty);  // indications 값도 모델에 추가

        return "supplement/supplement_list";
    }

    // 이름 및 효과 검색
    @GetMapping("/search")
    public String searchSupplements(@RequestParam(value = "name") String name,
                                    @RequestParam(value = "page", defaultValue = "1") int page,
                                    @RequestParam(value = "primaryFnclty", required = false) String primaryFnclty,
                                    Model model) {
        return getAllSupplements(page, name, primaryFnclty, model);
    }

    // 효과 검색
    @GetMapping("/filter")
    public String filterByPrimaryFnclty(@RequestParam(value = "primaryFnclty") String primaryFnclty,
                                        @RequestParam(value = "page", defaultValue = "1") int page,
                                        Model model) {
        return getAllSupplements(page, "", primaryFnclty, model);
    }

    // 카테고리 상세 페이지 조회
    @GetMapping("/detail/{id}")
    public String getSupplementDetail(@PathVariable("id") Long id, Model model, Principal principal) {
        // 로그인한 사용자의 정보도 함께 모델에 추가
        model.addAttribute("principal", principal);

        // 카테고리 정보 가져오기
        SupplementDTO supplementDTO = supplementService.getSupplementDetail(id);
        model.addAttribute("supplementDTO", supplementDTO);

        return "supplement/supplement_detail";
    }

    // 답변 추가
    @PostMapping("/answer/create/{supplementId}")
    public String addAnswer(@PathVariable Long supplementId, @RequestParam String content) {
        supplementService.addAnswer(supplementId, content);
        return "redirect:/supplement/detail/" + supplementId;
    }

    // 페이지네이션 세부사항 추가
    private void addPaginationDetails(int currentPage, Model model) {
        int totalPages = supplementService.getTotalPages();
        int startPage = ((currentPage - 1) / BLOCK_SIZE) * BLOCK_SIZE + 1;
        int endPage = Math.min(startPage + BLOCK_SIZE - 1, totalPages);

        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
    }

    // 좋아요 추가 (로그인한 사용자만 가능)
    @GetMapping("vote/{id}")
    @PreAuthorize("isAuthenticated()")
    public String supplementVote(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirectAttributes) {
        String username = principal.getName();
        supplementService.voter(id, username);
        redirectAttributes.addFlashAttribute("message", "투표가 완료되었습니다!");
        return "redirect:/supplement/detail/" + id;
    }
}