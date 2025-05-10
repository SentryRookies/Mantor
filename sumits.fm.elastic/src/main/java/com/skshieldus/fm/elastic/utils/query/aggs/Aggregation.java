package com.skshieldus.fm.elastic.utils.query.aggs;

import java.util.Map;

public interface Aggregation {

    Map<String, Object> toMap();
}
