package com.skshieldus.fm.elastic.utils.query.aggs;

import com.skshieldus.fm.elastic.utils.factory.ElasticUtil;
import com.skshieldus.fm.elastic.utils.query.SortQuery;
import com.skshieldus.fm.elastic.utils.query.SourceQuery;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class TopHitsAggregation implements Aggregation{

    @Builder.Default
    private int size = 1;   // 검색할 문서 개수 (기본값: 1)

    private SourceQuery sourceQuery;    // SourceQuery를 사용하여 includes 및 excludes 설정
    private List<SortQuery> sortQueryList;  // SortQuery를 사용하여 정렬 정보 설정

    private Integer from;   // 시작 인덱스 (페이징 지원)
    private Boolean trackScores;    // 검색 점수 반환 여부

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> topHits = new HashMap<>();

        topHits.put("size", size);

        // _source 필드에서 반환할 필드와 제외할 필드 설정
        if (sourceQuery != null) {
            topHits.putAll(sourceQuery.toMap());
        }

        // SortQuery를 사용하여 정렬 설정
        if (sortQueryList != null && !sortQueryList.isEmpty()) {
            topHits.put("sort", ElasticUtil.getSortQueries(sortQueryList));
        }

        // 페이징을 위한 from 값 설정
        if (from != null) {
            topHits.put("from", from);
        }

        // 검색 점수 반환 여부 설정
        if (trackScores != null) {
            topHits.put("track_scores", trackScores);
        }

        return Map.of("top_hits", topHits);
    }
}
