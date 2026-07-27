package event;

public record ReviewSummaryChangedEvent(
        String productId,
        double averageRating,
        long reviewCount
) {
}
