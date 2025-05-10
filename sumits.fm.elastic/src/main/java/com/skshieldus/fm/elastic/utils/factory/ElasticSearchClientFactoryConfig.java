package com.skshieldus.fm.elastic.utils.factory;


import lombok.Getter;
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;


@Getter
public class ElasticSearchClientFactoryConfig {

    private final String pathInfo; //ES 설정파일 경로

    private final RestClient esClient; //ES 연결 클라이언트

    /**
     * ElasticSearch 설정파일 정보 셋팅
     */
    private List<String> elasticsearchHosts; // ES 노드 호스트 목록
    private int connectionTimeout; //연결 타임아웃 (밀리초 단위), 연결 시도 후 타임아웃까지 대기하는 시간
    private int socketTimeout; // 소켓 타임아웃 (밀리초 단위), 데이터 읽기 시 타임아웃까지 대기하는 시간
    private int maxConnTotal; // 전체 연결 수 최대값, 모든 호스트에 대한 최대 연결 수
    private int maxConnPerRoute; // 각 호스트 당 연결 수 최대값, 호스트 당 최대 연결 수

    public ElasticSearchClientFactoryConfig(String pathInfo) {
        this.pathInfo = "config/elastic-" + pathInfo + "-config.yml";
        this.loadConfigFromYaml();
        this.esClient = getRestClientFactory();
    }

    public RestClient getRestClientFactory() {
        RestClientBuilder builder = RestClient.builder(createHttpHosts())
                .setRequestConfigCallback(requestConfigBuilder ->
                        requestConfigBuilder
                                .setConnectTimeout(connectionTimeout)
                                .setSocketTimeout(socketTimeout))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder
                                .setDefaultIOReactorConfig(IOReactorConfig.custom()
                                        .setIoThreadCount(Runtime.getRuntime().availableProcessors())
                                        .build())
                                .setMaxConnTotal(maxConnTotal)
                                .setMaxConnPerRoute(maxConnPerRoute));
        return builder.build();
    }

    private void loadConfigFromYaml() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(this.pathInfo)) {
            if (inputStream == null) {
                throw new RuntimeException("Failed to load Elasticsearch : YAML 파일을 찾을 수 없습니다.");
            }
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);
            Map<String, Object> elasticsearchConfig = (Map<String, Object>) config.get("elasticsearch");
            elasticsearchHosts = (List<String>) elasticsearchConfig.get("hosts");
            connectionTimeout = (int) elasticsearchConfig.get("connectionTimeout");
            socketTimeout = (int) elasticsearchConfig.get("socketTimeout");
            maxConnTotal = (int) elasticsearchConfig.get("maxConnTotal");
            maxConnPerRoute = (int) elasticsearchConfig.get("maxConnPerRoute");
        } catch (Exception e) {
            throw new RuntimeException("Elasticsearch configuration 설정 파일이 정상 동작하지 않습니다.", e);
        }
    }

    private HttpHost[] createHttpHosts() {
        return elasticsearchHosts.stream()
                .map(host -> {
                    String[] parts = host.split(":");
                    return new HttpHost(parts[0], Integer.parseInt(parts[1]), "http");
                })
                .toArray(HttpHost[]::new);
    }

}
