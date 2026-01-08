package com.petlog.healthcare.controller;

import com.petlog.healthcare.dto.meshy.Meshy3DRequest;
import com.petlog.healthcare.dto.meshy.Meshy3DResponse;
import com.petlog.healthcare.entity.Pet3DModel;
import com.petlog.healthcare.infrastructure.meshy.MeshyClient;
import com.petlog.healthcare.repository.Pet3DModelRepository;
import com.petlog.healthcare.service.MeshyAutoRefineService;
import com.petlog.healthcare.service.Pet3DModelService;
import com.petlog.healthcare.service.S3ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 3D 모델 생성 API 컨트롤러
 * WHY: Meshy.ai를 활용한 AI 기반 3D 펫 모델 생성
 *
 * @author healthcare-team
 * @since 2026-01-06
 */
@Slf4j
@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
@Tag(name = "3D Model", description = "AI 기반 3D 모델 생성 API")
public class Model3DController {

    private final MeshyClient meshyClient;
    private final MeshyAutoRefineService meshyAutoRefineService;
    private final Pet3DModelService pet3DModelService;
    private final S3ImageService s3ImageService;
    private final Pet3DModelRepository pet3DModelRepository;

    /**
     * ⭐ 펫 ID로 3D 모델 생성 (User Service + Social Service 연동)
     *
     * 플로우:
     * 1. petId → User Service 펫정보/프로필사진 조회
     * 2. userId → Social Service 피드 이미지 조회
     * 3. 랜덤 이미지 선택 → Meshy.ai 3D 생성
     *
     * @param petId         펫 ID
     * @param userId        유저 ID (피드 이미지 조회용)
     * @param authorization JWT 토큰
     * @return taskId 및 안내 메시지
     */
    @PostMapping("/pet/{petId}")
    @Operation(summary = "펫 3D 모델 생성", description = "펫 프로필 또는 피드 이미지 중 랜덤으로 선택하여 3D 모델을 생성합니다")
    public ResponseEntity<Meshy3DResponse> generatePetModel(
            @PathVariable Long petId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader("Authorization") String authorization) {

        log.info("🐕 펫 3D 모델 생성 요청: petId={}, userId={}", petId, userId);

        // userId 없으면 기본값 사용
        Long effectiveUserId = userId != null ? userId : 0L;

        Meshy3DResponse response = pet3DModelService.generatePet3DModel(petId, effectiveUserId, authorization);

        return ResponseEntity.ok(response);
    }

