package com.aiinsight.domain.topic;

import com.aiinsight.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자별 관심 주제 엔티티
 * 키워드 기반으로 기사를 필터링하여 관심 주제별 리포트 제공
 */
@Entity
@Table(name = "interest_topics", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * 쉼표로 구분된 키워드 목록
     * 예: "google, gemini, deepmind, 구글, 제미나이"
     */
    @Column(nullable = false, length = 1000)
    private String keywords;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 키워드 목록을 배열로 반환
     */
    public String[] getKeywordArray() {
        if (keywords == null || keywords.isEmpty()) {
            return new String[0];
        }
        return keywords.split(",\\s*");
    }

    /**
     * 주어진 텍스트가 이 주제의 키워드를 포함하는지 확인
     */
    public boolean matchesText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lowerText = text.toLowerCase();
        for (String keyword : getKeywordArray()) {
            if (lowerText.contains(keyword.toLowerCase().trim())) {
                return true;
            }
        }
        return false;
    }
}
