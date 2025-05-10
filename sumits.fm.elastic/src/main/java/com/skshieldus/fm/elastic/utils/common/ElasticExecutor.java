package com.skshieldus.fm.elastic.utils.common;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skshieldus.core.enums.HttpMethodEnum;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public interface ElasticExecutor {
     static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    /**
     * Elasticsearch에서 전체 데이터를 페이지 단위로 조회합니다.
     * @param client RestClient 인스턴스
     * @param index 검색할 인덱스 이름
     * @param method HTTP 메소드
     * @param queryJson 검색 쿼리 JSON 문자열
     * @return 쿼리 데이터 사이즈 만큼
     */
    static <E> E search(RestClient client, String index, HttpMethodEnum method, String queryJson, Class<E> clazz) {
        try {
            Request request = new Request(method.getMethod(), "/" + index + "/_search");
            request.addParameter("pretty", "true");
            request.setJsonEntity(queryJson);

            // 요청 실행
            Response response = client.performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            return  parseSingleResult(responseBody, clazz);
        }catch (Exception e){
            System.out.println("ES ERROR : " + e.getMessage());
            return null;
        }
    }

    /**
     * Elasticsearch에서 전체 데이터를 페이지 단위로 조회합니다.
     * @param client RestClient 인스턴스
     * @param index 검색할 인덱스 이름
     * @param method HTTP 메소드
     * @param queryJson 검색 쿼리 JSON 문자열
     * @return 쿼리 데이터 사이즈 만큼
     */
    static <E> List<E> searchList(RestClient client, String index, HttpMethodEnum method, String queryJson, Class<E> clazz) {
        List<E> results = new ArrayList<>();
        try {
                Request request = new Request(method.getMethod(), "/" + index + "/_search");
                request.addParameter("pretty", "true");
                request.setJsonEntity(queryJson);

                // 요청 실행
                Response response = client.performRequest(request);
                String responseBody = EntityUtils.toString(response.getEntity());

                // 응답에서 검색 결과 추가
                List<E> pageResults = parseResults(responseBody, clazz);
                results.addAll(pageResults);

        }catch (Exception e){
            System.out.println("ES ERROR : " + e.getMessage());
        }
        return (List<E>) results;
    }

    /**
     * Elasticsearch에서 전체 데이터를 페이지 단위로 조회합니다.
     * @param client RestClient 인스턴스
     * @param index 검색할 인덱스 이름
     * @param method HTTP 메소드
     * @param queryJson 검색 쿼리 JSON 문자열
     * @return 전체 데이터 목록
     */
    static <E> List<E> searchAfterList(RestClient client, String index, HttpMethodEnum method, String queryJson, Class<E> clazz) {
        List<E> results = new ArrayList<>();
        List<String> searchAfter = null;
        try {
            while (true) {
                Request request = new Request(method.getMethod(), "/" + index + "/_search");
                request.addParameter("pretty", "true");
                request.addParameter("ignore_unavailable", "true");
                request.setJsonEntity(buildQueryWithSearchAfter(queryJson, searchAfter));

                // 요청 실행
                Response response = client.performRequest(request);
                String responseBody = EntityUtils.toString(response.getEntity());

                // 응답에서 검색 결과 추가
                List<E> pageResults = parseResults(responseBody, clazz);
                results.addAll(pageResults);

                // 마지막 결과에서 다음 페이지를 위한 sort 값을 추출
                searchAfter = getLastSortValue(responseBody);

                // 더 이상 결과가 없으면 종료
                if (searchAfter == null) {
                    break;
                }
            }
        }catch (Exception e){
            System.out.println("ES ERROR : " + e.getMessage());
        }
        return (List<E>) results;
    }

    /**
     * Elasticsearch에서 정렬 값 기준으로 조회할 페이지를 가져옵니다.
     * @param client Elasticsearch와 통신하기 위한 RestClient 인스턴스
     * @param index 검색 대상 Elasticsearch 인덱스 이름
     * @param method HTTP 요청 메소드 (GET, POST 등)
     * @param queryJson 검색 조건을 포함한 쿼리 JSON 문자열
     * @param sortValue  이전 페이지의 마지막 정렬 값 (search_after용). 첫 번째 페이지에서는 null로 전달 가능
     * @param searchAfterCount  페이지 searchAfter 동작 횟수
     * @return  조회된 페이지 데이터 목록
     */
    static JsonNode searchAfterPage(RestClient client, String index, HttpMethodEnum method, String queryJson, int searchAfterCount, List< List<String> > sortValue) {
        JsonNode result = null;
        List<String> searchAfterValue = (sortValue==null || sortValue.isEmpty()) ? null : sortValue.get(1); // lastValue
        try {
            String responseBody = null;
            while (searchAfterCount --> 0) {
                // 요청 생성
                Request request = new Request(method.getMethod(), "/" + index + "/_search");
                request.addParameter("pretty", "true");
                request.addParameter("ignore_unavailable", "true");
                request.setJsonEntity(buildQueryWithSearchAfter(queryJson, searchAfterValue));

                // 요청 실행
                Response response = client.performRequest(request);
                responseBody = EntityUtils.toString(response.getEntity());

                // 마지막 결과에서 다음 페이지를 위한 sort 값을 추출
                searchAfterValue = getLastSortValue(responseBody);

                // 더 이상 결과가 없으면 종료
                if (searchAfterValue == null) {
                    break;
                }
            }
            // 응답 처리
            result = (responseBody==null) ? null : parseResult(responseBody);

        }catch (Exception e){
            System.out.println("ES ERROR : " + e.getMessage());
        }
        return result;
    }

    /**
     * Elasticsearch에서 처음 기준으로 조회할 페이지를 가져옵니다.
     * @param client  Elasticsearch와 통신하기 위한 RestClient 인스턴스
     * @param index   검색 대상 Elasticsearch 인덱스 이름
     * @param method HTTP 요청 메소드 (GET, POST 등)
     * @param queryJson 검색 조건을 포함한 쿼리 JSON 문자열
     * @param searchAfterCount  페이지 searchAfter 동작 횟수
     * @return  조회된 페이지 데이터 목록
     */
    static JsonNode searchAfterPage(RestClient client, String index, HttpMethodEnum method, String queryJson, int searchAfterCount) {
        // 첫 번째 페이지의 경우 sortValue를 null로 전달
        return searchAfterPage(client, index, method, queryJson, searchAfterCount, null);
    }

    /**
     * Elasticsearch에서 정렬 값 기준으로 이전 페이지를 가져옵니다.
     * @param client Elasticsearch와 통신하기 위한 RestClient 인스턴스
     * @param index 검색 대상 Elasticsearch 인덱스 이름
     * @param method HTTP 요청 메소드 (GET, POST 등)
     * @param queryJson 검색 조건을 포함한 쿼리 JSON 문자열
     * @param sortValue  이전 페이지의 마지막 정렬 값 (search_after용). 첫 번째 페이지에서는 null로 전달 가능
     * @param searchBeforeCount  페이지 searchAfter 동작 횟수
     * @return  조회된 페이지 데이터 목록
     */
    static JsonNode searchBeforePage(RestClient client, String index, HttpMethodEnum method, String queryJson, int searchBeforeCount, List< List<String> > sortValue) {
        JsonNode result = null;
        List<String> searchBeforeValue = (sortValue == null || sortValue.isEmpty()) ? null : sortValue.get(0);  // firstValue
        try {
            String responseBody = null;

            // 정렬 순서를 역순으로 변경 (asc -> desc, desc -> asc)
            String reversedQueryJson = modifySortOrder(queryJson);

            // N번의 searchAfter 실행 (역순으로 이동)
            List<String> firstValue = null;
            for (int i=0; i<searchBeforeCount; i++) { // N+1번 실행
                Request request = new Request(method.getMethod(), "/" + index + "/_search");
                request.addParameter("pretty", "true");
                request.addParameter("ignore_unavailable", "true");
                request.setJsonEntity(buildQueryWithSearchAfter(reversedQueryJson, searchBeforeValue));

                // 요청 실행
                Response response = client.performRequest(request);
                responseBody = EntityUtils.toString(response.getEntity());

                // 현재 페이지의 마지막 값을 추출하여 다음 searchAfter에 사용
                firstValue = getLastSortValue(responseBody);
                // 더 이상 결과가 없으면 종료
                if (firstValue == null) {
                    return null;
                }
                searchBeforeValue = firstValue;
            }

            if(responseBody!=null) {
                // JSON 파싱
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(responseBody);

                // hits.hits 배열 가져오기
                JsonNode hitsNode = rootNode.path("hits").path("hits");

                // _source 리스트를 추출 후 역순 정렬
                List<JsonNode> reversedHits = StreamSupport.stream(hitsNode.spliterator(), false)
                        .collect(Collectors.toList());
                Collections.reverse(reversedHits);

                // 새로운 ArrayNode 생성 및 역순 데이터 추가
                ArrayNode reversedArrayNode = objectMapper.createArrayNode();
                reversedArrayNode.addAll(reversedHits);

                // 기존 JSON 구조 유지하며 `hits.hits`만 교체
                ((ObjectNode) rootNode.path("hits")).set("hits", reversedArrayNode);

                responseBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            }

            // 최종 응답 처리
            result = (responseBody==null) ? null : parseResult(responseBody);
        } catch (Exception e) {
            System.out.println("ES ERROR : " + e.getMessage());
        }
        return result;
    }


    /**
     * Elasticsearch에서 집계 결과를 조회합니다.
     * @param client RestClient 인스턴스
     * @param index 검색할 인덱스 이름
     * @param method HTTP 메소드
     * @param queryJson 검색 쿼리 JSON 문자열
     * @return 집계 결과
     */
    static JsonNode searchAggregations(RestClient client, String index, HttpMethodEnum method, String queryJson) {
        try {
            Request request = new Request(method.getMethod(), "/" + index + "/_search?");
            request.addParameter("ignore_unavailable", "true");
            request.addParameter("pretty", "true");
            request.setJsonEntity(queryJson);

            // 요청 실행
            Response response = client.performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            // JSON 응답을 JsonNode로 변환
            return parseAggregations(responseBody);
        } catch (Exception e) {
            System.out.println("ES ERROR : " + e.getMessage());
            return null;
        }
    }

    /**
     * Elasticsearch에서 전체 결과를 조회하여 JsonNode로 반환
     * @param client RestClient 인스턴스
     * @param index 검색할 인덱스 이름
     * @param method HTTP 메소드
     * @param queryJson 검색 쿼리 JSON 문자열
     * @return 집계 결과
     */
    static JsonNode searchAllList(RestClient client, String index, HttpMethodEnum method, String queryJson) {
        try {
            Request request = new Request(method.getMethod(), "/" + index + "/_search?");
            request.addParameter("ignore_unavailable", "true");
            request.addParameter("pretty", "true");
            request.setJsonEntity(queryJson);

            // 요청 실행
            Response response = client.performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            // JSON 응답을 JsonNode로 변환
            JsonNode rootNode = objectMapper.readTree(responseBody);
            return rootNode;
        } catch (Exception e) {
            System.out.println("ES ERROR : " + e.getMessage());
            return null;
        }
    }

    /**
     * search_after 파라미터를 포함하여 쿼리 JSON을 생성합니다.
     *
     * @param queryJson   기본 쿼리 JSON
     * @param searchAfter 이전 페이지의 마지막 sort 값
     * @return search_after 파라미터가 포함된 쿼리 JSON
     * @throws Exception JSON 파싱 예외
     */
    private static String buildQueryWithSearchAfter(String queryJson, List<String> searchAfter) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        // 기존 queryJson을 JsonNode로 파싱
        JsonNode queryNode = objectMapper.readTree(queryJson);

        // search_after를 추가
        ObjectNode queryObject = (ObjectNode) queryNode;

        // searchAfterValues가 비어있지 않은 경우
        if (searchAfter != null && !searchAfter.isEmpty()) {
            ArrayNode searchAfterArray = objectMapper.createArrayNode();

            // 모든 search_after 값을 배열에 추가
            for (String value : searchAfter) {
                searchAfterArray.add(value);
            }

            // queryNode에 search_after 추가
            queryObject.set("search_after", searchAfterArray);
        }

        // 결과 JSON을 문자열로 변환하여 반환
        return objectMapper.writeValueAsString(queryObject);
    }

    /**
     * 응답 JSON에서 마지막 sort 값을 추출합니다.
     * @param responseBody 응답 JSON 문자열
     * @return 마지막 sort 값
     * @throws IOException JSON 파싱 오류
     */
    private static List<String> getLastSortValue(String responseBody) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode hitsNode = rootNode.path("hits").path("hits");

        if (hitsNode.isArray() && hitsNode.size() > 0) {
            JsonNode lastHitNode = hitsNode.get(hitsNode.size() - 1); // 마지막 결과
            JsonNode sortNode = lastHitNode.path("sort"); // sort 필드

            if (sortNode.isArray() && sortNode.size() > 0) {
                List<String> sortValues = new ArrayList<>();
                for (JsonNode sortValueNode : sortNode) {
                    sortValues.add(sortValueNode.asText());
                }
                return sortValues;  // 모든 sort 값을 리스트로 반환
            }
        }

        return null;
    }

    // 클라이언트 종료를 위한 별도 메소드
    private static void closeClient(RestClient client) {
        try {
            //client.close();
        } catch (Exception e) {
            System.out.println("클라이언트 종료 중 오류 발생");
        }
    }

    private static <E> List<E> parseResults(String responseBody, Class<E> clazz) throws Exception {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode hitsNode = rootNode.path("hits").path("hits");
        List<E> results = new ArrayList<>();

        for (JsonNode hitNode : hitsNode) {
            JsonNode sourceNode = hitNode.path("_source");
            E result = objectMapper.treeToValue(sourceNode, clazz);
            results.add(result);
        }

        return results;
    }

    public static <E> E parseSingleResult(String responseBody, Class<E> clazz) throws IOException {
        // JSON 응답을 JsonNode로 변환
        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode hitsNode = rootNode.path("hits").path("hits");

        // hitsNode가 빈 배열인지 확인
        if (hitsNode.isArray() && hitsNode.size() == 0) {
            return null; // 결과가 없으면 null 반환
        }

        // 첫 번째 결과 추출
        JsonNode firstHitNode = hitsNode.get(0).path("_source");
        // 첫 번째 결과를 지정된 클래스 타입으로 변환
        return objectMapper.treeToValue(firstHitNode, clazz);
    }

    private static JsonNode parseResult(String responseBody) throws IOException {
        // JSON 응답을 JsonNode로 변환
        JsonNode rootNode = objectMapper.readTree(responseBody);
        return rootNode.path("hits");
    }

    private static JsonNode parseAggregations(String responseBody) throws IOException {
        // JSON 응답을 JsonNode로 변환
        JsonNode rootNode = objectMapper.readTree(responseBody);
        return rootNode.path("aggregations");
    }

    private static String modifySortOrder(String queryJson) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(queryJson);

        // 가장 바깥쪽 'sort' 필드만 변경
        JsonNode sortNode = rootNode.path("sort");

        // 리스트 형태 처리
        if (sortNode.isArray() && sortNode.size() > 0) {
            for (JsonNode sortField : sortNode) {
                if (sortField.isObject()) {
                    ObjectNode fieldNode = (ObjectNode) sortField;
                    Iterator<String> fieldNames = fieldNode.fieldNames();

                    while (fieldNames.hasNext()) {
                        String fieldName = fieldNames.next();
                        JsonNode fieldValueNode = fieldNode.get(fieldName);

                        // 'order' 필드가 존재하면 asc <-> desc 변경
                        if (fieldValueNode.isObject() && fieldValueNode.has("order")) {
                            ObjectNode fieldValueObject = (ObjectNode) fieldValueNode;
                            String currentOrder = fieldValueObject.get("order").asText();
                            fieldValueObject.put("order", currentOrder.equalsIgnoreCase("asc") ? "desc" : "asc");
                        }
                    }
                }
            }
        }
        // 객체 형태의 처리
        else if(sortNode.isObject()) {
            ObjectNode sortObject = (ObjectNode) sortNode;
            Iterator<String> fieldNames = sortObject.fieldNames();

            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValueNode = sortObject.get(fieldName);

                // 'order' 필드가 존재하면 asc <-> desc 변경
                if (fieldValueNode.isObject() && fieldValueNode.has("order")) {
                    ObjectNode fieldValueObject = (ObjectNode) fieldValueNode;
                    String currentOrder = fieldValueObject.get("order").asText();
                    fieldValueObject.put("order", currentOrder.equalsIgnoreCase("asc") ? "desc" : "asc");
                }
            }
        }

        // JSON을 다시 문자열로 변환하여 반환
        return objectMapper.writeValueAsString(rootNode);
    }

}