    /**
     * 텍스트로 3D 모델 생성
     *
     * @param request 프롬프트 (예: "cute golden retriever dog")
     * @return taskId 및 안내 메시지
     */
    @PostMapping("/generate-from-text")
    @Operation(summary = "텍스트→3D 모델 생성", description = "텍스트 설명으로 3D 모델을 생성합니다")
    public ResponseEntity<Meshy3DResponse> generateFromText(@RequestBody Meshy3DRequest.TextToModel request) {
        log.info("📥 3D 모델 생성 요청 (텍스트): {}", request.getPrompt());

        String taskId = meshyClient.generateFromText(request.getPrompt());

        Meshy3DResponse response = Meshy3DResponse.builder()
                .taskId(taskId)
                .status("queued")
                .message("3D 모델 생성이 시작되었습니다. /api/model/status/" + taskId + " 에서 상태를 확인하세요.")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * ⭐ 이미지로 3D 모델 생성 (자동 텍스처 적용!)
     * WHY: Preview 완료 후 자동으로 Refine하여 텍스처/색상 적용
     * 
     * ⚠️ 주의: 이 API는 최대 5-10분 소요될 수 있습니다 (동기식)
     *
     * @param request 이미지 URL
     * @return 텍스처가 적용된 최종 3D 모델 응답
     */
    @PostMapping("/generate-from-image")
    @Operation(summary = "이미지→3D 모델 생성 (자동 텍스처)", description = "이미지에서 3D 모델을 생성하고 자동으로 텍스처를 적용합니다. 완료까지 5-10분 소요됩니다.")
    public ResponseEntity<Meshy3DResponse> generateFromImage(@RequestBody Meshy3DRequest.ImageToModel request) {
        log.info("📥 3D 모델 생성 요청 (이미지 + 자동 Refine): {}", request.getImageUrl());

        // ⭐ 동기식 자동 Refine (Preview → Refine → 완료까지 대기)
        Meshy3DResponse response = meshyAutoRefineService.generateWithAutoRefine(request.getImageUrl());

        return ResponseEntity.ok(response);
    }

    /**
     * 작업 상태 조회 (기본)
     *
     * @param taskId 작업 ID
     * @return 상태 및 결과 URL
     */
    @GetMapping("/status/{taskId}")
    @Operation(summary = "3D 모델 생성 상태 조회", description = "생성 진행 상태와 완료 시 다운로드 URL을 반환합니다")
    public ResponseEntity<Meshy3DResponse> getStatus(@PathVariable String taskId) {
        log.info("📊 3D 모델 상태 조회: {}", taskId);

        var status = meshyClient.getTaskStatus(taskId);

        Meshy3DResponse response = Meshy3DResponse.builder()
                .taskId((String) status.get("taskId"))
                .status((String) status.get("status"))
                .progress((Integer) status.get("progress"))
                .modelUrl((String) status.get("modelUrl"))
                .renderedImageUrl((String) status.get("renderedImageUrl"))
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * ⭐ 전체 상태 조회 (Preview + Refine 포함)
     * WHY: 자동 Refine 사용 시 최종 텍스처 적용 상태까지 조회
     *
     * @param taskId 작업 ID (Preview taskId)
     * @return 단계별 상태 및 최종 모델 URL
     */
    @GetMapping("/status/full/{taskId}")
    @Operation(summary = "3D 모델 전체 상태 조회 (텍스처 포함)", description = "Preview → Refine 전체 프로세스 상태를 조회합니다")
    public ResponseEntity<Map<String, Object>> getFullStatus(@PathVariable String taskId) {
        log.info("📊 3D 모델 전체 상태 조회: {}", taskId);

        Map<String, Object> status = meshyAutoRefineService.getFinalStatus(taskId);
        return ResponseEntity.ok(status);
    }

    /**
     * ⭐ Preview 모델을 Refine하여 텍스처/색상 완성
     * WHY: Preview는 형태만, Refine은 원본 이미지 색상과 텍스처까지 완성
     *
     * @param previewTaskId Preview 단계에서 받은 taskId
     * @return 새로운 refineTaskId
     */
    @PostMapping("/refine/{previewTaskId}")
    @Operation(summary = "3D 모델 Refine (텍스처 완성)", description = "Preview 모델에 색상과 텍스처를 적용하여 완성본을 생성합니다")
    public ResponseEntity<Meshy3DResponse> refineModel(@PathVariable String previewTaskId) {
        log.info("🎨 3D 모델 Refine 요청: previewTaskId={}", previewTaskId);

        String refineTaskId = meshyClient.refinePreviewTask(previewTaskId);

        Meshy3DResponse response = Meshy3DResponse.builder()
                .taskId(refineTaskId)
                .status("queued")
                .message("텍스처 적용이 시작되었습니다! /api/model/status/" + refineTaskId + " 에서 상태를 확인하세요.")
                .build();

        return ResponseEntity.ok(response);
    }

    // ================== 파일 업로드 & 저장된 모델 API ==================

    /**
     * ⭐ 파일 직접 업로드로 3D 모델 생성 (자동 텍스처 적용!)
     * WHY: S3 URL이 아닌 로컬 파일을 직접 업로드해서 3D 생성 + 텍스처 적용
     *
     * ⚠️ 주의: 완료까지 5-10분 소요 (동기식)
     *
     * @param file   이미지 파일
     * @param petId  펫 ID (옵션)
     * @param userId 유저 ID
     * @return 텍스처가 적용된 최종 3D 모델 응답
     */
    @PostMapping(value = "/generate-from-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "파일 업로드로 3D 모델 생성 (자동 텍스처)", description = "이미지 파일을 직접 업로드하여 3D 모델을 생성하고 텍스처를 자동 적용합니다. 완료까지 5-10분 소요됩니다.")
    public ResponseEntity<Meshy3DResponse> generateFromFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "petId", required = false) Long petId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        log.info("📁 파일 업로드 3D 생성 요청 (자동 텍스처): fileName={}, petId={}, userId={}",
                file.getOriginalFilename(), petId, userId);

        // 1. S3에 이미지 업로드
        String imageUrl = s3ImageService.uploadImage(file, "3d-models");
        if (imageUrl == null) {
            return ResponseEntity.badRequest().body(
                    Meshy3DResponse.builder()
                            .status("failed")
                            .message("이미지 업로드 실패")
                            .build());
        }

        log.info("✅ S3 업로드 완료: {}", imageUrl);

        // 2. ⭐ 자동 Retexture 서비스 사용 (Preview → Retexture → 완료)
        Meshy3DResponse response = meshyAutoRefineService.generateWithAutoRefine(imageUrl);

        // 3. DB에 저장 (petId가 있는경우, 최종 결과로 저장)
        if (petId != null && userId != null && "SUCCEEDED".equals(response.getStatus())) {
            Pet3DModel model = Pet3DModel.builder()
                    .userId(userId)
                    .petId(petId)
                    .sourceImageUrl(imageUrl)
                    .meshyTaskId(response.getTaskId())
                    .modelUrl(response.getModelUrl()) // ⭐ 최종 모델 URL 저장
                    .status("COMPLETED")
                    .progress(100)
                    .build();
            pet3DModelRepository.save(model);
            log.info("💾 3D 모델 정보 저장: petId={}, taskId={}, modelUrl={}",
                    petId, response.getTaskId(), response.getModelUrl());
        }

        // 원본 이미지 URL 추가
        return ResponseEntity.ok(Meshy3DResponse.builder()
                .taskId(response.getTaskId())
                .status(response.getStatus())
                .progress(response.getProgress())
                .modelUrl(response.getModelUrl())
                .renderedImageUrl(response.getRenderedImageUrl())
                .sourceImageUrl(imageUrl)
                .message(response.getMessage())
                .build());
    }

    /**
     * 특정 펫의 저장된 3D 모델 조회
     * WHY: 펫별로 생성된 3D 모델을 영구 저장하고 조회
     */
    @GetMapping("/pet/{petId}/saved")
    @Operation(summary = "펫의 저장된 3D 모델 조회", description = "펫별로 저장된 최신 3D 모델을 조회합니다")
    public ResponseEntity<Pet3DModel> getSavedPetModel(@PathVariable Long petId) {
        log.info("🔍 저장된 3D 모델 조회: petId={}", petId);

        return pet3DModelRepository.findTopByPetIdAndStatusOrderByCreatedAtDesc(petId, "SUCCEEDED")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 사용자의 전체 펫 3D 모델 목록 조회
     * WHY: 여러 마리 펫별로 3D 모델 목록 조회
     */
    @GetMapping("/user/all")
    @Operation(summary = "사용자의 전체 펫 3D 모델 조회", description = "사용자의 모든 펫 3D 모델 목록을 조회합니다")
    public ResponseEntity<List<Pet3DModel>> getUserPetModels(
            @RequestHeader(value = "X-User-Id") Long userId) {
        log.info("🔍 사용자 전체 3D 모델 조회: userId={}", userId);

        List<Pet3DModel> models = pet3DModelRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(models);
    }

    /**
     * 펫에 3D 모델이 있는지 확인
     * WHY: 프론트에서 최초 생성 여부 확인
     */
    @GetMapping("/pet/{petId}/exists")
    @Operation(summary = "펫 3D 모델 존재 확인", description = "펫에 생성된 3D 모델이 있는지 확인합니다")
    public ResponseEntity<Map<String, Object>> checkPetModelExists(@PathVariable Long petId) {
        log.info("🔍 3D 모델 존재 확인: petId={}", petId);

        boolean exists = pet3DModelRepository.existsByPetIdAndStatus(petId, "SUCCEEDED");

        return ResponseEntity.ok(Map.of(
                "petId", petId,
                "hasModel", exists));
    }
}
