package com.aiinsight.controller;

import com.aiinsight.dto.InterestTopicDto;
import com.aiinsight.service.InterestTopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interest-topics")
@RequiredArgsConstructor
@Tag(name = "관심 주제 관리", description = "사용자별 관심 주제 CRUD 및 리포트 API")
public class InterestTopicController {

    private final InterestTopicService interestTopicService;

    @GetMapping
    @Operation(summary = "관심 주제 목록 조회", description = "사용자의 모든 관심 주제를 조회합니다")
    public ResponseEntity<List<InterestTopicDto.Response>> getTopics() {
        return ResponseEntity.ok(interestTopicService.getTopics());
    }

    @GetMapping("/active")
    @Operation(summary = "활성 관심 주제 목록 조회", description = "활성화된 관심 주제만 조회합니다")
    public ResponseEntity<List<InterestTopicDto.Response>> getActiveTopics() {
        return ResponseEntity.ok(interestTopicService.getActiveTopics());
    }

    @GetMapping("/{id}")
    @Operation(summary = "관심 주제 상세 조회", description = "특정 관심 주제의 상세 정보를 조회합니다")
    public ResponseEntity<InterestTopicDto.Response> getTopic(@PathVariable Long id) {
        return ResponseEntity.ok(interestTopicService.getTopic(id));
    }

    @PostMapping
    @Operation(summary = "관심 주제 생성", description = "새로운 관심 주제를 생성합니다")
    public ResponseEntity<InterestTopicDto.Response> createTopic(
            @Valid @RequestBody InterestTopicDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interestTopicService.createTopic(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "관심 주제 수정", description = "관심 주제의 정보를 수정합니다")
    public ResponseEntity<InterestTopicDto.Response> updateTopic(
            @PathVariable Long id,
            @Valid @RequestBody InterestTopicDto.UpdateRequest request) {
        return ResponseEntity.ok(interestTopicService.updateTopic(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "관심 주제 삭제", description = "관심 주제를 삭제합니다")
    public ResponseEntity<Void> deleteTopic(@PathVariable Long id) {
        interestTopicService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reorder")
    @Operation(summary = "관심 주제 순서 변경", description = "관심 주제의 표시 순서를 변경합니다")
    public ResponseEntity<List<InterestTopicDto.Response>> reorderTopics(
            @RequestBody InterestTopicDto.ReorderRequest request) {
        return ResponseEntity.ok(interestTopicService.reorderTopics(request));
    }

    @GetMapping("/{id}/report")
    @Operation(summary = "관심 주제별 리포트 조회", description = "특정 관심 주제에 해당하는 기사들의 리포트를 조회합니다")
    public ResponseEntity<InterestTopicDto.TopicReportResponse> getTopicReport(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(interestTopicService.getTopicReport(id, limit));
    }

    @GetMapping("/report")
    @Operation(summary = "전체 관심 주제 리포트 조회", description = "모든 활성 관심 주제에 대한 리포트를 조회합니다")
    public ResponseEntity<InterestTopicDto.AllTopicsReportResponse> getAllTopicsReport(
            @RequestParam(defaultValue = "5") int articlesPerTopic) {
        return ResponseEntity.ok(interestTopicService.getAllTopicsReport(articlesPerTopic));
    }

    @PostMapping("/initialize")
    @Operation(summary = "기본 주제 초기화", description = "기본 관심 주제 6개를 초기화합니다")
    public ResponseEntity<List<InterestTopicDto.Response>> initializeDefaultTopics() {
        interestTopicService.initializeDefaultTopics();
        return ResponseEntity.ok(interestTopicService.getTopics());
    }
}
