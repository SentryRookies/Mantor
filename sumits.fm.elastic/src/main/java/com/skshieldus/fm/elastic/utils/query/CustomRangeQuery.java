package com.skshieldus.fm.elastic.utils.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class CustomRangeQuery implements DefaultQuery {
    private String field;
    private Long gt;
    private Long gte;
    private Long lt;
    private Long lte;

    public CustomRangeQuery(String field) {
        this.field = field;
    }

    public CustomRangeQuery setGt(Long gt) {
        this.gt = gt;
        return this;
    }

    public CustomRangeQuery setGte(Long gte) {
        this.gte = gte;
        return this;
    }

    public CustomRangeQuery setLt(Long lt) {
        this.lt = lt;
        return this;
    }

    public CustomRangeQuery setLte(Long lte) {
        this.lte = lte;
        return this;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> rangeConditions = new HashMap<>();
        if (gt != null) {
            rangeConditions.put("gt", gt);
        }
        if (gte != null) {
            rangeConditions.put("gte", gte);
        }
        if (lt != null) {
            rangeConditions.put("lt", lt);
        }
        if (lte != null) {
            rangeConditions.put("lte", lte);
        }
        return Map.of(
                "range", Map.of(
                        field, rangeConditions
                )
        );
    }
}