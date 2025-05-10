package com.skshieldus.database.jdbc.factory;

import com.skshieldus.database.utils.factory.DataSourceFactoryConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@Getter
@EnableTransactionManagement
public class DataSourceFactory extends DataSourceFactoryConfig {
    public DataSourceFactory(String pathInfo) {
        super(pathInfo);
    }
}
