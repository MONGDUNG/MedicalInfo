package com.global.notice.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;

import com.global.member.entity.MemberEntity;
import com.global.member.repository.MemberRepository;
import com.global.notice.dto.AnswerDTO;
import com.global.notice.dto.SupplementDTO;
import com.global.notice.entity.AnswerEntity;
import com.global.notice.entity.SupplementEntity;
import com.global.notice.repository.AnswerRepository;
import com.global.notice.repository.SupplementRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplementService {

    private final SupplementRepository supplementRepository;
    private final AnswerRepository answerRepository;
    private final MemberRepository memberRepository;
    
    private static final int PAGE_SIZE = 15;  // 한 페이지에 표시할 항목 수
    private static final int BLOCK_SIZE = 10; // 페이지 그룹 (10개씩 보여주기)

    // 전체 데이터 조회 (페이지네이션 적용)
    public List<SupplementDTO> getAllSupplements(int page) {
        // 'voter' 필드를 기준으로 내림차순 정렬
        Page<SupplementEntity> supplements = supplementRepository.findAll(PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Order.desc("voter"))));
        return supplements.stream()
                .map(this::supplementToDTO)
                .collect(Collectors.toList());
    }
    
    // 이름과 효능 검색
    public List<SupplementDTO> searchSupplements(String name, String primaryFnclty, int page) {
        Specification<SupplementEntity> spec = searchByNameAndprimaryFnclty(name, primaryFnclty);
        // 'voter' 필드를 기준으로 내림차순 정렬
        Page<SupplementEntity> supplements = supplementRepository.findAll(spec, PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Order.desc("voter"))));
        return supplements.stream()
                .map(this::supplementToDTO)
                .collect(Collectors.toList());
    }
    
    // 전체 페이지 수 (전체 페이지 그룹을 계산하여 반환)
    public int getTotalPages() {
        long totalCount = supplementRepository.count();
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
    private Specification<SupplementEntity> searchByNameAndprimaryFnclty(String name, String primaryFnclty) {
        return (root, query, criteriaBuilder) -> {
            if (name != null && primaryFnclty != null) {
                return criteriaBuilder.and(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("prdlstNm")), "%" + name.toLowerCase() + "%"),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("primaryFnclty")), "%" + primaryFnclty.toLowerCase() + "%")
                );
            } else if (name != null) {
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("prdlstNm")), "%" + name.toLowerCase() + "%");
            } else if (primaryFnclty != null) {
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("primaryFnclty")), "%" + primaryFnclty.toLowerCase() + "%");
            }
            return criteriaBuilder.conjunction(); // 아무 조건 없이 모든 결과를 반환
        };
    }

    // SupplementEntity -> SupplementDTO 변환
    private SupplementDTO supplementToDTO(SupplementEntity supplement) {
        List<AnswerDTO> answerDTOList = supplement.getAnswerList().stream()
                .map(this::answerToAnswerDTO)
                .collect(Collectors.toList());

        return SupplementDTO.builder()
                .id(supplement.getId())
                .prdlstNm(supplement.getPrdlstNm())
                .bsshNm(supplement.getBsshNm())
                .primaryFnclty(supplement.getPrimaryFnclty())
                .rawmtrlNm(supplement.getRawmtrlNm())
                .answerList(answerDTOList)
                .voter(supplement.getVoterCount())  // 수정: getVoterCount() 메서드를 통해 좋아요 수를 반영
                .build();
    }

 // SupplementEntity에서 좋아요 수를 계산하는 메서드
    public int getVoterCount(SupplementEntity supplement) {
        return supplement.getVoter().size(); // 좋아요 수를 반환
    }
    
    
    // 카테고리 상세 페이지 조회
    public SupplementDTO getSupplementDetail(Long id) {
        SupplementEntity supplement = supplementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplement not found"));
        return supplementToDTO(supplement);
    }

    // 답변 추가
    public void addAnswer(Long supplementId, String content) {
        SupplementEntity supplement = supplementRepository.findById(supplementId)
                .orElseThrow(() -> new RuntimeException("Supplement not found"));
        
        AnswerEntity answer = new AnswerEntity();
        answer.setContent(content);
        answer.setSupplement(supplement);
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
                .build();
    }

 // 좋아요 추가
    public void voter(Long id, String username) {
        SupplementEntity supplement = supplementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplement not found"));
        
        // 이미 있는 'voter' 필드에 사용자 추가 (단, 사용자가 중복으로 추가되지 않도록 처리)
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        Set<MemberEntity> voters = supplement.getVoter();
        // 중복 확인 후 추가
        if (!voters.contains(member)) {
            voters.add(member);
            supplementRepository.save(supplement);
        }
    }
}