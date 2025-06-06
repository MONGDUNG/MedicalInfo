package com.global.map.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.global.map.dto.EmergencyDTO;
import com.global.map.dto.ItemDTO;
import com.global.map.dto.MedinstDTO;
import com.global.map.dto.PharmacyDTO;
import com.global.map.entity.ConvenienceStoreEntity;
import com.global.map.entity.EmergencyEntity;
import com.global.map.entity.MedinstEntity;
import com.global.map.entity.PharmacyEntity;
import com.global.map.etc.MedicalFacility;
import com.global.map.repository.MedicalFacilityRepository;
import com.global.map.repository.MedinstRepository;

import lombok.Builder;
import lombok.RequiredArgsConstructor;


@Builder
@Service
@RequiredArgsConstructor // final이 붙은 필드를 파라미터로 받는 생성자를 만들어줌
public class MapService {
   
    private final MedinstRepository medinstRepository;
    private final MedicalFacilityRepository medicalFacilityRepository;
    
    private final double centerLat = 37.5665;  // 서울시청 (회원 주소로 변경 가능)
    private final double centerLng = 126.9780;
    
    

	public int getRadius(int level) {
    	int radius = 0;
    	switch(level) {
			case 0:
				radius = 80;
				break;
			case 1:
				radius = 160;
				break;
			case 2:
				radius = 320;
				break;
			case 3:
				radius = 640;
				break;
			case 4:
				radius = 1280;
				break;
			case 5:
				radius = 2560;
				break;
			case 6:
				radius = 5120;
				break;
			case 7:
				radius = 10240;
				break;
			case 8:
				radius = 20480;
				break;
			case 9:
				radius = 40960;
				break;
			case 10:
				radius = 81920;
				break;
			default:
				radius = 300000;
			
		}
		return radius;
	}
	
