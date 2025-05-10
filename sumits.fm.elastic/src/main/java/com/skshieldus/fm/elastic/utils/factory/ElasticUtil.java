package com.skshieldus.fm.elastic.utils.factory;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skshieldus.fm.elastic.utils.query.BoolQuery;
import com.skshieldus.fm.elastic.utils.query.SortQuery;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // prefix와 특정 시간 범위 내의 인덱스 이름을 생성하는 메서드
    public static String getIndexNames(String prefix) {
        // 한국 시간대 설정
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        // 현재 한국 시간 가져오기
        ZonedDateTime nowKorea = ZonedDateTime.now(koreaZone);

        // 한국 시간으로 어제와 오늘 날짜 구하기
        LocalDate todayKorea = nowKorea.toLocalDate();
        LocalDate yesterdayKorea = todayKorea.minusDays(1);

        // 한국 시간으로 06:00과 10:00 시간 설정
        LocalTime startTime = LocalTime.of(6, 0);
        LocalTime endTime = LocalTime.of(10, 0);

        // 시작 시간과 종료 시간을 한국 시간대로 ZonedDateTime으로 변환
        ZonedDateTime startKorea = ZonedDateTime.of(todayKorea, startTime, koreaZone);
        ZonedDateTime endKorea = ZonedDateTime.of(todayKorea, endTime, koreaZone);

        // 현재 시간이 한국 시간으로 06:00 ~ 10:00 사이인지 확인
        boolean isBetween = nowKorea.isAfter(startKorea) && nowKorea.isBefore(endKorea);

        // 현재 시간이 10:00 이후인지 확인
        boolean isAfterEnd = nowKorea.isAfter(endKorea);

        // 날짜 포맷
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String yesterdayIndex = formatter.format(yesterdayKorea);
        String todayIndex = formatter.format(todayKorea);

        // 인덱스 이름 생성
        if (isBetween) {
            // 06:00 ~ 10:00 사이에는 어제의 인덱스만 생성
            return String.format("%s_%s", prefix, yesterdayIndex);
        } else if (isAfterEnd) {
            // 10:00 이후에는 어제와 오늘 인덱스 모두 생성
            return String.format("%s_%s,%s_%s", prefix, yesterdayIndex, prefix, todayIndex);
        } else {
            // 그 외의 시간대에는 오늘 인덱스만 생성
            return String.format("%s_%s", prefix, todayIndex);
        }
    }

    /**
     * 주어진 현재 시간과 시간대를 기준으로 15분 전부터 현재까지의 시간 범위를 UTC로 계산합니다.
     *
     * @return 시작 시간과 끝 시간을 포함한 배열 (UTC 기준)
     */
    public static Instant[] calculateTimeRangeUTC() {

        // 현재 시스템 시간을 기준으로
        LocalDateTime now = LocalDateTime.now();

        // 원하는 시간대 설정 (예: 한국 시간대)
        ZoneId zoneId = ZoneId.of("Asia/Seoul");

        // 현재 시간을 기준으로 가장 가까운 15분 단위로 정시를 맞추기
        LocalDateTime endLocalTime = now.truncatedTo(ChronoUnit.MINUTES);

        // 15분 단위로 정시를 맞추기 위한 계산
        int minutes = endLocalTime.getMinute();
        int roundedMinutes = (minutes / 15) * 15;
        endLocalTime = endLocalTime.withMinute(roundedMinutes).withSecond(0).withNano(0);

        // 시작 시간은 endLocalTime에서 15분 전
        LocalDateTime startLocalTime = endLocalTime.minusMinutes(15);

        // LocalDateTime을 UTC로 변환하여 Instant 생성
        ZonedDateTime startZonedTimeUTC = startLocalTime.atZone(zoneId).withZoneSameInstant(ZoneId.of("UTC"));
        ZonedDateTime endZonedTimeUTC = endLocalTime.atZone(zoneId).withZoneSameInstant(ZoneId.of("UTC"));

        // Instant로 변환하여 리턴
        Instant startTime = startZonedTimeUTC.toInstant();
        Instant endTime = endZonedTimeUTC.toInstant();

        return new Instant[]{startTime, endTime};
    }


    /**
     * Elasticsearch 쿼리 JSON을 생성합니다.
     * @param boolQuery BoolQuery 객체
     * @param sortQueries 정렬 기준 리스트
     * @return Elasticsearch 쿼리 JSON 문자열
     * @throws Exception JSON 직렬화 오류
     */
    public static String getQueryWithSort(BoolQuery boolQuery, List<SortQuery> sortQueries, Integer size) throws Exception {
        // 쿼리 구성
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("query", Map.of("bool", boolQuery));

        // 정렬 기준 추가
        if (sortQueries != null && !sortQueries.isEmpty()) {
            List<Map<String, Object>> sortCriteria = sortQueries.stream()
                    .map(SortQuery::toMap)
                    .toList();
            queryMap.put("sort", sortCriteria);
        }

        // size 추가
        if (size != null) {
            queryMap.put("size", size);
        }

        // JSON 문자열로 변환
        return objectMapper.writeValueAsString(queryMap);
    }

    public static String getQueryBool(BoolQuery boolQuery) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(
                Map.of("query", Map.of("bool", boolQuery))
        );
    }

    public static ZonedDateTime parseToUtcZonedDateTime(String zoneId, String dateTimeString) {
        // 날짜 파싱을 위한 포맷터 설정
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 로컬 날짜 및 시간 파싱
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, formatter);
        // 로컬 시간대를 지정하여 ZonedDateTime으로 변환

        if(zoneId == null || zoneId.isEmpty()) { zoneId = "Asia/Seoul"; }
        ZoneId localZoneId = ZoneId.of(zoneId); // 한국 표준시
        ZonedDateTime localZonedDateTime = localDateTime.atZone(localZoneId);
        // UTC 시간대로 변환
        // Instant로 반환
        return localZonedDateTime.withZoneSameInstant(ZoneOffset.UTC);
    }

    public static String generateIndexNames(String prefix, String startDate, String endDate) {
        // 시작일 파싱 (하루 전부터)
        LocalDate start = LocalDate.parse(startDate).minusDays(1);

        // 종료일이 없으면 시작일만 처리
        LocalDate end = (endDate == null || endDate.trim().isEmpty())
                ? LocalDate.parse(startDate)
                : LocalDate.parse(endDate);

        // 시작일이 종료일보다 후면 swap
        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }

        StringBuilder indexString = new StringBuilder();
        LocalDate currentDate = start;

        while (!currentDate.isAfter(end)) {
            if (!indexString.isEmpty()) {
                indexString.append(",");
            }
            indexString.append(prefix)
                    .append(currentDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            currentDate = currentDate.plusDays(1);
        }

        return indexString.toString();
    }

    public static List<Map<String, Object>> getSortQueries(List<SortQuery> sortQueries) {
        List<Map<String, Object>> sortCriteria = null;

        if (sortQueries != null && !sortQueries.isEmpty()) {
            sortCriteria = sortQueries.stream()
                    .map(SortQuery::toMap)
                    .toList();
        }

        return sortCriteria;
    }
}
