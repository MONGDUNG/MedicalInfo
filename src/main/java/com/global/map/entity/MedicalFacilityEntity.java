package com.global.map.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "MEDICAL_FACILITY")
public class MedicalFacilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String hospitalName;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String latitude;

    @Column(nullable = false)
    private String longitude;

    @Column(nullable = true)
    private String mon;

    @Column(nullable = true)
    private String tues;

    @Column(nullable = true)
    private String wednes;

    @Column(nullable = true)
    private String thurs;

    @Column(nullable = true)
    private String fri;

    @Column(nullable = true)
    private String satur;

    @Column(nullable = true)
    private String sun;

    @Column(nullable = true)
    private String holi;

    @Builder
    public MedicalFacilityEntity(String hospitalName, String address, String phoneNumber,
                                 String latitude, String longitude, String mon, String tues, 
                                 String wednes, String thurs, String fri, String satur, 
                                 String sun, String holi) {
        this.hospitalName = hospitalName;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mon = mon;
        this.tues = tues;
        this.wednes = wednes;
        this.thurs = thurs;
        this.fri = fri;
        this.satur = satur;
        this.sun = sun;
        this.holi = holi;
    }
}

