package com.global.map.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.global.map.entity.MedInfoEntity;

public interface MedInfoRepository extends JpaRepository<MedInfoEntity, Long>{
	Optional<MedInfoEntity> findByHospitalCode(String hospitalCode);

}
