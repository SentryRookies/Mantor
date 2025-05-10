package com.skshieldus.database.utils.hikari;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;

/**
 * create on 2023-04-17.
 * <p> HikariPoolConfig </p>
 * @author sjoh14
 * @version 1.0
 */
public class HikariPoolConfig extends UnpooledDataSourceFactory {
    public HikariPoolConfig() {
        this.dataSource = new HikariDataSource();
    }
}