	public int getCategoryCode(String category) {
	    int code = 0;
	    switch(category) {
	        case "상급종합":
	            code = 1;
	            break;
	        case "종합병원":
	            code = 11;
	            break;
	        case "병원":
	            code = 21;
	            break;
	        case "요양병원":
	            code = 28;
	            break;
	        case "정신병원":
	            code = 29;
	            break;
	        case "의원":
	            code = 31;
	            break;
	        case "치과병원":
	            code = 41;
	            break;
	        case "치과의원":
	            code = 51;
	            break;
	        case "조산원":
	            code = 61;
	            break;
	        case "보건소":
	            code = 71;
	            break;
	        case "보건지소":
	            code = 72;
	            break;
	        case "보건진료소":
	            code = 73;
	            break;
	        case "보건의료원":
	            code = 75;
	            break;
	        case "한방병원":
	            code = 92;
	            break;
	        case "한의원":
	            code = 93;
	            break;
	        default:
	            code = 0;
	            break;
	    }
	    return code;
	}
	// 진료과로 가져오기
	public List<ItemDTO> getNearbyMedicalFacilitiesByDept(double lat, double lng, int level, String category, String deptName) {
	    int radius = getRadius(level);
	    
	    // 전체 데이터 가져오기
	    List<MedicalFacility> medicalFacilities = medicalFacilityRepository.findNearByHospitalsByDept(lat, lng, radius, category, deptName);
	    
	    int size = medicalFacilities.size(); // 리스트 크기 확인
	    if (size <= 200) {
	        return medicalFacilities.stream().map(this::itemToDTO).toList(); // 200개 이하라면 그대로 반환
	    }

	    // 랜덤한 200개의 인덱스를 선택
	    Random random = new Random();
	    Set<Integer> randomIndexes = new HashSet<>();
	    while (randomIndexes.size() < 200) {
	        randomIndexes.add(random.nextInt(size));
	    }

	    // 선택된 인덱스의 데이터만 새로운 리스트에 저장
	    List<ItemDTO> result = new ArrayList<>();
	    for (int index : randomIndexes) {
	        result.add(itemToDTO(medicalFacilities.get(index)));
	    }

	    return result;
	}
    // MapService.java
    public List<ItemDTO> getNearbyHospitals(double lat, double lng, int level, String category) {
    	double radius = getRadius(level);
    	List<MedicalFacility> medicalFacilities = medicalFacilityRepository.findNearByHospitals(lat, lng, radius, category);
    	 int size = medicalFacilities.size(); // 리스트 크기 확인
 	    if (size <= 200) {
 	        return medicalFacilities.stream().map(this::itemToDTO).toList(); // 200개 이하라면 그대로 반환
 	    }

 	    // 랜덤한 200개의 인덱스를 선택
 	    Random random = new Random();
 	    Set<Integer> randomIndexes = new HashSet<>();
 	    while (randomIndexes.size() < 200) {
 	        randomIndexes.add(random.nextInt(size));
 	    }

 	    // 선택된 인덱스의 데이터만 새로운 리스트에 저장
 	    List<ItemDTO> result = new ArrayList<>();
 	    for (int index : randomIndexes) {
 	        result.add(itemToDTO(medicalFacilities.get(index)));
 	    }

 	    return result;
    }
 
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371 * 1000; // 지구 반지름 (m)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    private MedinstDTO medinstToDTO(MedinstEntity entity) {
    	if (entity == null) {
			return null; // entity가 null인 경우 null 반환
		}
        return MedinstDTO.builder()
                .id(entity.getId())
                .hospitalCode(entity.getHospitalCode())
                .hospitalName(entity.getHospitalName())
                .categoryCode(entity.getCategoryCode())
                .postalCode(entity.getPostalCode())
                .address(entity.getAddress())
                .phoneNumber(entity.getPhoneNumber())
                .website(entity.getWebsite())
                .establishedDate(entity.getEstablishedDate() != null ? entity.getEstablishedDate().toString() : null)
                .totalDoc(entity.getTotalDoc())
                .latitude(Double.parseDouble(entity.getLatitude()))
                .longitude(Double.parseDouble(entity.getLongitude()))
                .build();
    }
    private PharmacyDTO pharmacyToDTO(PharmacyEntity entity) {
        return PharmacyDTO.builder()
                .id(entity.getId())
                .pharmacyName(entity.getPharmacyName())
                .address(entity.getAddress())
                .latitude(Double.parseDouble(entity.getLatitude()))
                .longitude(Double.parseDouble(entity.getLongitude()))
                .build();
    }
    private EmergencyDTO emergencyToDTO(EmergencyEntity entity) {
        return EmergencyDTO.builder()
                .erId(entity.getErId())
                .hospitalName(entity.getHospitalName())
                .address(entity.getAddress())
                .latitude(Double.parseDouble(entity.getLatitude()))
                .longitude(Double.parseDouble(entity.getLongitude()))
                .build();
    }


    private ItemDTO itemToDTO(MedicalFacility entity) {
        return ItemDTO.builder()
            .name(entity.getName())
            .address(entity.getAddress())
            .phone(entity.getPhone())
            .lat(entity.getLat())
            .lng(entity.getLng())
            .categoryName(entity.getCategoryName())
            .build();
    }

   private ItemDTO itemToDTO(ConvenienceStoreEntity entity) {
      return ItemDTO.builder()
            .name(entity.getStoreName())
            .address(entity.getAddress())
            .phone(entity.getPhoneNumber())
            .lat(Double.parseDouble(entity.getLatitude()))
            .lng(Double.parseDouble(entity.getLongitude()))
            .categoryName("편의점")
            .build();
   }

   private ItemDTO itemToDTO(EmergencyEntity entity) {
      return ItemDTO.builder()
            .name(entity.getHospitalName())
            .address(entity.getAddress())
            .phone(entity.getPhoneNumber())
            .lat(Double.parseDouble(entity.getLatitude()))
            .lng(Double.parseDouble(entity.getLongitude()))
            .categoryName("응급실")
            .build();
   }

   private ItemDTO itemToDTO(PharmacyEntity entity) {
      return ItemDTO.builder()
            .name(entity.getPharmacyName())
            .address(entity.getAddress())
            .phone(entity.getPhoneNumber())
            .lat(Double.parseDouble(entity.getLatitude()))
            .lng(Double.parseDouble(entity.getLongitude()))
            .categoryName("약국")
            .build();
   }

   private ItemDTO itemToDTO(MedinstEntity entity) {
      List<String> deptNames = getDeptNames(entity.getHospitalName(), Double.parseDouble(entity.getLatitude()), Double.parseDouble(entity.getLongitude()));
      String categoryName = categoryCDtoName(entity.getCategoryCode());
      return ItemDTO.builder()
            .name(entity.getHospitalName())
            .address(entity.getAddress())
            .phone(entity.getPhoneNumber())
            .lat(Double.parseDouble(entity.getLatitude()))
            .lng(Double.parseDouble(entity.getLongitude()))
            .deptName(deptNames)
            .categoryName(categoryName)
            .build();
   }
   private List<String> getDeptNames(String name, Double lat, Double lng) {
          if (name == null || lat == null || lng == null) {
              throw new IllegalArgumentException("name, lat, lng는 null이 될 수 없습니다.");
          }

          return Optional.ofNullable(medicalFacilityRepository.findByNameAndLatAndLng(name, lat, lng))
                  .orElse(Collections.emptyList()) // repository가 null 반환하면 빈 리스트로 처리
                  .stream()
                  .map(MedicalFacility::getDeptName) // deptName(String) 반환
                  .filter(Objects::nonNull) // null 값 제거
                  .map(dept -> dept.split(",")) // 쉼표로 나눈 후 배열로 변환
                  .flatMap(Arrays::stream) // 배열을 Stream으로 변환 (제네릭 문제 해결됨)
                  .map(String::trim) // 공백 제거
                  .filter(s -> !s.isEmpty()) // 빈 문자열 제거
                  .distinct() // 중복 제거
                  .collect(Collectors.toList());
      }
   private String categoryCDtoName(String code) {
         String categoryName = null;
         switch (code) {
            case "1" : categoryName = "상급종합";
            break;
            case "11" : categoryName = "종합병원";
            break;
            case "21" : categoryName = "병원";
            break;
            case "28" : categoryName = "요양병원";
            break;
            case "29" : categoryName = "정신병원";
            break;
            case "31" : categoryName = "의원";
            break;
            case "41" : categoryName = "치과병원";
            break;
            case "51" : categoryName = "치과의원";
            break;
            case "61" : categoryName = "조산원";
            break;
            case "71" : categoryName = "보건소";
            break;
            case "72" : categoryName = "보건지소";
            break;
            case "73" : categoryName = "보건진료소";
            break;
            case "75" : categoryName = "보건의료원";
            break;
            case "92" : categoryName = "한방병원";
            break;
            case "93" : categoryName = "한의원";
            break;
            default : categoryName = "기타";
            break;
         }
         return categoryName;
      }
   
   public String findHCdByHNmAndAdr(String hospitalName, String address) {
       return medinstRepository.findHCdByHNmAndAdr(hospitalName, address)
               .orElseThrow(() -> new RuntimeException("Hospital code not found"));
   }
   public MedinstDTO getHospitalInfo(String hospitalCode) {
	   MedinstEntity entity = medinstRepository.findByHospitalCode(hospitalCode);
	   return medinstToDTO(entity);
   }
   public void updateReviewCount(String hospitalCode) {
	   medinstRepository.updateReviewCount(hospitalCode);
   }
   public void decreaseReviewCount(String hospitalCode) {
	   medinstRepository.decreaseReviewCount(hospitalCode);
   }
   public void updateAvgRating(String hospitalCode, double avgRating) {
	   medinstRepository.updateRating(hospitalCode, avgRating);
   }
   //리뷰수 가져오는 메서드
   public int getReviewCount(String hospitalCode) {
	   return medinstRepository.findReviewCountByHospitalCode(hospitalCode);
   }
   public double getAvgRating(String hospitalCode) {
	   return medinstRepository.findAvgRatingByHospitalCode(hospitalCode);
   }
}
   







