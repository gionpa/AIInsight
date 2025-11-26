package com.aiinsight.domain.crawl;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelectorConfig {

    // 기사 목록 컨테이너 선택자
    private String articleListSelector;

    // 개별 기사 아이템 선택자
    private String articleItemSelector;

    // 제목 선택자
    private String titleSelector;

    // 링크 선택자 (href 속성을 가진 요소)
    private String linkSelector;

    // 본문 선택자 (상세 페이지용)
    private String contentSelector;

    // 작성자 선택자
    private String authorSelector;

    // 날짜 선택자
    private String dateSelector;

    // 날짜 포맷 (예: "yyyy-MM-dd", "MMM dd, yyyy")
    private String dateFormat;

    // 썸네일 이미지 선택자
    private String thumbnailSelector;

    // 페이지네이션 설정
    private PaginationConfig pagination;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaginationConfig {
        private Boolean enabled;
        private String nextPageSelector;  // 다음 페이지 버튼 선택자
        private String pageParamName;     // URL 파라미터 이름 (예: "page")
        private Integer maxPages;         // 최대 페이지 수
    }
}
