package com.petlog.record.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetClientResponse {

    // PetResponse.GetPetDto 구조에 맞춤
    private Long petId;
    //펫 이름
    private String petName;
    //종류
    private Species species;
    //품종
    private String breed;
    //성별
    private GenderType genderType;
    //중성화여부
    private boolean is_neutered;
    //프로필 사진
    private String profileImage;
    //나이
    private Integer age;
    //생일
    private LocalDate birth;
    //상태
    private Status status;
}