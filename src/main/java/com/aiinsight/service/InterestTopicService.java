package com.aiinsight.service;

import com.aiinsight.domain.article.NewsArticle;
import com.aiinsight.domain.article.NewsArticleRepository;
import com.aiinsight.domain.topic.InterestTopic;
import com.aiinsight.domain.topic.InterestTopicRepository;
import com.aiinsight.domain.user.User;
import com.aiinsight.domain.user.UserRepository;
import com.aiinsight.dto.InterestTopicDto;
import com.aiinsight.dto.NewsArticleDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InterestTopicService {

    private final InterestTopicRepository interestTopicRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final UserRepository userRepository;

    // 임시 기본 사용자 ID (나중에 로그인 기능 추가 시 변경)
    private static final Long DEFAULT_USER_ID = 1L;

    /**
     * 현재 사용자의 관심 주제 목록 조회
     */
    public List<InterestTopicDto.Response> getTopics() {
        return interestTopicRepository.findByUserIdOrderByDisplayOrderAsc(DEFAULT_USER_ID)
                .stream()
                .map(InterestTopicDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 활성화된 관심 주제 목록만 조회
     */
    public List<InterestTopicDto.Response> getActiveTopics() {
        return interestTopicRepository.findByUserIdAndIsActiveTrueOrderByDisplayOrderAsc(DEFAULT_USER_ID)
                .stream()
                .map(InterestTopicDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 관심 주제 상세 조회
     */
    public InterestTopicDto.Response getTopic(Long topicId) {
        InterestTopic topic = interestTopicRepository.findByIdAndUserId(topicId, DEFAULT_USER_ID)
                .orElseThrow(() -> new EntityNotFoundException("관심 주제를 찾을 수 없습니다: " + topicId));
        return InterestTopicDto.Response.from(topic);
    }

    /**
     * 관심 주제 생성
     */
    @Transactional
    public InterestTopicDto.Response createTopic(InterestTopicDto.CreateRequest request) {
        User user = getOrCreateDefaultUser();

        // 중복 이름 체크
        if (interestTopicRepository.existsByUserIdAndName(DEFAULT_USER_ID, request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 주제 이름입니다: " + request.getName());
        }

        // 다음 순서 계산
        Integer nextOrder = interestTopicRepository.findMaxDisplayOrderByUserId(DEFAULT_USER_ID) + 1;

        InterestTopic topic = InterestTopic.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .keywords(request.getKeywords())
                .displayOrder(nextOrder)
                .isActive(true)
                .build();

        InterestTopic saved = interestTopicRepository.save(topic);
        log.info("관심 주제 생성: {} (ID: {})", saved.getName(), saved.getId());
        return InterestTopicDto.Response.from(saved);
    }

    /**
     * 관심 주제 수정
     */
    @Transactional
    public InterestTopicDto.Response updateTopic(Long topicId, InterestTopicDto.UpdateRequest request) {
        InterestTopic topic = interestTopicRepository.findByIdAndUserId(topicId, DEFAULT_USER_ID)
                .orElseThrow(() -> new EntityNotFoundException("관심 주제를 찾을 수 없습니다: " + topicId));

        // 이름 변경 시 중복 체크
        if (request.getName() != null && !request.getName().equals(topic.getName())) {
            if (interestTopicRepository.existsByUserIdAndNameAndIdNot(DEFAULT_USER_ID, request.getName(), topicId)) {
                throw new IllegalArgumentException("이미 존재하는 주제 이름입니다: " + request.getName());
            }
            topic.setName(request.getName());
        }

        if (request.getDescription() != null) {
            topic.setDescription(request.getDescription());
        }
        if (request.getKeywords() != null) {
            topic.setKeywords(request.getKeywords());
        }
        if (request.getIsActive() != null) {
            topic.setIsActive(request.getIsActive());
        }

        InterestTopic saved = interestTopicRepository.save(topic);
        log.info("관심 주제 수정: {} (ID: {})", saved.getName(), saved.getId());
        return InterestTopicDto.Response.from(saved);
    }

    /**
     * 관심 주제 삭제
     */
    @Transactional
    public void deleteTopic(Long topicId) {
        InterestTopic topic = interestTopicRepository.findByIdAndUserId(topicId, DEFAULT_USER_ID)
                .orElseThrow(() -> new EntityNotFoundException("관심 주제를 찾을 수 없습니다: " + topicId));

        Integer deletedOrder = topic.getDisplayOrder();
        interestTopicRepository.delete(topic);

        // 삭제된 순서 이후의 항목들 순서 조정
        interestTopicRepository.decrementDisplayOrderAfter(DEFAULT_USER_ID, deletedOrder);

        log.info("관심 주제 삭제: {} (ID: {})", topic.getName(), topicId);
    }

    /**
     * 관심 주제 순서 변경
     */
    @Transactional
    public List<InterestTopicDto.Response> reorderTopics(InterestTopicDto.ReorderRequest request) {
        List<Long> topicIds = request.getTopicIds();

        for (int i = 0; i < topicIds.size(); i++) {
            InterestTopic topic = interestTopicRepository.findByIdAndUserId(topicIds.get(i), DEFAULT_USER_ID)
                    .orElseThrow(() -> new EntityNotFoundException("관심 주제를 찾을 수 없습니다"));
            topic.setDisplayOrder(i + 1);
            interestTopicRepository.save(topic);
        }

        log.info("관심 주제 순서 변경 완료");
        return getTopics();
    }

    /**
     * 특정 관심 주제에 해당하는 기사 조회
     */
    public InterestTopicDto.TopicReportResponse getTopicReport(Long topicId, int limit) {
        InterestTopic topic = interestTopicRepository.findByIdAndUserId(topicId, DEFAULT_USER_ID)
                .orElseThrow(() -> new EntityNotFoundException("관심 주제를 찾을 수 없습니다: " + topicId));

        List<NewsArticle> allArticles = newsArticleRepository.findAll(PageRequest.of(0, 500)).getContent();

        List<NewsArticle> matchedArticles = allArticles.stream()
                .filter(article -> matchesTopicKeywords(article, topic))
                .sorted((a, b) -> {
                    // HIGH importance 우선, 그 다음 최신순
                    int importanceCompare = compareImportance(b.getImportance(), a.getImportance());
                    if (importanceCompare != 0) return importanceCompare;
                    return b.getCrawledAt().compareTo(a.getCrawledAt());
                })
                .limit(limit)
                .collect(Collectors.toList());

        int highCount = (int) matchedArticles.stream()
                .filter(a -> a.getImportance() == NewsArticle.ArticleImportance.HIGH)
                .count();

        return InterestTopicDto.TopicReportResponse.builder()
                .topicId(topic.getId())
                .topicName(topic.getName())
                .description(topic.getDescription())
                .keywords(topic.getKeywords())
                .totalArticles(matchedArticles.size())
                .highImportanceCount(highCount)
                .articles(matchedArticles.stream()
                        .map(NewsArticleDto.Response::from)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 모든 활성 관심 주제에 대한 리포트 조회
     */
    public InterestTopicDto.AllTopicsReportResponse getAllTopicsReport(int articlesPerTopic) {
        List<InterestTopic> activeTopics = interestTopicRepository
                .findByUserIdAndIsActiveTrueOrderByDisplayOrderAsc(DEFAULT_USER_ID);

        List<NewsArticle> allArticles = newsArticleRepository.findAll(PageRequest.of(0, 500)).getContent();

        List<InterestTopicDto.TopicReportResponse> topicReports = new ArrayList<>();
        int totalArticles = 0;

        for (InterestTopic topic : activeTopics) {
            List<NewsArticle> matchedArticles = allArticles.stream()
                    .filter(article -> matchesTopicKeywords(article, topic))
                    .sorted((a, b) -> {
                        int importanceCompare = compareImportance(b.getImportance(), a.getImportance());
                        if (importanceCompare != 0) return importanceCompare;
                        return b.getCrawledAt().compareTo(a.getCrawledAt());
                    })
                    .limit(articlesPerTopic)
                    .collect(Collectors.toList());

            int highCount = (int) matchedArticles.stream()
                    .filter(a -> a.getImportance() == NewsArticle.ArticleImportance.HIGH)
                    .count();

            topicReports.add(InterestTopicDto.TopicReportResponse.builder()
                    .topicId(topic.getId())
                    .topicName(topic.getName())
                    .description(topic.getDescription())
                    .keywords(topic.getKeywords())
                    .totalArticles(matchedArticles.size())
                    .highImportanceCount(highCount)
                    .articles(matchedArticles.stream()
                            .map(NewsArticleDto.Response::from)
                            .collect(Collectors.toList()))
                    .build());

            totalArticles += matchedArticles.size();
        }

        return InterestTopicDto.AllTopicsReportResponse.builder()
                .generatedAt(LocalDateTime.now())
                .totalTopics(activeTopics.size())
                .totalArticles(totalArticles)
                .topics(topicReports)
                .build();
    }

    /**
     * 기사가 주제의 키워드와 매칭되는지 확인
     */
    private boolean matchesTopicKeywords(NewsArticle article, InterestTopic topic) {
        String searchText = buildSearchText(article);
        return topic.matchesText(searchText);
    }

    private String buildSearchText(NewsArticle article) {
        StringBuilder sb = new StringBuilder();
        if (article.getTitle() != null) sb.append(article.getTitle()).append(" ");
        if (article.getTitleKo() != null) sb.append(article.getTitleKo()).append(" ");
        if (article.getSummary() != null) sb.append(article.getSummary()).append(" ");
        return sb.toString();
    }

    private int compareImportance(NewsArticle.ArticleImportance a, NewsArticle.ArticleImportance b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return Integer.compare(getImportanceOrder(a), getImportanceOrder(b));
    }

    private int getImportanceOrder(NewsArticle.ArticleImportance importance) {
        return switch (importance) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    /**
     * 기본 사용자 조회 또는 생성
     */
    private User getOrCreateDefaultUser() {
        return userRepository.findById(DEFAULT_USER_ID)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email("default@aiinsight.com")
                            .name("Default User")
                            .build();
                    return userRepository.save(newUser);
                });
    }

    /**
     * 기본 주제 초기화 (최초 실행 시)
     */
    @Transactional
    public void initializeDefaultTopics() {
        if (interestTopicRepository.countByUserId(DEFAULT_USER_ID) > 0) {
            log.info("이미 관심 주제가 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        User user = getOrCreateDefaultUser();

        List<InterestTopicDto.CreateRequest> defaultTopics = List.of(
                InterestTopicDto.CreateRequest.builder()
                        .name("Google AI/Gemini")
                        .description("구글 AI와 제미나이 관련 소식")
                        .keywords("google, gemini, deepmind, 구글, 제미나이")
                        .build(),
                InterestTopicDto.CreateRequest.builder()
                        .name("AI 코딩/개발도구")
                        .description("AI 기반 코딩 도구 및 개발자 도구")
                        .keywords("copilot, code, coding, developer, 코딩, 개발")
                        .build(),
                InterestTopicDto.CreateRequest.builder()
                        .name("AI 제품/서비스")
                        .description("AI 제품 출시 및 서비스 소식")
                        .keywords("product, launch, release, 출시, 제품")
                        .build(),
                InterestTopicDto.CreateRequest.builder()
                        .name("Microsoft AI")
                        .description("마이크로소프트 AI 관련 소식")
                        .keywords("microsoft, azure, 마이크로소프트")
                        .build(),
                InterestTopicDto.CreateRequest.builder()
                        .name("OpenAI/GPT")
                        .description("OpenAI와 GPT 관련 소식")
                        .keywords("openai, gpt, chatgpt")
                        .build(),
                InterestTopicDto.CreateRequest.builder()
                        .name("AI 연구/벤치마크")
                        .description("AI 연구 동향 및 벤치마크")
                        .keywords("research, paper, benchmark, study, 연구")
                        .build()
        );

        for (InterestTopicDto.CreateRequest request : defaultTopics) {
            createTopic(request);
        }

        log.info("기본 관심 주제 {} 개 초기화 완료", defaultTopics.size());
    }
}
