package com.skshieldus.fm.elastic.utils.query;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class SourceQuery implements DefaultQuery {

    private List<String> includes;
    private List<String> excludes;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        if (includes != null) {
            map.put("includes", includes);
        }

        if (excludes != null) {
            map.put("excludes", excludes);
        }

        return Map.of("_source", map);
    }
}
