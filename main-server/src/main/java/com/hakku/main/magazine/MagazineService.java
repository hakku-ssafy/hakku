package com.hakku.main.magazine;

import com.hakku.main.magazine.domain.Magazine;
import com.hakku.main.magazine.exception.MagazineNotFoundException;
import com.hakku.main.magazine.repository.MagazineRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매거진 CRUD. 조회는 공개(발행분만), 발행/수정/삭제는 어드민 전용
 * (인가는 SecurityConfig 의 {@code /api/admin/**} → hasRole(ADMIN) 에서 강제).
 */
@Service
public class MagazineService {

    private final MagazineRepository repository;

    public MagazineService(MagazineRepository repository) {
        this.repository = repository;
    }

    /** 공개 캐러셀/목록용 — 발행된 매거진만 정렬 순서대로. */
    @Transactional(readOnly = true)
    public List<MagazineResponse> listPublished() {
        return repository.findPublishedOrdered().stream().map(MagazineResponse::from).toList();
    }

    /** 어드민용 — 미발행 포함 전체 매거진. */
    @Transactional(readOnly = true)
    public List<MagazineResponse> listAll() {
        return repository.findAllOrdered().stream().map(MagazineResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MagazineResponse get(Long id) {
        return MagazineResponse.from(findOrThrow(id));
    }

    @Transactional
    public MagazineResponse create(MagazineCommand cmd) {
        Magazine saved = repository.save(new Magazine(
                cmd.kicker(), cmd.title(), cmd.subtitle(), cmd.content(),
                cmd.coverImageUrl(), cmd.displayOrder(), cmd.published()));
        return MagazineResponse.from(saved);
    }

    @Transactional
    public MagazineResponse update(Long id, MagazineCommand cmd) {
        Magazine magazine = findOrThrow(id);
        magazine.update(cmd.kicker(), cmd.title(), cmd.subtitle(), cmd.content(),
                cmd.coverImageUrl(), cmd.displayOrder(), cmd.published());
        repository.save(magazine);
        return MagazineResponse.from(magazine);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findOrThrow(id));
    }

    private Magazine findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new MagazineNotFoundException(id));
    }
}
