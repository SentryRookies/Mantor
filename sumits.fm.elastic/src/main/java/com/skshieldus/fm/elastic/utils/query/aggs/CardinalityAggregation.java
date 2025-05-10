package com.skshieldus.fm.elastic.utils.query.aggs;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class CardinalityAggregation implements Aggregation{

    private String field;

    @Override
    public Map<String, Object> toMap() {
        return Map.of(
                "cardinality", Map.of(
                        "field", field
                )
        );
    }
}
