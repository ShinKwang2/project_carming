package com.carming.backend.place.service;

import com.carming.backend.place.domain.Place;
import com.carming.backend.place.dto.request.PlaceSearch;
import com.carming.backend.place.dto.response.PlaceResponseDto;
import com.carming.backend.place.dto.response.popular.PopularPlaceDetailDto;
import com.carming.backend.place.dto.response.popular.PopularPlaceListDto;
import com.carming.backend.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class PlaceService {

    private final Long DEFAULT_POPULAR_SIZE = 20L;

    private final PlaceRepository placeRepository;

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
        return places.stream()
                .map(PlaceResponseDto::from)
                .collect(Collectors.toList());
    }

    private List<PlaceResponseDto> findPlacesByTag(PlaceSearch search) {
        List<Place> places = placeRepository.findPlacesByTag(search);
        return places.stream()
                .map(PlaceResponseDto::from)
                .collect(Collectors.toList());
    }
}
