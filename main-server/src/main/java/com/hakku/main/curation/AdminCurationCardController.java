package com.hakku.main.curation;

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
 * 큐레이션 카드 어드민 관리 API. 전체 경로가 {@code /api/admin/**} 이므로
 * SecurityConfig 에서 ADMIN 역할만 접근하도록 강제된다(백엔드 1차 방어선).
 */
@RestController
@RequestMapping("/api/admin/curation-cards")
public class AdminCurationCardController {

    private final CurationCardService curationCardService;

    public AdminCurationCardController(CurationCardService curationCardService) {
        this.curationCardService = curationCardService;
    }

    /** 비활성 포함 전체 카드(정렬 순서대로). */
    @GetMapping
    public List<CurationCardResponse> list() {
        return curationCardService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CurationCardResponse create(@Valid @RequestBody CurationCardRequest request) {
        return curationCardService.create(request.toCommand());
    }

    @PutMapping("/{id}")
    public CurationCardResponse update(@PathVariable Long id,
                                       @Valid @RequestBody CurationCardRequest request) {
        return curationCardService.update(id, request.toCommand());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        curationCardService.delete(id);
    }

    /** active 미지정 시 기본 노출(true). displayOrder 미지정 시 0. */
    public record CurationCardRequest(
            String kicker,
            @NotBlank String title,
            String subtitle,
            String body,
            String imageUrl,
            String linkUrl,
            int displayOrder,
            Boolean active) {

        CurationCardCommand toCommand() {
            return new CurationCardCommand(kicker, title, subtitle, body, imageUrl, linkUrl,
                    displayOrder, active == null || active);
        }
    }
}
