package com.petlog.record.entity;

public enum ImageSource {
    GALLERY, // 외부 갤러리 (새로 업로드) -> 외부 서비스 전송 O
    ARCHIVE  // 내부 보관함 (기존 사진 선택) -> 외부 서비스 전송 X
}