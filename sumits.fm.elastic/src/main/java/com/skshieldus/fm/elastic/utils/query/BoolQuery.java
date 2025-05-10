package com.skshieldus.fm.elastic.utils.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BoolQuery {
    @JsonProperty("must")
    private List<Map<String, Object>> must;

    @JsonProperty("should")
    private List<Map<String, Object>> should;

    @JsonProperty("must_not")
    private List<Map<String, Object>> mustNot;

    @JsonProperty("filter")
    private List<Map<String, Object>> filter;

    // 팩토리 메서드
    public static BoolQuery createWithMatchQueries(
            List<DefaultQuery> mustQueries,
            List<DefaultQuery> shouldQueries,
            List<DefaultQuery> mustNotQueries,
            List<DefaultQuery> filterQueries
    ) {
        BoolQuery boolQuery = new BoolQuery();
        boolQuery.setMust(mustQueries.stream().map(DefaultQuery::toMap).collect(Collectors.toList()));
        boolQuery.setShould(shouldQueries.stream().map(DefaultQuery::toMap).collect(Collectors.toList()));
        boolQuery.setMustNot(mustNotQueries.stream().map(DefaultQuery::toMap).collect(Collectors.toList()));
        boolQuery.setFilter(filterQueries.stream().map(DefaultQuery::toMap).collect(Collectors.toList()));
        return boolQuery;
    }

}
