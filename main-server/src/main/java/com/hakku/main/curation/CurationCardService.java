package com.hakku.main.curation;

import com.hakku.main.curation.domain.CurationCard;
import com.hakku.main.curation.exception.CurationCardNotFoundException;
import com.hakku.main.curation.repository.CurationCardRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 큐레이션 카드 CRUD. 조회는 공개(활성만), 생성/수정/삭제는 어드민 전용
 * (인가는 SecurityConfig 의 {@code /api/admin/**} → hasRole(ADMIN) 에서 강제).
 */
@Service
public class CurationCardService {

    private final CurationCardRepository repository;

    public CurationCardService(CurationCardRepository repository) {
        this.repository = repository;
    }

    /** 공개 캐러셀용 — 활성 카드만 정렬 순서대로. */
    @Transactional(readOnly = true)
    public List<CurationCardResponse> listActive() {
        return repository.findActiveOrdered().stream().map(CurationCardResponse::from).toList();
    }

    /** 어드민용 — 비활성 포함 전체 카드. */
    @Transactional(readOnly = true)
    public List<CurationCardResponse> listAll() {
        return repository.findAllOrdered().stream().map(CurationCardResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CurationCardResponse get(Long id) {
        return CurationCardResponse.from(findOrThrow(id));
    }

    @Transactional
    public CurationCardResponse create(CurationCardCommand cmd) {
        CurationCard saved = repository.save(new CurationCard(
                cmd.kicker(), cmd.title(), cmd.subtitle(), cmd.body(),
                cmd.imageUrl(), cmd.linkUrl(), cmd.displayOrder(), cmd.active()));
        return CurationCardResponse.from(saved);
    }

    @Transactional
    public CurationCardResponse update(Long id, CurationCardCommand cmd) {
        CurationCard card = findOrThrow(id);
        card.update(cmd.kicker(), cmd.title(), cmd.subtitle(), cmd.body(),
                cmd.imageUrl(), cmd.linkUrl(), cmd.displayOrder(), cmd.active());
        repository.save(card);
        return CurationCardResponse.from(card);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findOrThrow(id));
    }

    private CurationCard findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new CurationCardNotFoundException(id));
    }
}
