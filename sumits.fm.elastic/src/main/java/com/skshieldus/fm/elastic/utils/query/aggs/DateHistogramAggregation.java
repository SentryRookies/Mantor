package com.skshieldus.fm.elastic.utils.query.aggs;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
public class DateHistogramAggregation implements Aggregation {

    private String field;

    @Builder.Default
    private String calendarInterval = "1d";

    @Builder.Default
    private String format = "yyyy-MM-dd";

    @Builder.Default
    private String timeZone = "Asia/Seoul";

    private Object extendedBoundsMin;
    private Object extendedBoundsMax;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> dateHistogram = new HashMap<>();

        dateHistogram.put("field", field);
        dateHistogram.put("calendar_interval", calendarInterval);
        dateHistogram.put("format", format);
        dateHistogram.put("time_zone", timeZone);

        if (extendedBoundsMax != null || extendedBoundsMin != null) {
            Map<String, Object> extendedBounds = new HashMap<>();

            if (extendedBoundsMin != null) {
                extendedBounds.put("min", extendedBoundsMin);
            }

            if (extendedBoundsMax != null) {
                extendedBounds.put("max", extendedBoundsMax);
            }

            dateHistogram.put("extended_bounds", extendedBounds);
        }

        return Map.of("date_histogram", dateHistogram);
    }
}
