package com.skshieldus.fm.elastic.utils.filterparser.domain;

import java.util.Map;

public interface Node {
    Map<String, Object> toEsQuery();
}
