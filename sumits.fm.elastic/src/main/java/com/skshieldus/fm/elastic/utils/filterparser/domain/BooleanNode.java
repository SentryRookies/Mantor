package com.skshieldus.fm.elastic.utils.filterparser.domain;

import com.skshieldus.fm.elastic.utils.filterparser.enums.FilterOperator;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class BooleanNode implements Node {

    private String operator;
    private Node left;
    private Node Right;

    @Override
    public Map<String, Object> toEsQuery() {
        List<Map<String, Object>> clauses = List.of(left.toEsQuery(), Right.toEsQuery());

        Map<String, Object> boolQuery = new HashMap<>();
        if (FilterOperator.AND.equals(operator)) {
            boolQuery.put("filter", clauses);
        } else if (FilterOperator.OR.equals(operator)) {
            boolQuery.put("should", clauses);
            boolQuery.put("minimum_should_match", 1);
        }

        return Map.of("bool", boolQuery);
    }
}
