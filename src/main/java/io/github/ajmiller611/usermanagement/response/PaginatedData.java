package io.github.ajmiller611.usermanagement.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents paginated content as part of a larger API response.
 *
 * <p>This class is included as the data field within a {@link ResponseWrapper}.
 * It provides metadata about the pagination and the list of items for the current page.</p>
 *
 * @param <T> the type of the items contained in the paginated data.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaginatedData<T> {

  private List<T> data;
  private int currentPage;
  private int totalPages;
  private long totalItems;
}
