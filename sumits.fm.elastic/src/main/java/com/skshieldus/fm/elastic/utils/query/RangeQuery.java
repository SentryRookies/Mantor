package com.skshieldus.fm.elastic.utils.query;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class RangeQuery implements DefaultQuery{
    private String field;
    private Long gte;
    private Long lt;

    public Map<String, Object> toMap() {
        return Map.of(
                "range", Map.of(
                        field, Map.of(
                                "gte", gte,
                                "lt", lt
                        )
                )
        );
    }
}