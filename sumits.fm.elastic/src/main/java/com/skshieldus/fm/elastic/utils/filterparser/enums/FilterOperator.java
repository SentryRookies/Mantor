package com.skshieldus.fm.elastic.utils.filterparser.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum FilterOperator {

    LEFT_PARENTHESIS("["),
    RIGHT_PARENTHESIS("]"),

    EQUALS("eq"),
    NOT_EQUALS("neq"),
    GREATER_THAN("gt"),
    GREATER_THAN_EQUALS("gte"),
    LESS_THAN("lt"),
    LESS_THAN_EQUALS("lte"),
    EXISTS("exists"),
    NOT_EXISTS("not_exists"),

    AND("AND"),
    OR("OR"),

    INVALID("");

    private final String operator;

    private static final Map<String, FilterOperator> OPERATOR_MAP = new HashMap<>();

    static {
        for (FilterOperator op : values()) {
            OPERATOR_MAP.put(op.operator, op);
        }
    }

    FilterOperator(String operator){
        this.operator = operator;
    }

    public boolean equals(String operator) {
        return this.operator.equals(operator);
    }

    public static FilterOperator fromString(String operator) {
        return OPERATOR_MAP.getOrDefault(operator, INVALID);
    }
}
