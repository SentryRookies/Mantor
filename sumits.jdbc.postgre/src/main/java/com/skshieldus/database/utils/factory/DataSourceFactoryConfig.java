package com.skshieldus.database.utils.factory;

import com.skshieldus.utils.LogUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
@Slf4j
public class DataSourceFactoryConfig {
    private final SqlSessionFactory sqlFactory;
    private final String pathInfo;

    public DataSourceFactoryConfig(String pathInfo) {
        this.pathInfo = "sqlmap/mybatis-" + pathInfo + "-config.xml";
        this.sqlFactory = getSessionFactory();
    }
    public SqlSessionFactory getSessionFactory() {
        try {
            String profile = System.getProperty("spring.profiles.active");

            // 1) MyBatis Config 파일 가져오기
            InputStream inputStream = Resources.getResourceAsStream(pathInfo);

            // 2) MyBatis Config로 전달할 Propertie 사용할
            Properties properties = new Properties();

            String basePath = "properties/dbconn.properties";

            // 2-1) Local 환경 이외 (dev, stg, prod)일 경우에는 ConfigMap으로 설정한 Path에서 값을 읽어옴
            String resourcePath = profile.equals("loc") ? basePath: "file:///" + basePath;
            properties.put("resourcePath", resourcePath);

            // 3) SqlSessionFactory 생성
            return new SqlSessionFactoryBuilder()
                    .build(inputStream, properties);
        }catch (IOException var4) {
            log.error(LogUtils.getStackErr(var4));
        }
        return null;
    }
}
