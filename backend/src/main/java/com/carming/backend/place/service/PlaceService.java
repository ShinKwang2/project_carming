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
        List<Place> places = placeRepository.findPlaces(search);
        return places.stream()
                .map(PlaceResponseDto::from)
                .collect(Collectors.toList());
    }

    public List<PopularPlaceListDto> getPopularPlaces() {
        return placeRepository.findPopular(DEFAULT_POPULAR_SIZE);
    }

    public PopularPlaceDetailDto getPopularPlaceDetail(Long placeId) {
        return placeRepository.findPopularPlaceDetail(placeId);
    }
}
