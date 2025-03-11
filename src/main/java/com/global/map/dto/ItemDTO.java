package com.global.map.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDTO {

    @JsonProperty("NAME")
    private String name;

    @JsonProperty("ADDRESS")
    private String address;

    @JsonProperty("PHONE")
    private String phone;

    @JsonProperty("LAT")
    private double lat;

    @JsonProperty("LNG")
    private double lng;

    @JsonProperty("DEPT_NAME")
    private List<String> deptName;

    @JsonProperty("CATEGORY_NAME")
    private String categoryName;
}