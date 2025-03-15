package com.carming.backend.place.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class PlaceTodayRateRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String KEY_FORMAT = "review::%s::place::%s";
    private static final Long LIMIT_DAYS = 2L;

    private final StringRedisTemplate redisTemplate;

    public void increase(LocalDateTime date, Long placeId, Integer rate) {
        redisTemplate.opsForValue().increment(generateKey(date, placeId), rate);
        redisTemplate.expire(generateKey(date, placeId), getDurationFrom(date));
    }

    public Integer read(LocalDateTime date, Long placeId) {
        String result = redisTemplate.opsForValue().get(generateKey(date, placeId));
        return result == null ? 0 : Integer.valueOf(result);
    }

    public List<Integer> readAll(LocalDateTime date, List<Long> placeIds) {
        List<String> keys = placeIds.stream()
                .map(placeId -> generateKey(date, placeId))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(keys)) {
            return new ArrayList<>();
        }

        List<String> rates = redisTemplate.opsForValue().multiGet(keys);
        return rates.stream()
                .map(rate -> rate == null ? 0 : Integer.valueOf(rate))
                .collect(Collectors.toList());
    }

    private String generateKey(LocalDateTime now, Long placeId) {
        return String.format(KEY_FORMAT, generateKey(now), placeId);
    }

    private String generateKey(LocalDateTime now) {
        return now.format(DATE_TIME_FORMATTER);
    }

    private Duration getDurationFrom(LocalDateTime now) {
        LocalDateTime limitDay = now.plusDays(LIMIT_DAYS);
        return Duration.between(now, limitDay);
    }
}
