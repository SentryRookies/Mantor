package com.skshieldus.fm.elastic.repo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skshieldus.core.elastic.service.DataSearchElasticService;
import com.skshieldus.core.enums.HttpMethodEnum;
import com.skshieldus.core.utils.CommonUtil;
import com.skshieldus.domain.datasearch.HvacDataVO;
import com.skshieldus.fm.elastic.factory.ElasticSearchClientFactory;
import com.skshieldus.fm.elastic.utils.common.ElasticExecutor;
import com.skshieldus.fm.elastic.utils.factory.ElasticUtil;
import com.skshieldus.fm.elastic.utils.filterparser.FilterParserUtil;
import com.skshieldus.fm.elastic.utils.query.*;
import com.skshieldus.fm.elastic.utils.query.aggs.*;
import org.elasticsearch.client.RestClient;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class DataSearchElasticServiceImpl implements DataSearchElasticService {

    private final RestClient client;

    public DataSearchElasticServiceImpl(ElasticSearchClientFactory elasticSearchClientFactory) {
        this.client = elasticSearchClientFactory.getEsClient();
    }

    @Override
    public JsonNode getParkingDataAggs(String bldId, String startDate, String endDate, String datetimeType) throws Exception {
        String parkingIndex = ElasticUtil.generateIndexNames("parking_event_", startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

        LocalDateTime now = LocalDateTime.now();
        if (endDateTime.isAfter(now)) {
            endDateTime = now;
        }

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        Map<String, Object> queryMap = new HashMap<>();

        ElasticQuery filterQuery1 = new ElasticQuery("term", "buildingIdNew", bldId);
        ElasticQuery filterQuery2 = new ElasticQuery("terms", "eventName", List.of("입차", "출차"));
        RangeQuery rangeQuery = new RangeQuery("eventTime", startDateMilli, endDateMilli);

        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                Collections.emptyList(), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                Collections.emptyList(), // 빈 리스트(must_not)
                List.of(filterQuery1, filterQuery2, rangeQuery)  // filter
        );

        AggregationQuery aggsQuery = AggregationQuery.builder()
                .name("parking_date_histogram")
                .aggregation(
                        DateHistogramAggregation.builder()
                                .field("eventTime")
                                .calendarInterval(datetimeType.equals("Hour") ? "1h" : "1d")
                                .format(datetimeType.equals("Hour") ? "M/d HH:mm" : "yy M/d")
                                .extendedBoundsMin(startDateMilli)
                                .extendedBoundsMax(endDateMilli)
                                .build()
                )
                .subAggregation(
                        AggregationQuery.builder()
                                .name("parking_terms")
                                .aggregation(
                                        TermsAggregation.builder()
                                                .field("eventName")
                                                .size(10)
                                                .build()
                                )
                                .build()
                )
                .build();

        queryMap.put("query", Map.of("bool", boolQuery));
        queryMap.put("size", 0);
        queryMap.putAll(aggsQuery.toMap());

        ObjectMapper objectMapper = new ObjectMapper();
        return ElasticExecutor.searchAggregations(this.client, parkingIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap));
    }

    @Override
    public JsonNode selectCctvEventList(String bldId, String eventReason, String eventName, String searchText, String searchField, String startDate, String endDate, int pageSize, int pageIndex, int searchAfterIndex, List<List<String>> sortValue) throws Exception {
        //조회 인덱스 생성
        String cctvIndex = ElasticUtil.generateIndexNames("cctv_event_", startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

        LocalDateTime now = LocalDateTime.now();
        if (endDateTime.isAfter(now)) {
            endDateTime = now;
        }

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        // "query" 쿼리 맵 생성
        Map<String, Object> queryMap = new HashMap<>();

        // "wildcard" 쿼리 맵 생성
        Map<String, Object> wildCardMap = new HashMap<>();

        wildCardMap.put("value", "*" + searchText + "*");
        wildCardMap.put("case_insensitive", true); //lowerCase로 조회

        ElasticQuery filterQuery1 = null;
        ElasticQuery term1 = null;
        ElasticQuery term2 = null;

        if (eventReason != null && !eventReason.isEmpty()) {
            // eventReason 값이 존재하는 경우
            filterQuery1 = new ElasticQuery("term", "eventReason", eventReason);
            if(eventReason.equals("status")) {
                term1 = new ElasticQuery("term", "eventReason", "receive");
                term2 = new ElasticQuery("term", "eventCategory", "roi_event");
            }
        } else {
            // eventReason 값이 없으면
            filterQuery1 = new ElasticQuery("terms", "eventReason", List.of("healthcheck_1", "receive_emergency", "status"));
            term1 = new ElasticQuery("term", "eventReason", "receive");
            term2 = new ElasticQuery("term", "eventCategory", "roi_event");
        }
        //ElasticQuery filterQuery4 = new ElasticQuery("term", "eventName", eventName);

        ElasticQuery filterQuery2 = new ElasticQuery("wildcard", searchField, wildCardMap);
        ElasticQuery filterQuery3 = new ElasticQuery("term", "buildingIdNew", bldId);
        SortQuery sortQuery1 = new SortQuery("collectorReceiptTime", "desc");
        RangeQuery rangeQuery = new RangeQuery("collectorReceiptTime", startDateMilli, endDateMilli);

        BoolQuery boolQuery;
        if(searchText != null && !searchText.isEmpty()) {
            boolQuery = BoolQuery.createWithMatchQueries(
                    Collections.emptyList(), // 빈 리스트(must)
                    Collections.emptyList(), // 빈 리스트(should)
                    Collections.emptyList(), // 빈 리스트(must_not)
                    List.of(filterQuery1, filterQuery2, filterQuery3, rangeQuery)  // filter
            );
        } else {
            boolQuery = BoolQuery.createWithMatchQueries(
                    Collections.emptyList(), // 빈 리스트(must)
                    Collections.emptyList(), // 빈 리스트(should)
                    Collections.emptyList(), // 빈 리스트(must_not)
                    List.of(filterQuery1, filterQuery3, rangeQuery)  // filter
            );
        }

        //queryMap.put("from", PageStartIndex);
        queryMap.put("size", pageSize);
        queryMap.put("track_total_hits", true); // 총 갯수 조회
        queryMap.put("sort", sortQuery1.toMap());

        if(term1!=null && term2!=null) {
            BoolQuery boolQuery2 = BoolQuery.createWithMatchQueries(
                    List.of(term1, term2), // 빈 리스트(must)
                    Collections.emptyList(), // 빈 리스트(should)
                    Collections.emptyList(), // 빈 리스트(must_not)
                    List.of(filterQuery3, rangeQuery)
            );
            Map<String, Object> boolQuery3 = new HashMap<>();
            boolQuery3.put("should", List.of(Map.of("bool", boolQuery), Map.of("bool", boolQuery2)));

            queryMap.put("query", Map.of("bool", boolQuery3));
        } else {
            queryMap.put("query", Map.of("bool", boolQuery));
        }

        AggregationQuery aggsQuery = AggregationQuery.builder()
                .name("cctv_status_count")
                .aggregation(
                        TermsAggregation.builder()
                                .field("eventName")
                                .build()
                )
                .build();

        queryMap.putAll(aggsQuery.toMap());

        ObjectMapper objectMapper = new ObjectMapper();
        String esBoolQuery = objectMapper.writeValueAsString(queryMap);

        int pageDifference = pageIndex - searchAfterIndex;
        if(pageIndex<=1){
            //첫 페이지 요청
            return ElasticExecutor.searchAfterPage(this.client, cctvIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap), 1);
        }
        else if(pageDifference > 0){
            //다음 페이지 요청
            int searchAfterCount = pageIndex - searchAfterIndex;
            return ElasticExecutor.searchAfterPage(this.client, cctvIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap), searchAfterCount, sortValue);
        }
        else {
            //이전 페이지 요청 or 같은 페이지 요청
            int searchBeforeCount = searchAfterIndex - pageIndex;
            return ElasticExecutor.searchBeforePage(this.client, cctvIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap), searchBeforeCount, sortValue);
        }
    }

    @Override
    public JsonNode selectCctvEventListData(String bldId, String eventReason, String searchText, String searchField, String startDate, String endDate) throws Exception {
        //조회 인덱스 생성
        String cctvIndex = ElasticUtil.generateIndexNames("cctv_event_", startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

        LocalDateTime now = LocalDateTime.now();
        if (endDateTime.isAfter(now)) {
            endDateTime = now;
        }

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        // "query" 쿼리 맵 생성
        Map<String, Object> queryMap = new HashMap<>();

        // "wildcard" 쿼리 맵 생성
        Map<String, Object> wildCardMap = new HashMap<>();

        wildCardMap.put("value", "*" + searchText + "*");
        wildCardMap.put("case_insensitive", true); //lowerCase로 조회

        ElasticQuery filterQuery1 = null;
        ElasticQuery term1 = null;
        ElasticQuery term2 = null;

        if (eventReason != null && !eventReason.isEmpty()) {
            // eventReason 값이 존재하는 경우
            filterQuery1 = new ElasticQuery("term", "eventReason", eventReason);
            if(eventReason.equals("status")) {
                term1 = new ElasticQuery("term", "eventReason", "receive");
                term2 = new ElasticQuery("term", "eventCategory", "roi_event");
            }
        } else {
            // eventReason 값이 없으면
            filterQuery1 = new ElasticQuery("terms", "eventReason", List.of("healthcheck_1", "receive_emergency", "status"));
            term1 = new ElasticQuery("term", "eventReason", "receive");
            term2 = new ElasticQuery("term", "eventCategory", "roi_event");
        }

        ElasticQuery filterQuery2 = new ElasticQuery("wildcard", searchField, wildCardMap);
        ElasticQuery filterQuery3 = new ElasticQuery("term", "buildingIdNew", bldId);
        SortQuery sortQuery1 = new SortQuery("collectorReceiptTime", "desc");
        RangeQuery rangeQuery = new RangeQuery("collectorReceiptTime", startDateMilli, endDateMilli);

        BoolQuery boolQuery;

        if(searchText != null && !searchText.isEmpty()) {
            boolQuery = BoolQuery.createWithMatchQueries(
                    Collections.emptyList(), // 빈 리스트(must)
                    Collections.emptyList(), // 빈 리스트(should)
                    Collections.emptyList(), // 빈 리스트(must_not)
                    List.of(filterQuery1, filterQuery2, filterQuery3, rangeQuery)  // filter
            );
        } else {
            boolQuery = BoolQuery.createWithMatchQueries(
                    Collections.emptyList(), // 빈 리스트(must)
                    Collections.emptyList(), // 빈 리스트(should)
                    Collections.emptyList(), // 빈 리스트(must_not)
                    List.of(filterQuery1, filterQuery3, rangeQuery)  // filter
            );
        }

        queryMap.put("size", 10000);
        queryMap.put("track_total_hits", true); // 총 갯수 조회
        queryMap.put("sort", sortQuery1.toMap());

        if(term1!=null && term2!=null) {
            BoolQuery boolQuery2 = BoolQuery.createWithMatchQueries(
                    List.of(term1, term2), // 빈 리스트(must)
                    Collections.emptyList(), // 빈 리스트(should)
                    Collections.emptyList(), // 빈 리스트(must_not)
                    List.of(filterQuery3, rangeQuery)
            );
            Map<String, Object> boolQuery3 = new HashMap<>();
            boolQuery3.put("should", List.of(Map.of("bool", boolQuery), Map.of("bool", boolQuery2)));

            queryMap.put("query", Map.of("bool", boolQuery3));
        } else {
            queryMap.put("query", Map.of("bool", boolQuery));
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return ElasticExecutor.searchAllList(this.client, cctvIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap));
    }

    @Override
    public JsonNode selectCctvEventAgg(String bldId, String startDate, String endDate) throws Exception {
        //조회 인덱스 생성
        String cctvIndex = ElasticUtil.generateIndexNames("cctv_event_", startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

        LocalDateTime now = LocalDateTime.now();
        if (endDateTime.isAfter(now)) {
            endDateTime = now;
        }

        ElasticQuery filterQuery1 = new ElasticQuery("terms", "eventReason", List.of("healthcheck_1", "receive_emergency", "status"));
        ElasticQuery filterQuery2 = new ElasticQuery("term", "buildingIdNew", bldId);

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        // "query" 쿼리 맵 생성
        Map<String, Object> queryMap = new HashMap<>();

        SortQuery sortQuery1 = new SortQuery("collectorReceiptTime", "desc");
        RangeQuery rangeQuery = new RangeQuery("collectorReceiptTime", startDateMilli, endDateMilli);

        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                Collections.emptyList(), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                Collections.emptyList(), // 빈 리스트(must_not)
                List.of(filterQuery1, filterQuery2, rangeQuery)  // filter
        );

        ElasticQuery term1 = new ElasticQuery("term", "eventReason", "receive");
        ElasticQuery term2 = new ElasticQuery("term", "eventCategory", "roi_event");

        BoolQuery boolQuery2 = BoolQuery.createWithMatchQueries(
                List.of(term1, term2), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                Collections.emptyList(), // 빈 리스트(must_not)
                List.of(filterQuery2, rangeQuery)
        );
        Map<String, Object> boolQuery3 = new HashMap<>();
        boolQuery3.put("should", List.of(Map.of("bool", boolQuery), Map.of("bool", boolQuery2)));

        AggregationQuery aggsQuery = AggregationQuery.builder()
                .name("cctv_status_count")
                .aggregation(
                        TermsAggregation.builder()
                                .field("eventName")
                                .build()
                )
                .build();
        queryMap.put("track_total_hits", true); // 총 갯수 조회
        queryMap.put("sort", sortQuery1.toMap());
        queryMap.put("query", Map.of("bool", boolQuery3));
        queryMap.putAll(aggsQuery.toMap());

        ObjectMapper objectMapper = new ObjectMapper();
        return ElasticExecutor.searchAggregations(this.client, cctvIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap));
    }

    private String getAcaasUserIdRegExp(String bldId){
        String regExp = null;
        /*  사옥 별 출근 기록에 작성되는 userId 값의 형태가 존재. 그에 따라 각 빌딩 마다의 요청사항을 기입하여 쿼리에 적용
         *  공통조건 - 1. userId가 7자리인 것만 추린다.
         *           - 2. userId가 "1"로 시작하는 것만 추린다.
         * */
        switch (bldId) {
            case "B00000016":
                // SKT타워 : B00000016
                // userId가 "19"로 시작하는 것은 제외시킨다.
                regExp = "1[0-8][0-9]{5}";
                break;
            case "B00000029":
                // 판교사옥 : B00000029
                // userId가 "11", "15", "19", "107" 로 시작하는 것만 추린다.
                regExp = "(1(1|5|9)[0-9]{5}|107[0-9]{4})";
                break;
            case "B00000023": case "B00000030": case "B00000032":
                // 보라매: B00000023 , 분당: B00000030 , 성수: B00000032
                // userId가 "11"로 시작하는 것만 추린다.
                regExp = "11[0-9]{5}";
                break;
            case "B00000022": case "B00000048":
                // 둔산(구): B00000022 , 둔산(신): B00000048
                // userId가 "110", "150", "1070"로 시작하는 것만 추린다.
                regExp = "(1(10|50)[0-9]{4}|1070[0-9]{3})";
                break;
            case "B00000035":
                // 원주: B00000035
                // userId가 "110", "150"로 시작하는 것만 추린다.
                regExp = "(1(10|50)[0-9]{4})";
                break;
            default:
                //그외 사옥 - 공통조건만 들어간다.
                regExp = "1[0-9]{6}";
                break;
        }
        return regExp;
    }

    @Override
    public JsonNode getAcaasAccessCountAggs(String bldId, String startDate, String endDate, String datetimeType) throws Exception{
        String acaasIndex = ElasticUtil.generateIndexNames("acs_event_",startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");
        LocalDateTime now = LocalDateTime.now();
        if (endDateTime.isAfter(now)) {
            endDateTime = now;
        }
        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        Map<String, Object> queryMap = new HashMap<>();
        // filter
        List<DefaultQuery> filterQueryList = new ArrayList<>();
        filterQueryList.add(new ElasticQuery("term", "accessCode", "인증성공"));
        filterQueryList.add(new ElasticQuery("term", "userType", "임직원"));
        filterQueryList.add(new ElasticQuery("term", "buildingIdNew", bldId));
        filterQueryList.add(new RangeQuery("eventTime", startDateMilli, endDateMilli));

        // must_not
        ElasticQuery mustNotQuery = new ElasticQuery("term", "userName", "Unknown");

        // 제외되는 사용자 ID
        filterQueryList.add(new ElasticQuery("regexp", "userId", getAcaasUserIdRegExp(bldId)));

        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                Collections.emptyList(), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                List.of(mustNotQuery), // (must_not)
                filterQueryList  // (filter)
        );

        AggregationQuery aggsQuery = AggregationQuery.builder()
                .name("daily_events")
                .aggregation(
                        DateHistogramAggregation.builder()
                                .field("eventTime")
                                .calendarInterval("1d")
                                .format("M/d")
                                .extendedBoundsMin(startDateMilli)
                                .extendedBoundsMax(endDateMilli)
                                .build()
                )
                .subAggregation(
                        AggregationQuery.builder()
                                .name("unique_user_count")
                                .aggregation(
                                        CardinalityAggregation.builder()
                                            .field("userId")
                                            .build()
                                )
                                .build()
                )
                .build();

        queryMap.put("query", Map.of("bool", boolQuery));
        queryMap.put("size", 0);
        queryMap.putAll(aggsQuery.toMap());

        ObjectMapper objectMapper = new ObjectMapper();
        return ElasticExecutor.searchAggregations(this.client, acaasIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap));
    }

    @Override
    public JsonNode getAcaasAccessHistData(String bldId, String startDate, String endDate, int pageSize, int pageIndex, int searchAfterIndex, List<List<String>> sortValue, String userType, String enterAuthType, boolean accessSuccessStatus, boolean accessFailStatus, String searchColumn, String searchText) throws Exception{
        String acaasIndex = ElasticUtil.generateIndexNames("acs_event_",startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

        LocalDateTime now = LocalDateTime.now();
        if (endDateTime.isAfter(now)) {
            endDateTime = now;
        }

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        // filter
        List<DefaultQuery> filterQueryList = new ArrayList<>();
        filterQueryList.add(new ElasticQuery("term", "urlpath", "/event/access"));
        filterQueryList.add(new ElasticQuery("term", "buildingIdNew", bldId));
        filterQueryList.add(new RangeQuery("eventTime", startDateMilli, endDateMilli));

        // 사용자 구분
        if(!CommonUtil.isEmpty(userType)) {
            filterQueryList.add(new ElasticQuery("term", "userType", userType));
        }

        // 인증 구분
        if(!CommonUtil.isEmpty(enterAuthType)) {
            filterQueryList.add(new ElasticQuery("term", "accessType", enterAuthType));
        }

        // 검색 영역
        if(!CommonUtil.isEmpty(searchColumn) && !CommonUtil.isEmpty(searchText)) {
            // "wildcard" 쿼리 맵 생성
            Map<String, Object> wildCardMap = new HashMap<>();
            wildCardMap.put("value", "*" + searchText + "*");
            wildCardMap.put("case_insensitive", true); //lowerCase로 조회
            filterQueryList.add(new ElasticQuery("wildcard", searchColumn, wildCardMap));
        }

        // 인증성공, 실패
        List<String> accessCodeList = new ArrayList<>();
        if(accessSuccessStatus) {
            accessCodeList.add("인증성공");
        }
        if(accessFailStatus) {
            accessCodeList.add("실패");
        }
        filterQueryList.add(new ElasticQuery("terms", "accessCode", accessCodeList));

        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                Collections.emptyList(), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                Collections.emptyList(), // 빈 리스트(must_not)
                filterQueryList  // filter
        );
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");
        SortQuery sortQuery2 = new SortQuery("eventId", "asc");

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("size", pageSize);
        queryMap.put("track_total_hits", true); // 총 갯수 조회
        queryMap.put("sort", ElasticUtil.getSortQueries( List.of(sortQuery1, sortQuery2) ));
        queryMap.put("query", Map.of("bool", boolQuery));

        ObjectMapper objectMapper = new ObjectMapper();
        int pageDifference = pageIndex - searchAfterIndex;
        if(pageIndex<=1){
            //첫 페이지 요청
            return ElasticExecutor.searchAfterPage(this.client, acaasIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap), 1);
        }
        else if(pageDifference > 0){
            //다음 페이지 요청
            int searchAfterCount = pageIndex - searchAfterIndex;
            return ElasticExecutor.searchAfterPage(this.client, acaasIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap), searchAfterCount, sortValue);
        }
        else {
            //이전 페이지 요청 or 같은 페이지 요청
            int searchBeforeCount = searchAfterIndex - pageIndex;
            return ElasticExecutor.searchBeforePage(this.client, acaasIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap), searchBeforeCount, sortValue);
        }
    }

    @Override
    public <E> List<E> getAcaasAccessHistAllList(String bldId, String startDate, String endDate, String userType, String enterAuthType, boolean accessSuccessStatus, boolean accessFailStatus, String searchColumn, String searchText, Class<E> clazz) throws Exception{
        String acaasIndex = ElasticUtil.generateIndexNames("acs_event_",startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

        LocalDateTime now = LocalDateTime.now();
        if (endDateTime.isAfter(now)) {
            endDateTime = now;
        }

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        // filter
        List<DefaultQuery> filterQueryList = new ArrayList<>();
        filterQueryList.add(new ElasticQuery("term", "urlpath", "/event/access"));
        filterQueryList.add(new ElasticQuery("term", "buildingIdNew", bldId));
        filterQueryList.add(new RangeQuery("eventTime", startDateMilli, endDateMilli));

        // 사용자 구분
        if(!CommonUtil.isEmpty(userType)) {
            filterQueryList.add(new ElasticQuery("term", "userType", userType));
        }

        // 인증 구분
        if(!CommonUtil.isEmpty(enterAuthType)) {
            filterQueryList.add(new ElasticQuery("term", "accessType", enterAuthType));
        }

        // 검색 영역
        if(!CommonUtil.isEmpty(searchColumn) && !CommonUtil.isEmpty(searchText)) {
            // "wildcard" 쿼리 맵 생성
            Map<String, Object> wildCardMap = new HashMap<>();
            wildCardMap.put("value", "*" + searchText + "*");
            wildCardMap.put("case_insensitive", true); //lowerCase로 조회
            filterQueryList.add(new ElasticQuery("wildcard", searchColumn, wildCardMap));
        }

        // 인증성공, 실패
        List<String> accessCodeList = new ArrayList<>();
        if(accessSuccessStatus) {
            accessCodeList.add("인증성공");
        }
        if(accessFailStatus) {
            accessCodeList.add("실패");
        }
        filterQueryList.add(new ElasticQuery("terms", "accessCode", accessCodeList));


        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                Collections.emptyList(), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                Collections.emptyList(), // 빈 리스트(must_not)
                filterQueryList  // filter
        );
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");
        SortQuery sortQuery2 = new SortQuery("eventId", "asc");

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("size", 5000);
        queryMap.put("sort", ElasticUtil.getSortQueries( List.of(sortQuery1, sortQuery2) ));
        queryMap.put("query", Map.of("bool", boolQuery));

        ObjectMapper objectMapper = new ObjectMapper();

        return ElasticExecutor.searchAfterList(this.client, acaasIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap), clazz);
    }

    @Override
    public JsonNode getAcaasAlarmEventAggs(String bldId, String startDate, String endDate) throws Exception{
        String acaasIndex = ElasticUtil.generateIndexNames("acs_event_",startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

        LocalDateTime now = LocalDateTime.now();
        if (endDateTime.isAfter(now)) {
            endDateTime = now;
        }

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        Map<String, Object> queryMap = new HashMap<>();

        ElasticQuery filterQuery1 = new ElasticQuery("term", "urlpath", "/event/alarm");
        ElasticQuery filterQuery2 = new ElasticQuery("term", "buildingIdNew", bldId);
        RangeQuery rangeQuery = new RangeQuery("eventTime", startDateMilli, endDateMilli);

        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                Collections.emptyList(), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                Collections.emptyList(), // 빈 리스트(must_not)
                List.of(filterQuery1, filterQuery2, rangeQuery)  // filter
        );

        AggregationQuery aggsQuery = AggregationQuery.builder()
                .name("alarm_event_count")
                .aggregation(
                        TermsAggregation.builder()
                                .field("alarmCode")
                                .size(100)
                                .build()
                )
                .build();

        queryMap.put("query", Map.of("bool", boolQuery));
        queryMap.put("size", 0);
        queryMap.putAll(aggsQuery.toMap());

        ObjectMapper objectMapper = new ObjectMapper();
        return ElasticExecutor.searchAggregations(this.client, acaasIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap));
    }

    @Override
    public JsonNode getAcaasAlarmEventData(String bldId, String startDate, String endDate, int pageSize, int pageIndex, int searchAfterIndex, List<List<String>> sortValue, String alarmType, String searchColumn, String searchText) throws Exception{
        String acaasIndex = ElasticUtil.generateIndexNames("acs_event_",startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

        LocalDateTime now = LocalDateTime.now();
        if (endDateTime.isAfter(now)) {
            endDateTime = now;
        }

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        // filter
        List<DefaultQuery> filterQueryList = new ArrayList<>();
        filterQueryList.add(new ElasticQuery("term", "urlpath", "/event/alarm"));
        filterQueryList.add(new ElasticQuery("term", "buildingIdNew", bldId));
        filterQueryList.add(new RangeQuery("eventTime", startDateMilli, endDateMilli));

        // 알람 이벤트 종류
        if(!CommonUtil.isEmpty(alarmType)) {
            filterQueryList.add(new ElasticQuery("term", "alarmCode", alarmType));
        }

        // 검색 영역
        if(!CommonUtil.isEmpty(searchColumn) && !CommonUtil.isEmpty(searchText)) {
            // "wildcard" 쿼리 맵 생성
            Map<String, Object> wildCardMap = new HashMap<>();
            wildCardMap.put("value", "*" + searchText + "*");
            wildCardMap.put("case_insensitive", true); //lowerCase로 조회
            filterQueryList.add(new ElasticQuery("wildcard", searchColumn, wildCardMap));
        }

        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                Collections.emptyList(), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                Collections.emptyList(), // 빈 리스트(must_not)
                filterQueryList  // filter
        );
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");
        SortQuery sortQuery2 = new SortQuery("eventId", "asc");

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("size", pageSize);
        queryMap.put("track_total_hits", true); // 총 갯수 조회
        queryMap.put("sort", ElasticUtil.getSortQueries( List.of(sortQuery1, sortQuery2) ));
        queryMap.put("query", Map.of("bool", boolQuery));

        ObjectMapper objectMapper = new ObjectMapper();
        int pageDifference = pageIndex - searchAfterIndex;
        if(pageIndex<=1){
            //첫 페이지 요청
            return ElasticExecutor.searchAfterPage(this.client, acaasIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap), 1);
        }
        else if(pageDifference > 0){
            //다음 페이지 요청
            int searchAfterCount = pageIndex - searchAfterIndex;
            return ElasticExecutor.searchAfterPage(this.client, acaasIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap), searchAfterCount, sortValue);
        }
        else {
            //이전 페이지 요청 or 같은 페이지 요청
            int searchBeforeCount = searchAfterIndex - pageIndex;
            return ElasticExecutor.searchBeforePage(this.client, acaasIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap), searchBeforeCount, sortValue);
        }
    }

    @Override
    public <E> List<E> getAcaasAlarmEventAllList(String bldId, String startDate, String endDate, String alarmType, String searchColumn, String searchText, Class<E> clazz) throws Exception{
        String acaasIndex = ElasticUtil.generateIndexNames("acs_event_",startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

        LocalDateTime now = LocalDateTime.now();
        if (endDateTime.isAfter(now)) {
            endDateTime = now;
        }

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        // filter
        List<DefaultQuery> filterQueryList = new ArrayList<>();
        filterQueryList.add(new ElasticQuery("term", "urlpath", "/event/alarm"));
        filterQueryList.add(new ElasticQuery("term", "buildingIdNew", bldId));
        filterQueryList.add(new RangeQuery("eventTime", startDateMilli, endDateMilli));

        // 알람 이벤트 종류
        if(!CommonUtil.isEmpty(alarmType)) {
            filterQueryList.add(new ElasticQuery("term", "alarmCode", alarmType));
        }

        // 검색 영역
        if(!CommonUtil.isEmpty(searchColumn) && !CommonUtil.isEmpty(searchText)) {
            // "wildcard" 쿼리 맵 생성
            Map<String, Object> wildCardMap = new HashMap<>();
            wildCardMap.put("value", "*" + searchText + "*");
            wildCardMap.put("case_insensitive", true); //lowerCase로 조회
            filterQueryList.add(new ElasticQuery("wildcard", searchColumn, wildCardMap));
        }

        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                Collections.emptyList(), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                Collections.emptyList(), // 빈 리스트(must_not)
                filterQueryList  // filter
        );
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");
        SortQuery sortQuery2 = new SortQuery("eventId", "asc");

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("sort", ElasticUtil.getSortQueries( List.of(sortQuery1, sortQuery2) ));
        queryMap.put("query", Map.of("bool", boolQuery));

        ObjectMapper objectMapper = new ObjectMapper();
        return ElasticExecutor.searchAfterList(this.client, acaasIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap), clazz);
    }

    @Override
    public JsonNode getLogExportData(String reqIndex, String reqQuery, List<List<String>> sortValue) throws Exception {

        return ElasticExecutor.searchAfterPage(this.client, reqIndex, HttpMethodEnum.GET, reqQuery, 1, sortValue);
    }

    @Override
    public Map<String, Object> getAccessLogExportQuery(String bldId, String startDate, String endDate, String userType, String enterAuthType, boolean accessSuccessStatus, boolean accessFailStatus, String searchColumn, String searchText) throws Exception{
        String acaasIndex = ElasticUtil.generateIndexNames("acs_event_",startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

        LocalDateTime now = LocalDateTime.now();
        if (endDateTime.isAfter(now)) {
            endDateTime = now;
        }

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        // filter
        List<DefaultQuery> filterQueryList = new ArrayList<>();
        filterQueryList.add(new ElasticQuery("term", "urlpath", "/event/access"));
        filterQueryList.add(new ElasticQuery("term", "buildingIdNew", bldId));
        filterQueryList.add(new RangeQuery("eventTime", startDateMilli, endDateMilli));

        // 사용자 구분
        if(!CommonUtil.isEmpty(userType)) {
            filterQueryList.add(new ElasticQuery("term", "userType", userType));
        }

        // 인증 구분
        if(!CommonUtil.isEmpty(enterAuthType)) {
            filterQueryList.add(new ElasticQuery("term", "accessType", enterAuthType));
        }

        // 검색 영역
        if(!CommonUtil.isEmpty(searchColumn) && !CommonUtil.isEmpty(searchText)) {
            // "wildcard" 쿼리 맵 생성
            Map<String, Object> wildCardMap = new HashMap<>();
            wildCardMap.put("value", "*" + searchText + "*");
            wildCardMap.put("case_insensitive", true); //lowerCase로 조회
            filterQueryList.add(new ElasticQuery("wildcard", searchColumn, wildCardMap));
        }

        // 인증성공, 실패
        List<String> accessCodeList = new ArrayList<>();
        if(accessSuccessStatus) {
            accessCodeList.add("인증성공");
        }
        if(accessFailStatus) {
            accessCodeList.add("실패");
        }
        filterQueryList.add(new ElasticQuery("terms", "accessCode", accessCodeList));

        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                Collections.emptyList(), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                Collections.emptyList(), // 빈 리스트(must_not)
                filterQueryList  // filter
        );

        // sort
        List<SortQuery> sortQueryList = new ArrayList<>();
        sortQueryList.add(new SortQuery("eventTime", "desc"));
        sortQueryList.add(new SortQuery("eventId", "asc"));

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("size", 10000);
        queryMap.put("sort", sortQueryList.stream().map(SortQuery::toMap).collect(Collectors.toList()));
        queryMap.put("query", Map.of("bool", boolQuery));
        queryMap.put("index", acaasIndex);

        return queryMap;
    }

    @Override
    public Map<String, Object> getAccessAlarmExportQuery(String bldId, String startDate, String endDate, String alarmType, String searchColumn, String searchText) throws Exception{
        String acaasIndex = ElasticUtil.generateIndexNames("acs_event_",startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

        LocalDateTime now = LocalDateTime.now();
        if (endDateTime.isAfter(now)) {
            endDateTime = now;
        }

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        // filter
        List<DefaultQuery> filterQueryList = new ArrayList<>();
        filterQueryList.add(new ElasticQuery("term", "urlpath", "/event/alarm"));
        filterQueryList.add(new ElasticQuery("term", "buildingIdNew", bldId));
        filterQueryList.add(new RangeQuery("eventTime", startDateMilli, endDateMilli));

        // 알람 이벤트 종류
        if(!CommonUtil.isEmpty(alarmType)) {
            filterQueryList.add(new ElasticQuery("term", "alarmCode", alarmType));
        }

        // 검색 영역
        if(!CommonUtil.isEmpty(searchColumn) && !CommonUtil.isEmpty(searchText)) {
            // "wildcard" 쿼리 맵 생성
            Map<String, Object> wildCardMap = new HashMap<>();
            wildCardMap.put("value", "*" + searchText + "*");
            wildCardMap.put("case_insensitive", true); //lowerCase로 조회
            filterQueryList.add(new ElasticQuery("wildcard", searchColumn, wildCardMap));
        }

        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                Collections.emptyList(), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                Collections.emptyList(), // 빈 리스트(must_not)
                filterQueryList  // filter
        );
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");
        SortQuery sortQuery2 = new SortQuery("eventId", "asc");

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("size", 10000);
        queryMap.put("sort", ElasticUtil.getSortQueries( List.of(sortQuery1, sortQuery2) ));
        queryMap.put("query", Map.of("bool", boolQuery));
        queryMap.put("index", acaasIndex);

        return queryMap;
    }

    @Override
    public Map<String, Object> getCctvLogExportQuery(String bldId, String eventReason, String searchText, String searchField, String startDate, String endDate) throws Exception {
        //조회 인덱스 생성
        String cctvIndex = ElasticUtil.generateIndexNames("cctv_event_", startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

        LocalDateTime now = LocalDateTime.now();
        if (endDateTime.isAfter(now)) {
            endDateTime = now;
        }

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        // "query" 쿼리 맵 생성
        Map<String, Object> queryMap = new HashMap<>();

        // "wildcard" 쿼리 맵 생성
        Map<String, Object> wildCardMap = new HashMap<>();

        wildCardMap.put("value", "*" + searchText + "*");
        wildCardMap.put("case_insensitive", true); //lowerCase로 조회

        ElasticQuery filterQuery1 = null;
        ElasticQuery term1 = null;
        ElasticQuery term2 = null;

        if (eventReason != null && !eventReason.isEmpty()) {
            // eventReason 값이 존재하는 경우
            filterQuery1 = new ElasticQuery("term", "eventReason", eventReason);
            if(eventReason.equals("status")) {
                term1 = new ElasticQuery("term", "eventReason", "receive");
                term2 = new ElasticQuery("term", "eventCategory", "roi_event");
            }
        } else {
            // eventReason 값이 없으면
            filterQuery1 = new ElasticQuery("terms", "eventReason", List.of("healthcheck_1", "receive_emergency", "status"));
            term1 = new ElasticQuery("term", "eventReason", "receive");
            term2 = new ElasticQuery("term", "eventCategory", "roi_event");
        }

        ElasticQuery filterQuery2 = new ElasticQuery("wildcard", searchField, wildCardMap);
        ElasticQuery filterQuery3 = new ElasticQuery("term", "buildingIdNew", bldId);
        RangeQuery rangeQuery = new RangeQuery("collectorReceiptTime", startDateMilli, endDateMilli);

        BoolQuery boolQuery;

        if(searchText != null && !searchText.isEmpty()) {
            boolQuery = BoolQuery.createWithMatchQueries(
                    Collections.emptyList(), // 빈 리스트(must)
                    Collections.emptyList(), // 빈 리스트(should)
                    Collections.emptyList(), // 빈 리스트(must_not)
                    List.of(filterQuery1, filterQuery2, filterQuery3, rangeQuery)  // filter
            );
        } else {
            boolQuery = BoolQuery.createWithMatchQueries(
                    Collections.emptyList(), // 빈 리스트(must)
                    Collections.emptyList(), // 빈 리스트(should)
                    Collections.emptyList(), // 빈 리스트(must_not)
                    List.of(filterQuery1, filterQuery3, rangeQuery)  // filter
            );
        }


        // sort
        List<SortQuery> sortQueryList = new ArrayList<>();
        sortQueryList.add(new SortQuery("collectorReceiptTime", "desc"));
        sortQueryList.add(new SortQuery("eventId", "asc"));

        queryMap.put("size", 10000);
        queryMap.put("track_total_hits", true); // 총 갯수 조회
        queryMap.put("sort", sortQueryList.stream().map(SortQuery::toMap).collect(Collectors.toList()) );

        if(term1!=null && term2!=null) {
            BoolQuery boolQuery2 = BoolQuery.createWithMatchQueries(
                    List.of(term1, term2), // 빈 리스트(must)
                    Collections.emptyList(), // 빈 리스트(should)
                    Collections.emptyList(), // 빈 리스트(must_not)
                    List.of(filterQuery3, rangeQuery)
            );
            Map<String, Object> boolQuery3 = new HashMap<>();
            boolQuery3.put("should", List.of(Map.of("bool", boolQuery), Map.of("bool", boolQuery2)));

            queryMap.put("query", Map.of("bool", boolQuery3));
        } else {
            queryMap.put("query", Map.of("bool", boolQuery));
        }
        queryMap.put("index", cctvIndex);

        //ObjectMapper objectMapper = new ObjectMapper();
        //return ElasticExecutor.searchAllList(this.client, cctvIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap));
        return queryMap;
    }

    @Override
    public Map<String, Object> getHvacLogExportQuery(String bldId, String startDate, String endDate, String field, String value, List<String> filterList) throws Exception {
        String hvacIndex = ElasticUtil.generateIndexNames("fm_event_", startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        Map<String, Object> queryMap = new HashMap<>();

        SourceQuery sourceQuery = SourceQuery.builder()
                .includes(
                        Arrays.stream(HvacDataVO.class.getDeclaredFields())
                                .map(Field::getName)
                                .collect(Collectors.toList())
                )
                .build();

        ElasticQuery filterQuery1 = new ElasticQuery("term", "urlpath", "/event/fm/status");
        ElasticQuery filterQuery2 = new ElasticQuery("term", "buildingIdNew", bldId);
        RangeQuery filterQuery3 = new RangeQuery("eventTime", startDateMilli, endDateMilli);

        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                Collections.emptyList(), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                Collections.emptyList(), // 빈 리스트(must_not)
                List.of(filterQuery1, filterQuery2, filterQuery3)  // filter
        );

        if (!value.isBlank()) {
            boolQuery.getFilter().add(Map.of("wildcard", Map.of(field, "*" + value + "*")));
        }

        if (!filterList.isEmpty()) {
            Map<String, Object> parsedQuery = FilterParserUtil.buildQuery(filterList);
            boolQuery.getFilter().add(parsedQuery);
        }

        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");
        SortQuery sortQuery2 = new SortQuery("eventId", "asc");

        queryMap.putAll(sourceQuery.toMap());
        queryMap.put("track_total_hits", true); // 총 갯수 조회
        queryMap.put("query", Map.of("bool", boolQuery));
        queryMap.put("size", 10000);
        queryMap.put("sort", ElasticUtil.getSortQueries(List.of(sortQuery1, sortQuery2)));
        queryMap.put("index", hvacIndex);

        return queryMap;
    }

    @Override
    public Map<String, List<String>> getAcaasBuildingInfo(String startDate, String endDate) throws Exception{
        String acaasIndex = ElasticUtil.generateIndexNames("acs_event_",startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

        LocalDateTime now = LocalDateTime.now();
        if (endDateTime.isAfter(now)) {
            endDateTime = now;
        }

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        Map<String, Object> queryMap = new HashMap<>();
        // filter
        ElasticQuery filterQuery = new ElasticQuery("term", "urlpath", "/event/access");
        RangeQuery rangeQuery = new RangeQuery("eventTime", startDateMilli, endDateMilli);

        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                Collections.emptyList(), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                Collections.emptyList(), // 빈 리스트(must_not)
                List.of(filterQuery, rangeQuery)  // filter
        );

        AggregationQuery aggsQuery = AggregationQuery.builder()
                .name("group_by_tenant")
                .aggregation(
                        TermsAggregation.builder()
                                .field("tenantId")
                                .size(1000)
                                .build()
                )
                .subAggregation(
                        AggregationQuery.builder()
                                .name("bld_list")
                                .aggregation(
                                        TermsAggregation.builder()
                                        .field("buildingIdNew")
                                        .size(1000)
                                        .build()
                                )
                                .build()
                )
                .build();

        queryMap.put("query", Map.of("bool", boolQuery));
        queryMap.put("size", 0);
        queryMap.putAll(aggsQuery.toMap());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode tenantEsData = ElasticExecutor.searchAggregations(this.client, acaasIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap));

        // Parsing
        Map<String, List<String>> parseMap = new HashMap<>();

        JsonNode bucketList = tenantEsData
                .path("group_by_tenant")
                .path("buckets");

        bucketList.forEach( headBucket -> {
            String tenantId = headBucket.path("key").asText();
            List<String> bldList = parseMap.computeIfAbsent(tenantId, k -> new ArrayList<>());

            JsonNode bldBucketList = headBucket.path("bld_list").path("buckets");
            bldBucketList.forEach( bldBucket -> {
                bldList.add(bldBucket.path("key").asText());
            });
        });

        return parseMap;
    }

    @Override
    public JsonNode getAcaasBuildingHourlyAttendance(String bldId, String startDate, String startHour, String endDate, String endHour) throws Exception{
        String acaasIndex = ElasticUtil.generateIndexNames("acs_event_",startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T"+startHour+":00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T"+endHour+":00:00");

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        Map<String, Object> queryMap = new HashMap<>();
        // filter
        List<DefaultQuery> filterQueryList = new ArrayList<>();
        filterQueryList.add(new ElasticQuery("term", "accessCode", "인증성공"));
        filterQueryList.add(new ElasticQuery("term", "userType", "임직원"));
        filterQueryList.add(new ElasticQuery("term", "buildingIdNew", bldId));
        filterQueryList.add(new RangeQuery("eventTime", startDateMilli, endDateMilli));
        // must_not
        ElasticQuery mustNotQuery = new ElasticQuery("term", "userName", "Unknown");

        // 제외되는 사용자 ID
        filterQueryList.add(new ElasticQuery("regexp", "userId", getAcaasUserIdRegExp(bldId)));

        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                Collections.emptyList(), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                List.of(mustNotQuery), // (must_not)
                filterQueryList  // (filter)
        );

        // aggQuery - compositeSourceTerms
        List< Map<String, Object> > compositeSourceTerms = List.of(
                Map.of("buildingIdNew",  Map.of("terms", Map.of("field", "buildingIdNew") ) ),
                Map.of("userId",  Map.of("terms", Map.of("field", "userId") ) )
        );
        // aggQuery - sourceQuery
        SourceQuery sourceQuery = SourceQuery.builder()
                .includes(
                        List.of("tenantId","buildingIdNew", "userType", "userId", "userName", "eventTime")
                )
                .build();
        // aggQuery - sortQuery
        SortQuery sortQuery1 = new SortQuery("eventTime", "asc");
        SortQuery sortQuery2 = new SortQuery("eventId", "asc");


        AggregationQuery aggsQuery = AggregationQuery.builder()
                .name("hour_events")
                .aggregation(
                        CompositeAggregation.builder()
                                .size(7000)
                                .sources(compositeSourceTerms)
                                .build()
                )
                .subAggregation(
                        AggregationQuery.builder()
                                .name("top_log")
                                .aggregation(
                                        TopHitsAggregation.builder()
                                                .size(1)
                                                .sourceQuery(sourceQuery)
                                                .sortQueryList(List.of(sortQuery1, sortQuery2))
                                                .build()
                                )
                                .build()
                )
                .build();

        queryMap.put("query", Map.of("bool", boolQuery));
        queryMap.put("size", 0);
        queryMap.putAll(aggsQuery.toMap());

        ObjectMapper objectMapper = new ObjectMapper();
        return ElasticExecutor.searchAggregations(this.client, acaasIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap));
    }


    @Override
    public JsonNode getAcaasBuildingFloorOccupancy(String bldId, String startDate, String startHour, String endDate, String endHour) throws Exception{
        String acaasIndex = ElasticUtil.generateIndexNames("acs_event_",startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T"+startHour+":00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T"+endHour+":00:00");

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        Map<String, Object> queryMap = new HashMap<>();
        // filter
        List<DefaultQuery> filterQueryList = new ArrayList<>();
        filterQueryList.add(new ElasticQuery("term", "accessCode", "인증성공"));
        filterQueryList.add(new ElasticQuery("term", "buildingIdNew", bldId));
        filterQueryList.add(new ElasticQuery("terms", "userType", List.of("임직원", "협력업체", "방문자")));
        filterQueryList.add(new RangeQuery("eventTime", startDateMilli, endDateMilli));
        // must_not
        ElasticQuery mustNotQuery = new ElasticQuery("term", "userName", "Unknown");

        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                Collections.emptyList(), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                List.of(mustNotQuery), // (must_not)
                filterQueryList  // (filter)
        );

        // aggQuery - compositeSourceTerms
        List< Map<String, Object> > compositeSourceTerms = List.of(
                Map.of("buildingIdNew",  Map.of("terms", Map.of("field", "buildingIdNew") ) ),
                Map.of("userId",  Map.of("terms", Map.of("field", "userId") ) )
        );
        // aggQuery - sourceQuery
        SourceQuery sourceQuery = SourceQuery.builder()
                .includes(
                        List.of("tenantId", "buildingIdNew", "userType", "userId", "userName", "locationFloor", "doorName", "consoleName", "eventTime")
                )
                .build();
        // aggQuery - sortQuery
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");
        SortQuery sortQuery2 = new SortQuery("eventId", "asc");

        AggregationQuery aggsQuery = AggregationQuery.builder()
                .name("hour_events")
                .aggregation(
                        CompositeAggregation.builder()
                                .size(7000)
                                .sources(compositeSourceTerms)
                                .build()
                )
                .subAggregation(
                        AggregationQuery.builder()
                                .name("top_log")
                                .aggregation(
                                        TopHitsAggregation.builder()
                                                .size(1)
                                                .sourceQuery(sourceQuery)
                                                .sortQueryList(List.of(sortQuery1, sortQuery2))
                                                .build()
                                )
                                .build()
                )
                .build();

        queryMap.put("query", Map.of("bool", boolQuery));
        queryMap.put("size", 0);
        queryMap.putAll(aggsQuery.toMap());

        ObjectMapper objectMapper = new ObjectMapper();
        return ElasticExecutor.searchAggregations(this.client, acaasIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap));
    }

    @Override
    public JsonNode getHvacDataList(String bldId, String startDate, String endDate, String field, String value, List<String> filterList, int pageSize, int pageIndex, int searchAfterIndex, List<List<String>> sortValue) throws Exception {
        String hvacIndex = ElasticUtil.generateIndexNames("fm_event_", startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        Map<String, Object> queryMap = new HashMap<>();

        SourceQuery sourceQuery = SourceQuery.builder()
                .includes(
                        Arrays.stream(HvacDataVO.class.getDeclaredFields())
                                .map(Field::getName)
                                .collect(Collectors.toList())
                )
                .build();

        ElasticQuery filterQuery1 = new ElasticQuery("term", "urlpath", "/event/fm/status");
        ElasticQuery filterQuery2 = new ElasticQuery("term", "buildingIdNew", bldId);
        RangeQuery filterQuery3 = new RangeQuery("eventTime", startDateMilli, endDateMilli);

        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                Collections.emptyList(), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                Collections.emptyList(), // 빈 리스트(must_not)
                List.of(filterQuery1, filterQuery2, filterQuery3)  // filter
        );

        // simple filter 값 있을 경우 wildcard 검색
        if (!value.isBlank()) {
            boolQuery.getFilter().add(Map.of("wildcard", Map.of(field, "*" + value + "*")));
        }

        // detail filter 값 있을 경우 필터 -> es query 변환
        if (!filterList.isEmpty()) {
            Map<String, Object> parsedQuery = FilterParserUtil.buildQuery(filterList);
            boolQuery.getFilter().add(parsedQuery);
        }

        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");
        SortQuery sortQuery2 = new SortQuery("eventId", "asc");

        queryMap.putAll(sourceQuery.toMap());
        queryMap.put("track_total_hits", true); // 총 갯수 조회
        queryMap.put("query", Map.of("bool", boolQuery));
        queryMap.put("size", pageSize);
        queryMap.put("sort", ElasticUtil.getSortQueries(List.of(sortQuery1, sortQuery2)));

        ObjectMapper objectMapper = new ObjectMapper();
        int pageDifference = pageIndex - searchAfterIndex;
        if(pageIndex <= 1){
            //첫 페이지 요청
            return ElasticExecutor.searchAfterPage(this.client, hvacIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap), 1);
        }
        else if(pageDifference > 0){
            //다음 페이지 요청
            int searchAfterCount = pageIndex - searchAfterIndex;
            return ElasticExecutor.searchAfterPage(this.client, hvacIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap), searchAfterCount, sortValue);
        }
        else {
            //이전 페이지 요청 or 같은 페이지 요청
            int searchBeforeCount = searchAfterIndex - pageIndex;
            return ElasticExecutor.searchBeforePage(this.client, hvacIndex, HttpMethodEnum.GET, objectMapper.writeValueAsString(queryMap), searchBeforeCount, sortValue);
        }
    }

    @Override
    public JsonNode getHvacFilterOptionList(String bldId, String startDate, String endDate, String field, String value) throws Exception {
        String hvacIndex = ElasticUtil.generateIndexNames("fm_event_", startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");

        long startDateMilli = startDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long endDateMilli = endDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        Map<String, Object> queryMap = new HashMap<>();

        ElasticQuery filterQuery1 = new ElasticQuery("term", "urlpath", "/event/fm/status");
        ElasticQuery filterQuery2 = new ElasticQuery("term", "buildingIdNew", bldId);
        ElasticQuery wildcardQuery = new ElasticQuery("wildcard", field, value + "*");
        RangeQuery rangeQuery = new RangeQuery("eventTime", startDateMilli, endDateMilli);

        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                Collections.emptyList(), // 빈 리스트(must)
                Collections.emptyList(), // 빈 리스트(should)
                Collections.emptyList(), // 빈 리스트(must_not)
                List.of(filterQuery1, filterQuery2, rangeQuery, wildcardQuery)  // filter
        );

        AggregationQuery aggsQuery = AggregationQuery.builder()
                .name("unique_values")
                .aggregation(
                        TermsAggregation.builder()
                                .field(field)
                                .size(10)
                                .build()
                )
                .build();

        queryMap.put("query", Map.of("bool", boolQuery));
        queryMap.put("size", 0);
        queryMap.putAll(aggsQuery.toMap());

        return ElasticExecutor.searchAggregations(this.client, hvacIndex, HttpMethodEnum.GET, new ObjectMapper().writeValueAsString(queryMap));
    }
}
