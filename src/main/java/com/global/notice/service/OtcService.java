package com.global.notice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.global.notice.dto.OtcDTO;
import com.global.notice.entity.OtcEntity;
import com.global.notice.repository.OtcRepository;


@Service
public class OtcService {

    private final OtcRepository otcRepository;

    public OtcService(OtcRepository otcRepository) {
        this.otcRepository = otcRepository;
    }

    public List<OtcEntity> getAllOtc() {
        return otcRepository.findAll();
    }
    
 // 카테고리 상세 페이지 조회
    public OtcDTO getOtcDetail(Integer id) {
    	return otcRepository.findById(id)
                .map(this::otcToDTO)
                .orElse(null);
    }
    
 // OtcEntity -> OtcDTO 변환
    private OtcDTO otcToDTO(OtcEntity otc) {

        return OtcDTO.builder()
                .id(otc.getId())
                .medicineType(otc.getMedicineType())
                .medicineName(otc.getMedicineName())
                .packageUnit(otc.getPackageUnit())
                .img(otc.getImg())
                .indications(otc.getIndications())
                .dosageInstructions(otc.getDosageInstructions())
                .precautions(otc.getPrecautions())
                .manufacturer(otc.getManufacturer())
                .build();
    }
}