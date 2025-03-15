package com.carming.backend.place.controller;

import com.carming.backend.place.dto.request.PlaceSearch;
import com.carming.backend.place.dto.response.PlaceResponseDto;
import com.carming.backend.place.dto.response.popular.PopularPlaceDetailDto;
import com.carming.backend.place.dto.response.popular.PopularPlaceListDto;
import com.carming.backend.place.service.PlaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/places")
@RestController
public class PlaceController {

    private final PlaceService placeService;

    //todo: must delete, only for spring security test
    @GetMapping("/test")
    public String test() {
        return "Login OK";
    }

    @GetMapping
    public List<PlaceResponseDto> getPlaces(@ModelAttribute PlaceSearch search) {
        log.info("search = {}", search);
        return placeService.getPlaces(search);
    }

    @GetMapping("/popular")
    public List<PopularPlaceListDto> getPopularPlaces() {
        return placeService.getPopularPlaces();
    }

    @GetMapping("/popular/{id}")
    public ResponseEntity<PopularPlaceDetailDto> getPopularPlaceDetail(@PathVariable("id") Long placeId) {
        return ResponseEntity.ok(placeService.getPopularPlaceDetail(placeId));
    }

    @GetMapping("/infinite-scroll")
    public List<PlaceResponseDto> readAllInfiniteScroll(
            @RequestParam("region") String region,
            @RequestParam("category") String category,
            @RequestParam("pageSize") Long pageSize,
            @RequestParam(value = "lastPlaceId", required = false) Long lastPlaceId
    ) {
        return placeService.readAllInfiniteScroll(region, category, pageSize, lastPlaceId);
    }

}
