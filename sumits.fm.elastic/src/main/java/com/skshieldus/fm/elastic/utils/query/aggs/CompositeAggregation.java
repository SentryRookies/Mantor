package com.skshieldus.fm.elastic.utils.query.aggs;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Getter
@Setter
@Builder
public class CompositeAggregation implements Aggregation{

    @Builder.Default
    private int size = 1000;                // Aggregation 결과의 최대 크기 설정
    private List< Map<String, Object> > sources; // 그룹핑에 사용할 필드 목록
    private boolean missingBucket;          // 값이 없는 필드 포함 여부

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> composite = new HashMap<>();
        composite.put("size", size);

        if (sources != null) {
            composite.put("sources", sources);
        }

        // 값이 없는 필드 포함 여부 설정
        if (missingBucket) {
            composite.put("missing_bucket", true);
        }

        return Map.of("composite", composite);
    }
}
