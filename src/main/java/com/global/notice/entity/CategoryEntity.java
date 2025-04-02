package com.global.notice.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CATEGORY")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CategoryEntity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID는 AUTO_INCREMENT니까 이거 맞춰줌
    @Column(name = "ID")
	private Long id;
	
	 	@Column(name = "ITEM_STANDARD_CODE", length = 20, nullable = false)
	 	private String itemStandardCode;

	    @Column(name = "PRODUCT_NAME", length = 500, nullable = false)
	    private String productName;

	    @Column(name = "COMPANY_NAME", length = 50, nullable = false)
	    private String companyName;

	    @Column(name = "ACTIVE_INGREDIENT", length = 500, nullable = false)
	    private String activeIngredient;

	    @Column(name = "ADDITIVES", length = 500, nullable = false)
	    private String additives;

	    @Column(name = "MANUFACTURING_TYPE", length = 20, nullable = false)
	    private String manufacturingType;

	    @Column(name = "STANDARD_CODE", length = 2000, nullable = false)
	    private String standardCode;

	    @Column(name = "DRUG_DETAIL_CODE", length = 20)
	    private String drugDetailCode;

	    @Column(name = "COUNTRY_OF_IMPORT", length = 20)
	    private String countryOfImport;
	    
	    @Column(name = "INDICATIONS", length = 500)
	    private String indications;
		
	    @OneToMany(mappedBy="category" , cascade=CascadeType.REMOVE)
		private List<AnswerEntity> answerList;
}