package com.skshieldus.fm.elastic.factory;

import com.skshieldus.fm.elastic.utils.factory.ElasticSearchClientFactoryConfig;
import lombok.Getter;



@Getter
public class ElasticSearchClientFactory extends ElasticSearchClientFactoryConfig {
    public ElasticSearchClientFactory(String pathInfo) {
        super(pathInfo);
    }
}
