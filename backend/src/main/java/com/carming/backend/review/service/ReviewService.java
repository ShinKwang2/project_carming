package com.carming.backend.review.service;

import com.carming.backend.course.domain.Course;
import com.carming.backend.course.exception.CourseNotFound;
import com.carming.backend.course.repository.CourseRepository;
import com.carming.backend.member.domain.Member;
import com.carming.backend.member.exception.MemberNotFound;
import com.carming.backend.member.repository.MemberRepository;
import com.carming.backend.place.domain.Place;
import com.carming.backend.place.domain.PlaceTag;
import com.carming.backend.place.repository.PlaceRepository;
import com.carming.backend.place.repository.PlaceTagRepository;
import com.carming.backend.review.domain.Review;
import com.carming.backend.review.domain.ReviewTag;
import com.carming.backend.review.dto.request.ReviewRequestDto;
import com.carming.backend.review.dto.response.ReviewResponseDto;
import com.carming.backend.review.repository.ReviewRepository;
import com.carming.backend.review.repository.ReviewTagRepository;
import com.carming.backend.place.repository.PlaceTodayRateRepository;
import com.carming.backend.tag.domain.Tag;
import com.carming.backend.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ReviewService {

    private final MemberRepository memberRepository;
    private final CourseRepository courseRepository;
    private final ReviewRepository reviewRepository;

    private final TagRepository tagRepository;

    private final ReviewTagRepository reviewTagRepository;

    private final PlaceTodayRateRepository placeTodayRateRepository;

    private final PlaceRepository placeRepository;

    private final PlaceTagRepository placeTagRepository;


    //todo 메소드 분리
    @Transactional
    public Long saveReview(ReviewRequestDto request, Long memberId) {

        Member loginMember = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFound::new);

        Course foundCourse = courseRepository.findById(request.getCourseReview().getCourseId())
                .orElseThrow(CourseNotFound::new);
        foundCourse.addRating(request.getCourseReview().getCourseRating());

        Review review = Review.builder()
                .courseRating(request.getCourseReview().getCourseRating())
                .content(request.getCourseReview().getContent())
                .build();
        review.changeMember(loginMember);
        review.changeCourse(foundCourse);

        Review savedReview = reviewRepository.save(review); //리뷰 저장
        ///////

        List<Tag> tags = tagRepository.findAllById(request.getCourseReview().getCourseTags());
        for (Tag tag : tags) {
            ReviewTag reviewTag = new ReviewTag(savedReview, tag);
            reviewTag.changeReview(savedReview);
            reviewTagRepository.save(reviewTag);
        }
        // 리뷰 태그 저장

        // 리뷰 별점 저장
        LocalDateTime now = LocalDateTime.now();
        List<Long> reviewIds = request.getPlaceReviews().stream()
                .map(ReviewRequestDto.PlaceReviewRequest::getPlaceId)
                .collect(Collectors.toList());
        List<Long> existingIds = placeRepository.existsAllByIds(reviewIds);
        request.getPlaceReviews().stream()
                .filter(placeReview -> existingIds.contains(placeReview.getPlaceId()))
                .forEach(placeReview -> {
                    placeTodayRateRepository.increase(now, placeReview.getPlaceId(), placeReview.getPlaceRating());
                    tagRepository.findAllById(placeReview.getPlaceTags()).stream()
                            .forEach(tag -> placeTagRepository.save(
                                    new PlaceTag(
                                            getPlaceProxy(placeReview.getPlaceId()),
                                            tag
                                    ))
                            );
                });

//        request.getPlaceReviews().stream()
//                .forEach(placeReview -> {
//                    Place foundPlace = placeRepository.findById(placeReview.getPlaceId())
//                            .orElseThrow(PlaceNotFound::new); //Place 조회
//                    foundPlace.addRating(placeReview.getPlaceRating());
//                    tagRepository.findAllById(placeReview.getPlaceTags()).stream()
//                            .forEach(tag -> {
//                                placeTagRepository.save(new PlaceTag(foundPlace, tag)); //Place 태그 조회 및 저장
//                            });
//                });

        return savedReview.getId();
    }

    public List<ReviewResponseDto> findByCourseTest1(Long courseId, Long size) {
        // 리뷰 리스트를 가져와 반환, 쿼리가 기본 쿼리 1개, 태그 쿼리가 태그 개수만큼 최대 4번 더 나간다.
        List<Review> reviews = reviewRepository.findByCourseTest1(courseId, size);

        List<ReviewResponseDto> response = reviews.stream()
                .map(ReviewResponseDto::from)
                .collect(Collectors.toList());

        return response;
    }

    public List<ReviewResponseDto> findByCourseTest2(Long courseId) {
        List<ReviewResponseDto> response = reviewRepository.findByCourseTest2(courseId);

//        for (ReviewResponseDto reviewResponseDto : response) {
//            System.out.println(reviewResponseDto);
//        };
        return response;
    }

    private List<String> getTagName(List<ReviewTag> reviewTags) {
        List<String> tagNames = new ArrayList<>();
        for (ReviewTag reviewTag : reviewTags) {
            tagNames.add(reviewTag.getTag().getName());
        }
        return tagNames;
    }

    private void saveCourseReview(ReviewRequestDto request) {


        request.getCourseReview().getCourseId();
        request.getCourseReview().getCourseTags();
        request.getCourseReview().getCourseRating();
        request.getCourseReview().getCourseRating();

        Review.builder()
                .courseRating(request.getCourseReview().getCourseRating())
                .content(request.getCourseReview().getContent())
                .build();
    }

    private Place getPlaceProxy(Long placeId) {
        return placeRepository.getProxy(placeId);
    }
}
