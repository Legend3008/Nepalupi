package np.com.nepalupi.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Section 15.5 + 15.7: Paginated API response with HATEOAS links.
 * <p>
 * Standard paginated response wrapper that includes:
 * <ul>
 *   <li>Page content (data)</li>
 *   <li>Pagination metadata (page number, size, total elements, total pages)</li>
 *   <li>HATEOAS links (self, first, last, next, prev)</li>
 *   <li>Sort information</li>
 * </ul>
 * 
 * @param <T> the content type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;
    private String sortBy;
    private String sortDirection;

    // HATEOAS links
    private Map<String, HateoasLink> _links;

    /**
     * Create a PagedResponse from Spring Data Page.
     */
    public static <T> PagedResponse<T> from(org.springframework.data.domain.Page<T> page,
                                              String baseUrl) {
        PagedResponse<T> response = new PagedResponse<>();
        response.setContent(page.getContent());
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setFirst(page.isFirst());
        response.setLast(page.isLast());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());

        if (page.getSort().isSorted()) {
            page.getSort().stream().findFirst().ifPresent(order -> {
                response.setSortBy(order.getProperty());
                response.setSortDirection(order.getDirection().name());
            });
        }

        // Build HATEOAS links
        response.set_links(buildLinks(page, baseUrl));

        return response;
    }

    private static <T> Map<String, HateoasLink> buildLinks(org.springframework.data.domain.Page<T> page,
                                                             String baseUrl) {
        java.util.HashMap<String, HateoasLink> links = new java.util.HashMap<>();

        links.put("self", new HateoasLink(
                String.format("%s?page=%d&size=%d", baseUrl, page.getNumber(), page.getSize()),
                "GET"));

        links.put("first", new HateoasLink(
                String.format("%s?page=0&size=%d", baseUrl, page.getSize()),
                "GET"));

        links.put("last", new HateoasLink(
                String.format("%s?page=%d&size=%d", baseUrl, Math.max(0, page.getTotalPages() - 1), page.getSize()),
                "GET"));

        if (page.hasNext()) {
            links.put("next", new HateoasLink(
                    String.format("%s?page=%d&size=%d", baseUrl, page.getNumber() + 1, page.getSize()),
                    "GET"));
        }

        if (page.hasPrevious()) {
            links.put("prev", new HateoasLink(
                    String.format("%s?page=%d&size=%d", baseUrl, page.getNumber() - 1, page.getSize()),
                    "GET"));
        }

        return links;
    }

    /**
     * HATEOAS link representation.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HateoasLink {
        private String href;
        private String method;
    }
}
