package com.aiinsight.service;

import com.aiinsight.domain.crawl.CrawlHistory;
import com.aiinsight.domain.crawl.CrawlHistoryRepository;
import com.aiinsight.domain.crawl.CrawlTarget;
import com.aiinsight.dto.CrawlHistoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CrawlHistoryService {

    private final CrawlHistoryRepository crawlHistoryRepository;

    public Page<CrawlHistoryDto.Response> findByTargetId(Long targetId, Pageable pageable) {
        return crawlHistoryRepository.findByTargetIdOrderByExecutedAtDesc(targetId, pageable)
                .map(CrawlHistoryDto.Response::from);
    }

    public Optional<CrawlHistoryDto.Response> findLatestByTargetId(Long targetId) {
        return crawlHistoryRepository.findTopByTargetIdOrderByExecutedAtDesc(targetId)
                .map(CrawlHistoryDto.Response::from);
    }

    public List<CrawlHistoryDto.Response> findTodayHistory() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return crawlHistoryRepository.findByExecutedAtAfterOrderByExecutedAtDesc(startOfDay).stream()
                .map(CrawlHistoryDto.Response::from)
                .collect(Collectors.toList());
    }

    public Page<CrawlHistoryDto.Response> findAllHistory(Pageable pageable) {
        return crawlHistoryRepository.findAllByOrderByExecutedAtDesc(pageable)
                .map(CrawlHistoryDto.Response::from);
    }

    @Transactional
    public CrawlHistory recordSuccess(CrawlTarget target, int articlesFound, int articlesNew, long durationMs) {
        CrawlHistory history = CrawlHistory.builder()
                .target(target)
                .status(CrawlTarget.CrawlStatus.SUCCESS)
                .articlesFound(articlesFound)
                .articlesNew(articlesNew)
                .durationMs(durationMs)
                .build();

        CrawlHistory saved = crawlHistoryRepository.save(history);
        log.info("크롤링 성공 기록: {} - 발견: {}, 신규: {}", target.getName(), articlesFound, articlesNew);
        return saved;
    }

    @Transactional
    public CrawlHistory recordFailure(CrawlTarget target, String errorMessage, long durationMs) {
        CrawlHistory history = CrawlHistory.builder()
                .target(target)
                .status(CrawlTarget.CrawlStatus.FAILED)
                .articlesFound(0)
                .articlesNew(0)
                .durationMs(durationMs)
                .errorMessage(errorMessage)
                .build();

        CrawlHistory saved = crawlHistoryRepository.save(history);
        log.error("크롤링 실패 기록: {} - {}", target.getName(), errorMessage);
        return saved;
    }

    @Transactional
    public CrawlHistory recordPartial(CrawlTarget target, int articlesFound, int articlesNew,
                                       String errorMessage, long durationMs) {
        CrawlHistory history = CrawlHistory.builder()
                .target(target)
                .status(CrawlTarget.CrawlStatus.PARTIAL)
                .articlesFound(articlesFound)
                .articlesNew(articlesNew)
                .durationMs(durationMs)
                .errorMessage(errorMessage)
                .build();

        CrawlHistory saved = crawlHistoryRepository.save(history);
        log.warn("크롤링 부분 성공: {} - 발견: {}, 신규: {}, 오류: {}",
                target.getName(), articlesFound, articlesNew, errorMessage);
        return saved;
    }

    public Long countSuccessfulToday() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return crawlHistoryRepository.countByStatusAfter(CrawlTarget.CrawlStatus.SUCCESS, startOfDay);
    }

    public Long countFailedToday() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return crawlHistoryRepository.countByStatusAfter(CrawlTarget.CrawlStatus.FAILED, startOfDay);
    }

    @Transactional
    public long deleteOldHistory() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        long count = crawlHistoryRepository.countByExecutedAtBefore(oneMonthAgo);
        if (count > 0) {
            crawlHistoryRepository.deleteByExecutedAtBefore(oneMonthAgo);
            log.info("한달 이전 크롤링 이력 삭제: {}건", count);
        }
        return count;
    }
}
