package com.skshieldus.fm.elastic.repo;

import com.skshieldus.core.elastic.service.FmElasticService;
import com.skshieldus.core.enums.HttpMethodEnum;
import com.skshieldus.domain.fm.*;
import com.skshieldus.fm.elastic.factory.ElasticSearchClientFactory;
import com.skshieldus.fm.elastic.utils.common.ElasticExecutor;
import com.skshieldus.fm.elastic.utils.factory.ElasticUtil;
import com.skshieldus.fm.elastic.utils.query.*;
import org.elasticsearch.client.RestClient;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class FmElasticServiceImpl implements FmElasticService {
    private final RestClient client;

    public FmElasticServiceImpl(ElasticSearchClientFactory elasticSearchClientFactory) {
        this.client = elasticSearchClientFactory.getEsClient();
    }

    @Override
    public void selectElasticTest() throws Exception {
        selectOperationStopStatusList();
    }

    @Override
    public List<FmOperationStatusVO> selectOperationStopStatusList() throws Exception {
        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        //쿼리 선언
        ElasticQuery mustTermQueryMgmtSysName = new ElasticQuery("term", "mgmtSysName", "가용성");
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", "상태");

        ElasticQuery mustNotTermQueryTertiaryCategory = new ElasticQuery("term", "tertiaryCategory", "공조기");

        // 시간 범위 계산 현재(서버 시간대 기준 UTC)
        Instant[] timeRange = ElasticUtil.calculateTimeRangeUTC();
        Instant startTime = timeRange[0];
        Instant endTime = timeRange[1];

        RangeQuery rangeQueryEventTime = new RangeQuery("eventTime", startTime.toEpochMilli(), endTime.toEpochMilli());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryMonitoringItemName, mustTermQueryMgmtSysName), // must
                Collections.emptyList(),        // 빈 리스트(should)
                List.of(mustNotTermQueryTertiaryCategory),        // 빈 리스트(must_not)
                List.of(rangeQueryEventTime)     // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "asc");
        SortQuery sortQuery2 = new SortQuery("eventId", "asc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1, sortQuery2), 5);

        //Elastic Search 조회
        List<FmOperationStatusVO> operationStatusList = ElasticExecutor.searchAfterList(this.client, index, HttpMethodEnum.GET, queryJson, FmOperationStatusVO.class);
        operationStatusList.forEach(status -> status.setOperationCheckDatetime(Date.from(endTime)));

        return operationStatusList;
    }

    @Override
    public List<FmDateChartDataVO> getRefrigeratorElectric(AssetOperationStatusRecentVO assetOperationRecent) throws Exception {
        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        //쿼리 선언
        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "냉동기");
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", "전력");
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetOperationRecent.getAssetId());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryMonitoringItemName, mustTermQueryTertiaryCategoryName, mustTermQueryAssetId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                Collections.emptyList()    // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), 6);

        List<FmDateChartDataVO> refrigeratorElectricList = ElasticExecutor.searchList(this.client, index, HttpMethodEnum.GET, queryJson, FmDateChartDataVO.class);

        return refrigeratorElectricList;
    }

    @Override
    public List<FmDateChartDataVO> getCoolingTowerElectric(AssetOperationStatusRecentVO assetOperationRecent) throws Exception {
        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        //쿼리 선언
        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "냉각탑");
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", "전력");
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetOperationRecent.getAssetId());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryMonitoringItemName, mustTermQueryTertiaryCategoryName, mustTermQueryAssetId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                Collections.emptyList()    // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), 6);

        List<FmDateChartDataVO> refrigeratorElectricList = ElasticExecutor.searchList(this.client, index, HttpMethodEnum.GET, queryJson, FmDateChartDataVO.class);

        return refrigeratorElectricList;
    }

    @Override
    public FmDateChartDataVO getRefrigeratorElectricWattage(AssetOperationStatusRecentVO assetOperationRecent) throws Exception {

        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        //쿼리 선언
        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "냉동기");
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", "전력");
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetOperationRecent.getAssetId());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryMonitoringItemName, mustTermQueryTertiaryCategoryName, mustTermQueryAssetId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                Collections.emptyList()    // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), 1);

        FmDateChartDataVO refrigeratorElectricList = ElasticExecutor.search(this.client, index, HttpMethodEnum.GET, queryJson, FmDateChartDataVO.class);

        return refrigeratorElectricList;
    }

    /**
     * 냉동기 - 공급온도 조회
     */
    @Override
    public List<FmDateChartDataVO> getRefrigeratorColdWaterSupplyDataList(AssetOperationStatusRecentVO assetOperationRecent) throws Exception {
        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        //쿼리 선언
        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "냉동기");
        ElasticQuery mustTermQueryMgmtSysName = new ElasticQuery("term", "mgmtSysName", "냉수");
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", "공급온도");
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetOperationRecent.getAssetId());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryTertiaryCategoryName, mustTermQueryMgmtSysName, mustTermQueryMonitoringItemName, mustTermQueryAssetId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                Collections.emptyList()    // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), 6);

        List<FmDateChartDataVO> refrigeratorColdWaterSupplyDataList = ElasticExecutor.searchList(this.client, index, HttpMethodEnum.GET, queryJson, FmDateChartDataVO.class);
        return refrigeratorColdWaterSupplyDataList;
    }

    /**
     * 냉동기 - 환수온도 조회
     */
    @Override
    public List<FmDateChartDataVO> getRefrigeratorColdWaterReturnDataList(AssetOperationStatusRecentVO assetOperationRecent) throws Exception {
        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        //쿼리 선언
        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "냉동기");
        ElasticQuery mustTermQueryMgmtSysName = new ElasticQuery("term", "mgmtSysName", "냉수");
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", "환수온도");
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetOperationRecent.getAssetId());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryTertiaryCategoryName, mustTermQueryMgmtSysName, mustTermQueryMonitoringItemName, mustTermQueryAssetId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                Collections.emptyList()    // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), 6);

        List<FmDateChartDataVO> refrigeratorColdWaterReturnDataList = ElasticExecutor.searchList(this.client, index, HttpMethodEnum.GET, queryJson, FmDateChartDataVO.class);
        return refrigeratorColdWaterReturnDataList;
    }

    @Override
    public FmDateChartDataVO getRefrigeratorColdWatterDiffData(AssetOperationStatusRecentVO assetOperationRecent) throws Exception {

        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "냉동기");
        ElasticQuery mustTermQueryMgmtSysName = new ElasticQuery("term", "mgmtSysName", "냉수");
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", "온도차");
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetOperationRecent.getAssetId());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryTertiaryCategoryName, mustTermQueryMgmtSysName, mustTermQueryMonitoringItemName, mustTermQueryAssetId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                Collections.emptyList()    // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), 1);

        FmDateChartDataVO refrigeratorElectricList = ElasticExecutor.search(this.client, index, HttpMethodEnum.GET, queryJson, FmDateChartDataVO.class);

        return refrigeratorElectricList;
    }

    /**
     * 냉동기 - 냉각수 공급온도 조회
     */
    @Override
    public List<FmDateChartDataVO> getRefrigeratorCoolantWaterSupplyDataList(AssetOperationStatusRecentVO assetOperationRecent) throws Exception {
        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        //쿼리 선언
        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "냉동기");
        ElasticQuery mustTermQueryMgmtSysName = new ElasticQuery("term", "mgmtSysName", "냉각수");
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", "공급온도");
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetOperationRecent.getAssetId());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryTertiaryCategoryName, mustTermQueryMgmtSysName, mustTermQueryMonitoringItemName, mustTermQueryAssetId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                Collections.emptyList()    // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), 6);

        List<FmDateChartDataVO> refrigeratorCoolantWaterSupplyDataList = ElasticExecutor.searchList(this.client, index, HttpMethodEnum.GET, queryJson, FmDateChartDataVO.class);
        return refrigeratorCoolantWaterSupplyDataList;
    }

    /**
     * 냉동기 - 냉각수 환수온도 조회
     */
    @Override
    public List<FmDateChartDataVO> getRefrigeratorCoolantWaterReturnDataList(AssetOperationStatusRecentVO assetOperationRecent) throws Exception {
        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        //쿼리 선언
        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "냉동기");
        ElasticQuery mustTermQueryMgmtSysName = new ElasticQuery("term", "mgmtSysName", "냉각수");
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", "환수온도");
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetOperationRecent.getAssetId());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryTertiaryCategoryName, mustTermQueryMgmtSysName, mustTermQueryMonitoringItemName, mustTermQueryAssetId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                Collections.emptyList()    // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), 6);

        List<FmDateChartDataVO> refrigeratorCoolantWaterReturnDataList = ElasticExecutor.searchList(this.client, index, HttpMethodEnum.GET, queryJson, FmDateChartDataVO.class);
        return refrigeratorCoolantWaterReturnDataList;
    }

    /**
     * COP/유량 정보 조회
     */
    @Override
    public FmDateChartDataVO getRefrigeratorCopFlowRateData(AssetOperationStatusRecentVO assetOperationRecent , String monitoringItemNm) throws Exception {
        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "냉동기");
        ElasticQuery mustTermQueryMgmtSysName = new ElasticQuery("term", "mgmtSysName", "효율");
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", monitoringItemNm);
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetOperationRecent.getAssetId());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryTertiaryCategoryName, mustTermQueryMgmtSysName, mustTermQueryMonitoringItemName, mustTermQueryAssetId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                Collections.emptyList()    // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), 1);

        return ElasticExecutor.search(this.client, index, HttpMethodEnum.GET, queryJson, FmDateChartDataVO.class);
    }

    @Override
    public List<FmDateChartDataVO> getCoolingTowerCoolantWaterSupplyReturnDataList(AssetOperationStatusRecentVO assetOperationRecent, String monitoringItemNm) throws Exception {
        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        //쿼리 선언
        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "냉각탑");
        ElasticQuery mustTermQueryMgmtSysName = new ElasticQuery("term", "mgmtSysName", "냉각수");
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", monitoringItemNm);
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetOperationRecent.getAssetId());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryTertiaryCategoryName, mustTermQueryMgmtSysName, mustTermQueryMonitoringItemName, mustTermQueryAssetId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                Collections.emptyList()    // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), 6);


        return ElasticExecutor.searchList(this.client, index, HttpMethodEnum.GET, queryJson, FmDateChartDataVO.class);
    }

    @Override
    public FmDateChartDataVO getCoolingTowerInfoData(AssetOperationStatusRecentVO assetOperationRecent,String mgmtSysName, String monitoringItemName) throws Exception {

        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "냉각탑");
        ElasticQuery mustTermQueryMgmtSysName = new ElasticQuery("term", "mgmtSysName", "설정");
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", monitoringItemName);
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetOperationRecent.getAssetId());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryTertiaryCategoryName, mustTermQueryMgmtSysName, mustTermQueryMonitoringItemName, mustTermQueryAssetId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                Collections.emptyList()    // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), 1);
        System.out.println(queryJson);

        return ElasticExecutor.search(this.client, index, HttpMethodEnum.GET, queryJson, FmDateChartDataVO.class);
    }

    @Override
    public List<FmDateChartDataVO> getAirHandlerDegLineDataList(AssetOperationStatusRecentVO assetOperationRecent, String monitoringItemNm) throws Exception {
        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        //쿼리 선언
        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "공조기");
        ElasticQuery mustTermQueryMgmtSysName = new ElasticQuery("term", "mgmtSysName", "공기");
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", monitoringItemNm);
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetOperationRecent.getAssetId());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryTertiaryCategoryName, mustTermQueryMgmtSysName, mustTermQueryMonitoringItemName, mustTermQueryAssetId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                Collections.emptyList()    // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), 6);


        return ElasticExecutor.searchList(this.client, index, HttpMethodEnum.GET, queryJson, FmDateChartDataVO.class);
    }

    @Override
    public List<FmDateChartDataVO> getAirHandleHumidityDataList(AssetOperationStatusRecentVO assetOperationRecent) throws Exception {
        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        //쿼리 선언
        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "공조기");
        ElasticQuery mustTermQueryMgmtSysName = new ElasticQuery("term", "mgmtSysName", "공기");
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", "환기습도");
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetOperationRecent.getAssetId());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryTertiaryCategoryName, mustTermQueryMgmtSysName, mustTermQueryMonitoringItemName, mustTermQueryAssetId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                Collections.emptyList()    // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), 6);


        return ElasticExecutor.searchList(this.client, index, HttpMethodEnum.GET, queryJson, FmDateChartDataVO.class);
    }

    @Override
    public FmDateChartDataVO getAirHandlerControlData(AssetOperationStatusRecentVO assetOperationRecent, String mgmtSysName, String monitoringItemNm) throws Exception {
        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "공조기");
        ElasticQuery mustTermQueryMgmtSysName = new ElasticQuery("term", "mgmtSysName", mgmtSysName);
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", monitoringItemNm);
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetOperationRecent.getAssetId());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryTertiaryCategoryName, mustTermQueryMgmtSysName, mustTermQueryMonitoringItemName, mustTermQueryAssetId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                Collections.emptyList()    // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), 1);

        return ElasticExecutor.search(this.client, index, HttpMethodEnum.GET, queryJson, FmDateChartDataVO.class);
    }

    @Override
    public FmDateChartDataVO getBoilerData(AssetOperationStatusRecentVO assetOperationRecent, String mgmtSysName, String monitoringItemNm) throws Exception {
        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "보일러");
        ElasticQuery mustTermQueryMgmtSysName = new ElasticQuery("term", "mgmtSysName", mgmtSysName);
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", monitoringItemNm);
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetOperationRecent.getAssetId());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryTertiaryCategoryName, mustTermQueryMgmtSysName, mustTermQueryMonitoringItemName, mustTermQueryAssetId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                Collections.emptyList()    // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), 1);

        return ElasticExecutor.search(this.client, index, HttpMethodEnum.GET, queryJson, FmDateChartDataVO.class);
    }

    @Override
    public List<FmDateChartDataVO> getBoilerWaterDegSupplyReturnDataList(AssetOperationStatusRecentVO assetOperationRecent, String monitoringItemNm) throws Exception {
        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        //쿼리 선언
        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "보일러");
        ElasticQuery mustTermQueryMgmtSysName = new ElasticQuery("term", "mgmtSysName", "온도");
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", monitoringItemNm);
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetOperationRecent.getAssetId());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryTertiaryCategoryName, mustTermQueryMgmtSysName, mustTermQueryMonitoringItemName, mustTermQueryAssetId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                Collections.emptyList()    // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), 6);


        return ElasticExecutor.searchList(this.client, index, HttpMethodEnum.GET, queryJson, FmDateChartDataVO.class);
    }

    @Override
    public List<FmBuildingEnvironmentStatus> getBuildingEnvironmentStatusByBldId(String bldId) {
        // fm_event index
        String index = ElasticUtil.getIndexNames("fm_event");

        // BuildingId, EventName, EventReason Query 생성
        ElasticQuery matchPhraseQueryBuildingId = new ElasticQuery("match_phrase", "buildingIdNew", bldId);
        ElasticQuery matchPhraseQueryEventName = new ElasticQuery("match_phrase", "eventName", "general");
        ElasticQuery matchPhraseQueryEventReason = new ElasticQuery("match_phrase", "eventReason", "status_general");
        ElasticQuery existsQueryLocationFloor = new ElasticQuery("match_phrase", "locationFloor", "");
        ElasticQuery mustTermsQueryMonitoringItemName = new ElasticQuery(
                "terms",
                "monitoringItemName",
                List.of("외기댐퍼", "농도", "실내습도", "실내온도")
        );

        String utcTimeString = ZonedDateTime.of(LocalDate.now().atStartOfDay(), ZoneId.of("Asia/Seoul"))
                .withZoneSameInstant(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT);

        // range 쿼리 생성
        ElasticQuery rangeQueryCollectorReceiptTime = new ElasticQuery(
                "range",
                "collectorReceiptTime",
                Map.of(
                        "gte", utcTimeString, // 오늘 00시 00분 00초
                        "format", "strict_date_optional_time"
                )
        );

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermsQueryMonitoringItemName), // must 리스트
                Collections.emptyList(),  // should 리스트
                List.of(
                        existsQueryLocationFloor // locationFloor 필드가 존재하지 않는 데이터만 조회
                ),  // must_not 리스트
                // filter 리스트
                List.of(
                        rangeQueryCollectorReceiptTime, // 오늘 데이터만 조회
                        matchPhraseQueryBuildingId, // 건물 ID
                        matchPhraseQueryEventName, // 이벤트명
                        matchPhraseQueryEventReason // 이벤트 사유
                ));

        SortQuery sortQuery1 = new SortQuery("eventTime", "asc");
        SortQuery sortQuery2 = new SortQuery("eventId", "asc");

        //bool -> JSON 변환
        String queryJson;
        try {
            queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1, sortQuery2), 5000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Elastic Search 조회
        List<FmBuildingEnvironmentStatus> fmBuildingEnvironmentStatuses = ElasticExecutor.searchAfterList(this.client, index, HttpMethodEnum.GET, queryJson, FmBuildingEnvironmentStatus.class);
 
        // getPointAddress 기준으로 중복 제거하고 최신 eventTime의 항목만 남기기
        Map<String, FmBuildingEnvironmentStatus> latestItemsMap = fmBuildingEnvironmentStatuses.stream()
                .collect(Collectors.toMap(
                        FmBuildingEnvironmentStatus::getPointAddress, // getPointAddress 기준으로
                        item -> item, // 각 item을 값으로 저장
                        (item1, item2) -> Long.parseLong(item1.getEventTime()) > Long.parseLong(item2.getEventTime()) ? item1 : item2 // 최신 eventTime의 항목 선택
                ));

        // 중복 제거된 리스트 얻기
        return new ArrayList<>(latestItemsMap.values());
    }

    @Override
    public List<FmFacilityObjectOperationStatusVO> selectFacilityOperationStatusList() throws Exception {
        //UTC 기준 일자 맞춰서 인덱스명 가져옴
        String index = ElasticUtil.getIndexNames("fm_event");

        //쿼리 선언
        ElasticQuery mustTermQueryTertiaryCategoryName = new ElasticQuery("term", "tertiaryCategory", "공조기");
        ElasticQuery mustTermQueryMonitoringItemName = new ElasticQuery("term", "monitoringItemName", "상태");
        ElasticQuery mustTermsQueryMgmtSysName = new ElasticQuery(
                "terms",
                "mgmtSysName",
                List.of("환기팬", "급기팬")
        );

        // 시간 범위 계산 현재(서버 시간대 기준 UTC)
        Instant[] timeRange = ElasticUtil.calculateTimeRangeUTC();
        Instant startTime = timeRange[0];
        Instant endTime = timeRange[1];

        RangeQuery rangeQueryEventTime = new RangeQuery("eventTime", startTime.toEpochMilli(), endTime.toEpochMilli());

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryTertiaryCategoryName, mustTermQueryMonitoringItemName, mustTermsQueryMgmtSysName), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                List.of(rangeQueryEventTime)     // 빈 리스트(filter)
        );

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");
        SortQuery sortQuery2 = new SortQuery("eventId", "asc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1, sortQuery2), 100);

        //Elastic Search 조회
        List<FmFacilityObjectOperationStatusVO> operationStatusList = ElasticExecutor.searchAfterList(this.client, index, HttpMethodEnum.GET, queryJson, FmFacilityObjectOperationStatusVO.class);
        operationStatusList.forEach(status -> status.setOperationCheckDatetime(Date.from(endTime)));

        return operationStatusList;
    }



    @Override
    public List<FmAlarmDateChartDataVO> getAlarmFacilityDataList(String assetId, Integer facilityObjectId, String eventDateTime, String endEventDateTime, int searchSize) throws Exception {
        // DateFormat
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 시작 날짜 처리
        LocalDateTime startLocalDateTime = LocalDateTime.parse(eventDateTime, inputFormatter);
        String startDate = startLocalDateTime.format(dateFormatter);
        String endDate = "";

        //쿼리 선언
        ElasticQuery mustTermQueryAssetId = new ElasticQuery("term", "assetId", assetId);
        ElasticQuery mustTermQueryFacilityObjectId = new ElasticQuery("term", "facilityObjectId", facilityObjectId);

        // ES 조회용 StartDate UTC 시간 변환
        ZonedDateTime startUtcZonedDateTime = ElasticUtil.parseToUtcZonedDateTime(null, eventDateTime);
        Instant startUtcTimeLong = startUtcZonedDateTime.toInstant();

        CustomRangeQuery rangeQueryEventTime;
        if (endEventDateTime.isEmpty()) {
            rangeQueryEventTime = new CustomRangeQuery("eventTime")
                    .setLte(startUtcTimeLong.toEpochMilli());
        } else {

            // ES 조회용 EndDate UTC 시간 변환
            ZonedDateTime endUtcZonedDateTime = ElasticUtil.parseToUtcZonedDateTime(null, endEventDateTime);
            Instant endUtcTimeLong = endUtcZonedDateTime.toInstant();

            rangeQueryEventTime = new CustomRangeQuery("eventTime")
                    .setGt(startUtcTimeLong.toEpochMilli())
                    .setLte(endUtcTimeLong.toEpochMilli());

            // ES Index 생성용 시간
            LocalDateTime endLocalDateTime = LocalDateTime.parse(endEventDateTime, inputFormatter);
            endDate = endLocalDateTime.format(dateFormatter);
        }

        // BoolQuery 생성
        BoolQuery boolQuery = BoolQuery.createWithMatchQueries(
                List.of(mustTermQueryAssetId, mustTermQueryFacilityObjectId), // must
                Collections.emptyList(),        // 빈 리스트(should)
                Collections.emptyList(),        // 빈 리스트(must_not)
                List.of(rangeQueryEventTime)     // 빈 리스트(filter)
        );

        String index = ElasticUtil.generateIndexNames("fm_event_", startDate, endDate);

        // SortQuery 객체 생성 (여러 개의 정렬 기준)
        SortQuery sortQuery1 = new SortQuery("eventTime", "desc");

        //bool + sort 쿼리 -> Json 변환
        String queryJson = ElasticUtil.getQueryWithSort(boolQuery, List.of(sortQuery1), searchSize);

        return ElasticExecutor.searchList(this.client, index, HttpMethodEnum.GET, queryJson, FmAlarmDateChartDataVO.class);
    }
}
