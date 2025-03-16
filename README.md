# Carming Backend

## 0. 지역별 파티셔닝

먼저 Place 테이블은 다음과 같다.

```sql
CREATE TABLE place (
    place_id BIGINT AUTO_INCREMENT,
    place_name VARCHAR(100) NOT NULL,
    place_tel VARCHAR(20),
    place_category VARCHAR(50) NOT NULL,
    place_lon DOUBLE,
    place_lat DOUBLE,
    place_region VARCHAR(50),
    place_address VARCHAR(80),
    place_rating_count INT DEFAULT 0,
    place_rating_sum INT DEFAULT 0,
    place_keyword VARCHAR(255),
    place_image VARCHAR(255),
    PRIMARY KEY (place_id)
);
```

장소라는 데이터 특성상 한번 정해지면 잘 바뀌지 않는다. 또한 이 데이터는 기본적으로 지속적으로 늘어날 수밖에 없다. 따라서 지역별로 Partition을 나누고자 한다. 어차피 기획한 서비스가 사용자가 선택한 구에서 장소를 추천하기 때문에 더더욱 파티셔닝을 하는 것이 좋다고 판단되었다.

```sql
    ...
    place_image VARCHAR(255),
    PRIMARY KEY (place_id, place_region)
) PARTITION BY LIST COLUMNS (place_region) (
    PARTITION p_seoul_gangnam VALUES IN ('강남구'),
    PARTITION p_seoul_gangbuk VALUES IN ('강북구'),
    PARTITION p_seoul_gangdong VALUES IN ('강동구'),
    PARTITION p_seoul_mapo VALUES IN ('마포구')
    ...
);
```

**MySQL의 제약사항으로 PK에 place_region을 포함해야 한다.**

## 1. Covering Index를 이용한 조회 성능 개선

이제 각 구마다 별점이 높은 순서로 보여주어야 하는데, 기본적인 쿼리는 다음과 같다.
원래 성능을 위해서 \* 를 쓰지 않지만 여기서는 그냥 쓰겠다.

```sql
SELECT * FROM place
  WHERE place_region = :region AND place_category = :category
  ORDER BY place_rating_sum DESC
  LIMIT :limit
  OFFSET :offset;
```

```java
queryFactory
  .selectFrom(place)
  .where(regionEq(search.getRegion()), categoryEq(search.getCategory()))
  .orderBy(place.ratingSum.desc())
  .limit(search.getSize())
  .offset(search.getOffset())
  .fetch();
```

하지만 자율주행 시뮬레이터에서 제공되는 맵인 마포구만 해도 대표사진이 있는 장소 데이터가 15만 개, 대표사진이 없는 데이터를 포함하면 40만개를 넘어갔다. 일단 대표사진이 있는 데이터만 쓰기로 결정해서, 크게 문제가 되지는 않지만 **요구사항이 달라지면 언제든지 데이터가 기하급수적으로 늘어날 우려가 있다**.

따라서 Covering Index를 사용하여 조회 속도를 줄이도록 할 것이다. 그리고 Covering Index로 생기는 트레이드 오프인 쓰기 부하를 어떻게 줄일지 생각해보자.

### 1️⃣ Index 만들기

현재 Place 리스트를 조회할 때 사용하는 조건은 다음과 같다.

- place_region
- place_category
- place_rating_sum

이중에 rationg_sum의 경우 종종 바뀌겠지만, place_region과 place_category의 경우 거의 read-only에 가까울 정도로 자주 바뀌지 않는다. **따라서 인덱스는 아래와 같이 만든다**.

```sql
CREATE INDEX idx_region_category_rating_sum_desc ON place (place_region, place_category, place_rating_sum DESC);
```

**실행 계획 분석하기**

```sql
explain SELECT * FROM place WHERE place_region = '마포구' AND place_category = 'CAFE' ORDER BY place_rating_sum DESC LIMIT 10 OFFSET 20000;
```

