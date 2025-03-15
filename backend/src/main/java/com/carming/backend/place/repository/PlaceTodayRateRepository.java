package com.carming.backend.place.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class PlaceTodayRateRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String RATING_FORMAT = "review::%s::place::rating::%s";
    private static final String RATING_COUNT_FORMAT = "review::%s::place::count::%s";
    private static final Long LIMIT_DAYS = 2L;

    private final StringRedisTemplate redisTemplate;

    public void increase(LocalDateTime date, Long placeId, Integer rate) {
        String ratingKey = generateRatingKey(date, placeId);
        String countKey = generateCountKey(date, placeId);
        redisTemplate.executePipelined((RedisCallback<?>) action -> {
            StringRedisConnection conn = (StringRedisConnection) action;
            conn.incrBy(ratingKey, rate);
            conn.expire(ratingKey, getDurationSecondsFrom(date));
            conn.incr(countKey);
            conn.expire(countKey, getDurationSecondsFrom(date));
            return null;
        });
    }

    public Integer read(LocalDateTime date, Long placeId) {
        String result = redisTemplate.opsForValue().get(generateRatingKey(date, placeId));
        return result == null ? 0 : Integer.valueOf(result);
    }

    public List<Integer> readAll(LocalDateTime date, List<Long> placeIds) {
        List<String> keys = placeIds.stream()
                .map(placeId -> generateRatingKey(date, placeId))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(keys)) {
            return new ArrayList<>();
        }

        List<String> rates = redisTemplate.opsForValue().multiGet(keys);
        return rates.stream()
                .map(rate -> rate == null ? 0 : Integer.valueOf(rate))
                .collect(Collectors.toList());
    }

    private String generateRatingKey(LocalDateTime now, Long placeId) {
        return String.format(RATING_FORMAT, generateKey(now), placeId);
    }

    private String generateCountKey(LocalDateTime now, Long placeId) {
        return String.format(RATING_COUNT_FORMAT, generateKey(now), placeId);
    }

    private String generateKey(LocalDateTime now) {
        return now.format(DATE_TIME_FORMATTER);
    }

    private Long getDurationSecondsFrom(LocalDateTime now) {
        LocalDateTime limitDay = now.plusDays(LIMIT_DAYS);
        return Duration.between(now, limitDay).getSeconds();
    }
}
