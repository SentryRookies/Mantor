package com.skshieldus.fm.elastic.utils.query.aggs;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class TermsAggregation implements Aggregation{

    private String field;

    @Builder.Default
    private int size = 10;

    @Override
    public Map<String, Object> toMap() {
        return Map.of(
                "terms", Map.of(
                        "field", field,
                        "size", size
                )
        );
    }
}