이렇게 가져오면 문제가 발생한다. 일단 MySQL의 경우 직접 만든 인덱스의 경우 Secondary Index이다. Secondary Index는 데이터에 접근하기 위한 포인터(PK)만 가지고 있을 뿐이다. 따라서 다음과 같은 순서를 가진다.

1. (place_region, place_category, place_rationg_sum DESC)로 생성된 Secondary Index에서 PK인 (place_id, place_region)을 찾는다.
2. Clustered Index에서 place 데이터를 찾는다.
3. OFFSET 20000을 만날 때까지 반복한다.
4. LIMIT 10개를 추출한다.

OFFSET을 할 때 동안 굳이 데이터에 계속 접근할 필요가 없다. 그렇다면 어떻게 해야 할까? 먼저 Secondary Index의 포인터로 PK 값이 담겨 있기 때문에 place_id, place_region만 조회한다면 Index를 사용할 것이다. 확인해보자.

```sql
explain SELECT place_id, place_region FROM place WHERE place_region = '마포구' AND place_category = 'CAFE' ORDER BY place_rating_sum DESC LIMIT 10 OFFSET 20000;
```

- partions: p_seoul_mapo
- key: idx_region_category_rating_sum_desc
- Extra: Using Index

위와 같이 인덱스를 잘 타는 것을 알 수 있다. 그렇다면 Secondary Index에서 포인터 값(PK)만 뽑은 다음 PK로 다시 조회한다면 어떨까?

### 2️⃣ Secondary Index와 Clustered Index 조합

먼저 이렇게 하면 잘 될까?

```sql
explain SELECT * FROM (
  SELECT place_id, place_region FROM place
    WHERE place_region = '마포구' AND place_category = 'CAFE'
    ORDER BY place_rating_sum DESC
    LIMIT 10 OFFSET 20000) t
    INNER JOIN place p
    ON t.place_id = p.place_id AND t.place_region = p.place_region;
```

p 즉, place 테이블에서 partions의 값이 없으므로 모든 파티션을 조회한다.

따라서 **p의 place_region이 마포구**만 가져오자.

```sql
explain SELECT * FROM (
  SELECT place_id, place_region FROM place
    WHERE place_region = '마포구' AND place_category = 'CAFE'
    ORDER BY place_rating_sum DESC
    LIMIT 10 OFFSET 20000) t
    INNER JOIN place p
    ON t.place_id = p.place_id AND t.place_region = p.place_region
    WHERE p.place_region = '마포구'
```

이제 partitions는 mapo로 잘 들어오지만 뭔가 아쉽다. p 테이블에서 Extra가 null이기 때문이다.
따라서 p.place_category = 'CAFE'도 붙여주자

```sql
explain SELECT * FROM (
  SELECT place_id, place_region FROM place
    WHERE place_region = '마포구' AND place_category = 'CAFE'
    ORDER BY place_rating_sum DESC
    LIMIT 10 OFFSET 20000) t
    INNER JOIN place p
    ON t.place_id = p.place_id AND t.place_region = p.place_region
    WHERE p.place_region = '마포구' AND p.place_category = 'CAFE';
```

### 3️⃣ QueryDSL에 적용

QueryDSL에서는 FROM절에 서브쿼리가 지원이 안되는 만큼 두번에 나눠서 하도록하자.

```java
 public List<Place> findPlaces(PlaceSearch search) {
        List<PlaceIdRegion> idRegions = queryFactory
                .select(Projections.constructor(PlaceIdRegion.class, place.id, place.region))
                .from(place)
                .where(regionEq(search.getRegions()), categoryEq(search.getCategory()))
                .orderBy(place.ratingSum.desc())
                .limit(search.getSize())
                .offset(search.getOffset())
                .fetch();

        if (CollectionUtils.isEmpty(idRegions)) {
            return new ArrayList<>();
        }

        return queryFactory
                .selectFrom(place)
                .where(
                        place.id.in(idRegions.stream().map(PlaceIdRegion::getId).collect(Collectors.toList()))
                        place.region.in(idRegions.stream().map(PlaceIdRegion::getRegion).collect(Collectors.toList()))
                )
                .orderBy(place.ratingSum.desc())
                .fetch();
    }
```
