package com.aiinsight.service;

import com.aiinsight.domain.crawl.CrawlTarget;
import com.aiinsight.domain.crawl.CrawlTargetRepository;
import com.aiinsight.dto.CrawlTargetDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CrawlTargetService {

    private final CrawlTargetRepository crawlTargetRepository;

    public List<CrawlTargetDto.Response> findAll() {
        return crawlTargetRepository.findAll().stream()
                .map(CrawlTargetDto.Response::from)
                .collect(Collectors.toList());
    }

    public Page<CrawlTargetDto.Response> findAll(Pageable pageable) {
        return crawlTargetRepository.findAll(pageable)
                .map(CrawlTargetDto.Response::from);
    }

    public CrawlTargetDto.Response findById(Long id) {
        CrawlTarget target = crawlTargetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("크롤링 대상을 찾을 수 없습니다: " + id));
        return CrawlTargetDto.Response.from(target);
    }

    public CrawlTarget findEntityById(Long id) {
        return crawlTargetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("크롤링 대상을 찾을 수 없습니다: " + id));
    }

    public List<CrawlTarget> findEnabledTargets() {
        return crawlTargetRepository.findByEnabledTrueOrderByNameAsc();
    }

    @Transactional
    public CrawlTargetDto.Response create(CrawlTargetDto.CreateRequest request) {
        if (crawlTargetRepository.existsByUrl(request.getUrl())) {
            throw new IllegalArgumentException("이미 등록된 URL입니다: " + request.getUrl());
        }

        CrawlTarget target = CrawlTarget.builder()
                .name(request.getName())
                .url(request.getUrl())
                .description(request.getDescription())
                .selectorConfig(request.getSelectorConfig())
                .cronExpression(request.getCronExpression())
                .crawlType(request.getCrawlType() != null ? request.getCrawlType() : CrawlTarget.CrawlType.STATIC)
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();

        CrawlTarget saved = crawlTargetRepository.save(target);
        log.info("크롤링 대상 생성: {} ({})", saved.getName(), saved.getId());
        return CrawlTargetDto.Response.from(saved);
    }

    @Transactional
    public CrawlTargetDto.Response update(Long id, CrawlTargetDto.UpdateRequest request) {
        CrawlTarget target = crawlTargetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("크롤링 대상을 찾을 수 없습니다: " + id));

        if (request.getUrl() != null && !request.getUrl().equals(target.getUrl())) {
            if (crawlTargetRepository.existsByUrlAndIdNot(request.getUrl(), id)) {
                throw new IllegalArgumentException("이미 등록된 URL입니다: " + request.getUrl());
            }
            target.setUrl(request.getUrl());
        }

        if (request.getName() != null) {
            target.setName(request.getName());
        }
        if (request.getDescription() != null) {
            target.setDescription(request.getDescription());
        }
        if (request.getSelectorConfig() != null) {
            target.setSelectorConfig(request.getSelectorConfig());
        }
        if (request.getCronExpression() != null) {
            target.setCronExpression(request.getCronExpression());
        }
        if (request.getCrawlType() != null) {
            target.setCrawlType(request.getCrawlType());
        }
        if (request.getEnabled() != null) {
            target.setEnabled(request.getEnabled());
        }

        CrawlTarget saved = crawlTargetRepository.save(target);
        log.info("크롤링 대상 수정: {} ({})", saved.getName(), saved.getId());
        return CrawlTargetDto.Response.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        CrawlTarget target = crawlTargetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("크롤링 대상을 찾을 수 없습니다: " + id));
        crawlTargetRepository.delete(target);
        log.info("크롤링 대상 삭제: {} ({})", target.getName(), target.getId());
    }

    @Transactional
    public CrawlTargetDto.Response toggleEnabled(Long id) {
        CrawlTarget target = crawlTargetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("크롤링 대상을 찾을 수 없습니다: " + id));
        target.setEnabled(!target.getEnabled());
        CrawlTarget saved = crawlTargetRepository.save(target);
        log.info("크롤링 대상 {} 상태 변경: {}", saved.getName(), saved.getEnabled() ? "활성화" : "비활성화");
        return CrawlTargetDto.Response.from(saved);
    }

    @Transactional
    public void updateLastCrawlStatus(Long id, CrawlTarget.CrawlStatus status) {
        CrawlTarget target = crawlTargetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("크롤링 대상을 찾을 수 없습니다: " + id));
        target.setLastStatus(status);
        target.setLastCrawledAt(java.time.LocalDateTime.now());
        crawlTargetRepository.save(target);
    }
}
