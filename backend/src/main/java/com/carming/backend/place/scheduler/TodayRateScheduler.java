package com.carming.backend.place.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class TodayRateScheduler {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String RATING_PATTERN = "review::%s::place::rating::*";
    private static final String COUNT_PATTERN = "review::%s::place::count::*";

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(cron = "0 0 3 * * *")
    public void updateTodayRate() {
        String date = LocalDateTime.now().minusDays(1L).format(DATE_TIME_FORMATTER);
        String ratingPattern = generateKeyByRatingPattern(date);
        String countPattern = generateKeyByCountPattern(date);

        List<String> ratingKeys = redisTemplate.keys(ratingPattern).stream().collect(Collectors.toList());
        List<Long> ratings = redisTemplate.opsForValue().multiGet(ratingKeys).stream()
                .filter(Objects::nonNull)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        List<String> countKeys = redisTemplate.keys(countPattern).stream().collect(Collectors.toList());
        List<Long> counts = redisTemplate.opsForValue().multiGet(countKeys).stream()
                .filter(Objects::nonNull)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        List<Long> placeIds = getPlaceIds(ratingKeys);

        int minSize = Math.min(placeIds.size(), Math.min(ratings.size(), counts.size()));

        String sql = "UPDATE place SET place_rating_sum = place_rating_sum + ?, place_rating_count = place_rating_count + ? WHERE place_id = ?";

        List<Object[]> batchArgs = new ArrayList<>();
        for (int i = 0; i < minSize; i++) {
            batchArgs.add(new Object[]{ ratings.get(i), counts.get(i), placeIds.get(i) });
        }
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private String generateKeyByRatingPattern(String date) {
        return String.format(RATING_PATTERN, date);
    }

    private String generateKeyByCountPattern(String date) {
        return String.format(COUNT_PATTERN, date);
    }

    private List<Long> getPlaceIds(List<String> keys) {
        return keys.stream()
                .map(key -> Long.valueOf(key.split("::")[3]))
                .collect(Collectors.toList());
    }

}
