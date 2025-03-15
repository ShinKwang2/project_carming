package com.carming.backend.place.service;

import com.carming.backend.place.domain.Place;
import com.carming.backend.place.dto.request.PlaceSearch;
import com.carming.backend.place.dto.response.PlaceResponseDto;
import com.carming.backend.place.dto.response.popular.PopularPlaceDetailDto;
import com.carming.backend.place.dto.response.popular.PopularPlaceListDto;
import com.carming.backend.place.repository.PlaceRepository;
import com.carming.backend.place.repository.PlaceTodayRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class PlaceService {

    private final Long DEFAULT_POPULAR_SIZE = 20L;

    private final PlaceRepository placeRepository;
    private final PlaceTodayRateRepository placeTodayRateRepository;

    public List<PlaceResponseDto> getPlaces(PlaceSearch search) {
        if (search.getTagId() != null) {
            return findPlacesByTag(search);
        }
        return findPlaces(search);
    }

    public List<PopularPlaceListDto> getPopularPlaces() {
        return placeRepository.findPopular(DEFAULT_POPULAR_SIZE);
    }

    public PopularPlaceDetailDto getPopularPlaceDetail(Long placeId) {
        return placeRepository.findPopularPlaceDetail(placeId);
    }

    public List<PlaceResponseDto> readAllInfiniteScroll(String region, String category, Long pageSize, Long lastPlaceId) {
        List<Place> places = lastPlaceId == null ?
                placeRepository.findAllInfiniteScroll(region, category, pageSize) :
                placeRepository.findAllInfiniteScroll(region, category, pageSize, lastPlaceId);
        return places.stream()
                .map(PlaceResponseDto::from)
                .collect(Collectors.toList());
    }

    private List<PlaceResponseDto> findPlaces(PlaceSearch search) {
        List<Place> places = placeRepository.findPlaces(search);
        List<Long> ids = places.stream()
                .map(place -> place.getId()).collect(Collectors.toList());
        List<Integer> rates = placeTodayRateRepository.readAll(LocalDateTime.now(), ids);


        return IntStream.range(0, Math.min(ids.size(), rates.size()))
                .mapToObj(index -> PlaceResponseDto.from(places.get(index), rates.get(index)))
                .collect(Collectors.toList());
    }

    private List<PlaceResponseDto> findPlacesByTag(PlaceSearch search) {
        List<Place> places = placeRepository.findPlacesByTag(search);
        List<Long> ids = places.stream()
                .map(place -> place.getId()).collect(Collectors.toList());
        List<Integer> rates = placeTodayRateRepository.readAll(LocalDateTime.now(), ids);

        return IntStream.range(0, Math.min(ids.size(), rates.size()))
                .mapToObj(index -> PlaceResponseDto.from(places.get(index), rates.get(index)))
                .collect(Collectors.toList());
    }
}
