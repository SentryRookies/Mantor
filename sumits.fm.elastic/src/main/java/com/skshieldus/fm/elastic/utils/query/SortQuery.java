package com.skshieldus.fm.elastic.utils.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SortQuery {

    @JsonProperty("field")
    private String field;

    @JsonProperty("order")
    private String order;

    /**
     * SortQuery 객체를 Map으로 변환합니다.
     * @return 정렬 기준을 나타내는 Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> sortMap = new HashMap<>();
        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("order", order);
        sortMap.put(field, fieldMap);
        return sortMap;
    }
}