package com.skshieldus.fm.elastic.utils.filterparser.domain;

import com.skshieldus.fm.elastic.utils.filterparser.enums.FilterOperator;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class FilterNode implements Node {

    private String field;
    private String fieldName;
    private String operator;
    private String operatorVal;
    private String value;

    @Override
    public Map<String, Object> toEsQuery() {

        FilterOperator op = FilterOperator.fromString(operator);
        return switch (op) {
            case EQUALS -> Map.of("term", Map.of(field, value));

            case NOT_EQUALS -> Map.of("bool", Map.of("must_not", Map.of("term", Map.of(field, value))));

            case GREATER_THAN, GREATER_THAN_EQUALS, LESS_THAN, LESS_THAN_EQUALS -> buildComparisonQuery();

            case EXISTS -> Map.of(
                    "bool", Map.of(
                            "filter", List.of(
                                    Map.of("exists", Map.of("field", field)),
                                    Map.of("bool", Map.of("must_not", Map.of("term", Map.of(field, ""))))
                            )
                    )
            );

            case NOT_EXISTS -> Map.of(
                    "bool", Map.of(
                            "should", List.of(
                                    // 필드 자체가 없는 경우
                                    Map.of("bool", Map.of("must_not", Map.of("exists", Map.of("field", field)))),
                                    // 필드가 존재하지만 값이 빈 문자열인 경우
                                    Map.of("term", Map.of(field, ""))
                            ),
                            "minimum_should_match", 1
                    )
            );

            default -> new HashMap<>();
        };
    }

    private Map<String, Object> buildComparisonQuery() {

        if ("presentVal".equals(field)) {
            return Map.of("script", Map.of(
                    "script", Map.of(
                            "source", getSource(),
                            "params", Map.of("compareValue", value),
                            "lang", "painless"
                    )
            ));
        }
        else {
            return Map.of("range", Map.of(field, Map.of(operator, value)));
        }
    }

    /*
    * presentVal이 keyword 타입이라 script로 비교해야 함
    * */
    private String getSource() {
        return "double fieldValue = 0.0; " +
                "double paramValue = 0.0;" +
                "if (doc['presentVal'].size() > 0) { " +
                "  String raw = doc['presentVal'].value; " +
                "  try { " +
                "    fieldValue = Double.parseDouble(raw); " +
                "    paramValue = Double.parseDouble(params.compareValue); " +
                "  } catch (Exception e) { " +
                "    fieldValue = 0.0; " +
                "    paramValue = 0.0; " +
                "  } " +
                "} " +
                "return fieldValue " + operatorVal + " paramValue;";
    }
}
