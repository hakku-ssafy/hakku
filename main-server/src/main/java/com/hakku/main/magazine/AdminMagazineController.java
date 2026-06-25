package com.hakku.main.magazine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 매거진 어드민 관리 API. 전체 경로가 {@code /api/admin/**} 이므로
 * SecurityConfig 에서 ADMIN 역할만 접근하도록 강제된다(백엔드 1차 방어선).
 */
@RestController
@RequestMapping("/api/admin/magazines")
public class AdminMagazineController {

    private final MagazineService magazineService;

    public AdminMagazineController(MagazineService magazineService) {
        this.magazineService = magazineService;
    }

    /** 미발행 포함 전체 매거진(정렬 순서대로). */
    @GetMapping
    public List<MagazineResponse> list() {
        return magazineService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MagazineResponse create(@Valid @RequestBody MagazineRequest request) {
        return magazineService.create(request.toCommand());
    }

    @PutMapping("/{id}")
    public MagazineResponse update(@PathVariable Long id,
                                   @Valid @RequestBody MagazineRequest request) {
        return magazineService.update(id, request.toCommand());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        magazineService.delete(id);
    }

    /** published 미지정 시 기본 발행(true). displayOrder 미지정 시 0. */
    public record MagazineRequest(
            String kicker,
            @NotBlank String title,
            String subtitle,
            String content,
            String coverImageUrl,
            int displayOrder,
            Boolean published) {

        MagazineCommand toCommand() {
            return new MagazineCommand(kicker, title, subtitle, content, coverImageUrl,
                    displayOrder, published == null || published);
        }
    }
}
