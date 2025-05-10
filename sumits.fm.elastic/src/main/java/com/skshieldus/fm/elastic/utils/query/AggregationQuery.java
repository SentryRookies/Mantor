package com.skshieldus.fm.elastic.utils.query;

import com.skshieldus.fm.elastic.utils.query.aggs.Aggregation;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
public class AggregationQuery implements DefaultQuery {

    private String name;
    private Aggregation aggregation;
    private AggregationQuery subAggregation;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(aggregation.toMap());

        if (subAggregation != null) {
            map.putAll(subAggregation.toMap());
        }

        return Map.of("aggs", Map.of(name, map));
    }
}
