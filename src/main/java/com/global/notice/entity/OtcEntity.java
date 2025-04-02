package com.global.notice.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "OTC")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class OtcEntity {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID는 AUTO_INCREMENT니까 이거 맞춰줌
    @Column(name = "ID")
	private Integer id;
	
	@Column(name = "MEDICINE_TYPE", length = 20, nullable = false)
 	private String medicineType;

    @Column(name = "MEDICINE_NAME", length = 50, nullable = false)
    private String medicineName;
    
    @Column(name = "PACKAGE_UNIT", length = 50)
    private String packageUnit;
    
    @Column(name = "IMG", length = 500)
    private String img;
    
    @Column(name = "INDICATIONS", length = 500, nullable = false)
 	private String indications;
    
    @Column(name = "DOSAGE_INSTRUCTIONS", length = 2000, nullable = false)
 	private String dosageInstructions;
    
    @Column(name = "PRECAUTIONS", length = 9000, nullable = false)
 	private String precautions;
    
    @Column(name = "MANUFACTURER", length = 20, nullable = false)
 	private String manufacturer;

}
