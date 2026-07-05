package com.example.searchservice.dto.response;

import lombok.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T>  implements Serializable {
    private int currentPage;
    private int pageSize;
    private int totalPages;
    private long totalElements;

    @Builder.Default
    private List<T> content = Collections.emptyList();
}
