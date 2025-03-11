package com.global.map.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDTO {

	    private String name;
	    private String address;
	    private String phone;
	    private Double lat;
	    private Double lng;
	    private String categoryName;
	    private String deptName;
}
