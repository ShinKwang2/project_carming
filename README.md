## Carming Backend

## 0. 지역별 파티셔닝

먼저 Place 테이블은 다음과 같다.

```sql
CREATE TABLE place (
    place_id BIGINT AUTO_INCREMENT,
    place_name VARCHAR(100) NOT NULL,
    place_tel VARCHAR(20),
    place_category VARCHAR(50) NOT NULL,
    place_lon DOUBLE,
    place_lan DOUBLE,
    place_region VARCHAR(50),
    place_address VARCHAR(80),
    place_rating_count INT DEFAULT 0,
    place_rating_sum INT DEFAULT 0,
    place_keyword VARCHAR(255),
    place_image VARCHAR(255),
    PRIMARY KEY (placed_id)
);
```

장소라는 데이터 특성상 한번 정해지면 잘 바뀌지 않는다. 또한 이 데이터는 기본적으로 지속적으로 늘어날 수밖에 없다. 따라서 지역별로 Partition을 나누고자 한다. 어차피 기획한 서비스가 사용자가 선택한 구에서 장소를 추천하기 때문에 더더욱 파티셔닝을 하는 것이 좋다고 판단되었다.

```sql
    ...
    place_image VARCHAR(255),
    PRIMARY KEY (placed_id)
) PARTITION BY LIST (place_region) (
    PARTITION p_seoul_gangnam VALUES IN ('강남구'),
    PARTITION p_seoul_gangbuk VALUES IN ('강북구'),
    PARTITION p_seoul_gangdong VALUES IN ('강동구'),
    PARTITION p_seoul_mapo VALUES IN ('마포구')
    ...
);

```

## 1. Covering Index를 이용한 조회 성능 개선

이제 각 구마다 별점이 높은 순서로 보여주어야 하는데, 기본적인 쿼리는 다음과 같다.

```sql
SELECT * FROM place
  WHERE place_region = :region AND place_category = :category
  ORDER BY place_rating_sum DESC
  OFFSET :offset
  LIMIT :limit;
```

```java
queryFactory
  .selectFrom(place)
  .where(regionEq(search.getRegion()), categoryEq(search.getCategory()))
  .orderBy(place.ratingSum.desc())
  .offset(search.getOffset())
  .limit(search.getSize())
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
