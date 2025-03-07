
package com.global.map.etc;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalFacility {

    @Id
    private Integer id;

    @Field(type = FieldType.Text, name = "NAME")
    private String name;

    @Field(type = FieldType.Text, name = "ADDRESS")
    private String address;

    @Field(type = FieldType.Text, name = "PHONE")
    private String phone;

    @Field(type = FieldType.Double, name = "LAT")
    private Double lat;

    @Field(type = FieldType.Double, name = "LNG")
    private Double lng;

    @Field(type = FieldType.Text, name = "CATEGORY_NAME")
    private String categoryName;

    @Field(type = FieldType.Text, name = "DEPT_NAME")
    private String deptName;


}
