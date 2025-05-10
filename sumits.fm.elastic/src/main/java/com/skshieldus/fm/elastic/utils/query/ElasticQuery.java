package com.skshieldus.fm.elastic.utils.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class ElasticQuery implements DefaultQuery{
    private String type;
    private String field;
    private Object value;

    // Map으로 변환
    public Map<String, Object> toMap() {
        return Map.of(type, Map.of(field, value));
    }
}

