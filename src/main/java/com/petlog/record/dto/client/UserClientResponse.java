package com.petlog.record.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserClientResponse {

    // UserResponse.GetUserDto 구조에 맞춤
    private String username;        // 사용자 이름(닉네임)
    private String genderType;      // 성별 (Enum -> String)
    private String profileImage;    // 프로필 사진
    private String statusMessage;   // 상태메세지
    private Integer age;            // 나이

    // 필요 시 추가 (GetUserDto에 있는 나머지 필드)
    // private Integer currentLat;
    // private Integer currentLng;
    // private Long petCoin;
}