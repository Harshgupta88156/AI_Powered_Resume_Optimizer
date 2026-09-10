package com.ai_resume.dashboard_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Tolerant view of a Spring {@code Page} JSON body.
 *
 * <p>Spring has shipped two wire formats for pages: the classic flat
 * {@code PageImpl} shape ({@code totalElements} at the root) and the newer
 * {@code PagedModel} shape (the same fields nested under {@code "page"}). This
 * DTO accepts either, so an upstream Spring/Boot upgrade can't silently turn
 * every dashboard counter into 0.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagedResponse<T> {

    private List<T> content = new ArrayList<>();

    // Flat (PageImpl) form
    private Long totalElements;
    private Integer totalPages;
    private Integer number;
    private Integer size;

    // Nested (PagedModel) form
    private PageMetadata page;

    public List<T> getContent() {
        return content == null ? List.of() : content;
    }

    public long total() {
        if (totalElements != null) {
            return totalElements;
        }
        if (page != null && page.getTotalElements() != null) {
            return page.getTotalElements();
        }
        return getContent().size();
    }

    public int pages() {
        if (totalPages != null) {
            return totalPages;
        }
        if (page != null && page.getTotalPages() != null) {
            return page.getTotalPages();
        }
        return getContent().isEmpty() ? 0 : 1;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageMetadata {
        private Integer size;
        private Integer number;
        private Long totalElements;
        private Integer totalPages;
    }
}

