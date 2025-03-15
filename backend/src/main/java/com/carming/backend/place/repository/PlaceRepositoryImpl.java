package com.carming.backend.place.repository;

import com.carming.backend.place.domain.Place;
import com.carming.backend.place.domain.PlaceCategory;
import com.carming.backend.place.dto.request.PlaceSearch;
import com.carming.backend.place.dto.response.popular.PlaceTagsBox;
import com.carming.backend.place.dto.response.popular.PopularPlaceDetailDto;
import com.carming.backend.place.dto.response.popular.PopularPlaceListDto;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;

import static com.carming.backend.place.domain.QPlace.place;
import static com.carming.backend.place.domain.QPlaceTag.placeTag;
import static com.carming.backend.tag.domain.QTag.tag;

@RequiredArgsConstructor
public class PlaceRepositoryImpl implements PlaceRepositoryCustom {

    @PersistenceContext
    private final EntityManager entityManager;
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Place> findPlaces(PlaceSearch search) {
        List<Long> ids = queryFactory
                .select(place.id)
                .from(place)
                .where(regionEq(search.getRegions()), categoryEq(search.getCategory()))
                .orderBy(place.ratingSum.desc())
                .offset(search.getOffset())
                .limit(search.getSize())
                .fetch();

        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }

        return queryFactory
                .selectFrom(place)
                .where(place.id.in(ids))
                .orderBy(place.ratingSum.desc())
                .fetch();
    }

    @Override
    public List<Place> findPlacesByTag(PlaceSearch search) {
        return queryFactory
                .select(placeTag.place)
                .from(placeTag)
                .where(regionTagEq(search.getRegions()), placeTag.tag.id.eq(search.getTagId()))
                .groupBy(placeTag.place)
                .orderBy(placeTag.place.count().desc())
                .offset(search.getOffset())
                .limit(search.getSize())
                .fetch();
    }

    @Override
    public List<Place> findPlacesByCourse(List<Long> placeKeys) {
        return queryFactory
                .selectFrom(place)
                .where(place.id.in(placeKeys))
                .fetch();
    }

    @Override
    public List<PopularPlaceListDto> findPopular(Long size) {
        return queryFactory
                .select(Projections.fields(PopularPlaceListDto.class,
                        place.id, place.image, place.name, place.address,
                        place.region, place.ratingSum, place.ratingCount))
                .from(place)
                .orderBy(place.ratingSum.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public PopularPlaceDetailDto findPopularPlaceDetail(Long id) {
        PopularPlaceDetailDto placeDetail = queryFactory
                .select(Projections.fields(PopularPlaceDetailDto.class,
                        place.id, place.name, place.tel, place.image, place.region,
                        place.address, place.ratingSum, place.ratingCount))
                .from(place)
                .where(place.id.eq(id))
                .fetchFirst();

        List<PlaceTagsBox> tags = queryFactory
                .select(Projections.fields(PlaceTagsBox.class,
                        placeTag.tag.name.as("tagName"),
                        placeTag.count().as("tagCount")))
                .from(placeTag)
                .join(placeTag.tag, tag)
                .join(placeTag.place, place)
                .where(placeTag.place.id.eq(id))
                .groupBy(placeTag.tag)
                .fetch();

        placeDetail.changePlaceTagsBox(tags);
        return placeDetail;
    }

    @Override
    public List<String> findPlaceNamesById(List<Long> placeKeys) {
        return queryFactory
                .select(place.name)
                .from(place)
                .where(place.id.in(placeKeys))
                .fetch();
    }

    @Override
    public List<String> findRegionsById(List<Long> placeKeys) {
        return queryFactory
                .select(place.region)
                .distinct()
                .from(place)
                .where(place.id.in(placeKeys))
                .fetch();
    }

    @Override
    public Place getProxy(Long placeId) {
        return entityManager.getReference(Place.class, placeId);
    }

    private BooleanExpression regionEq(List<String> regions) {
        if (regions == null) { //지역구 선택이 없을 시
            return null;
        }
        if (regions.isEmpty()) {//빈 리스트일 때
            return null;
        }
        return place.region.in(regions);
    }

    private BooleanExpression regionTagEq(List<String> regions) {
        if (regions == null) { //지역구 선택이 없을 시
            return null;
        }
        if (regions.isEmpty()) {//빈 리스트일 때
            return null;
        }
        return placeTag.place.region.in(regions);
    }

    private BooleanExpression categoryEq(String category) {
        if (!StringUtils.hasText(category)) { //category == null || category.equals("")
            return null;
        }

        try {
            return place.category.eq(PlaceCategory.valueOf(category.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
