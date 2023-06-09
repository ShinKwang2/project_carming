import {
  TouchableOpacity,
  Text,
  View,
  ViewStyle,
  StyleSheet,
  TextStyle,
  ImageBackground,
  ScrollView,
} from 'react-native'; // 리액트 네이티브에서 제공하는 컴포넌트 추가
import {Course} from '../types';
import {calcRating} from '../utils';
import styled from 'styled-components';
import RatingStar from './RatingStar';
import {useDispatch, useSelector} from 'react-redux';
import {
  addPlaceListToPreCart,
  addSelectedCourse,
  addSelectedCoursesInstance,
  addSelectedCoursesInstanceReviews,
} from '../redux/slices/mainSlice';
import {RootState} from '../redux/store';
import {REST_API_URL} from '@env';

interface PopularCourseItemProps {
  course: Course;
  index: number;
  buttonStyle?: ViewStyle;
  textStyle?: TextStyle;
  iconStyle?: {};
  onPress?: () => void;
  disabled?: boolean;
}

const PopularCourseItem: React.FC<PopularCourseItemProps> = ({
  course,
  index,
  buttonStyle,
  textStyle,
  iconStyle,
  onPress,
  disabled = false,
}) => {
  const dispatch = useDispatch();
  const token = useSelector((state: RootState) => state.auth.token);
  const headers = token ? {Authorization: token} : {};
  const handlePress = async () => {
    dispatch(addSelectedCourse(course));
    // 선택된 course의 id를 기반으로 추가적인 정보를 불러옴
    console.log(course);
    try {
      if (onPress && !disabled) onPress();
      const response1 = await fetch(
        `${REST_API_URL}/courses/popular/${course.id}`,
        {method: 'GET', headers},
      ); // 인기 코스 중 선택된 것의 상세 호출
      const response2 = await fetch(
        `${REST_API_URL}/courses/${course.id}/reviews`,
        {method: 'GET', headers},
      ); // 선택된 코스의 리뷰 호출
      const data1 = await response1.json();
      const data2 = await response2.json();
      // data를 기반으로 상태 업데이트
      // console.log('Selected Place Instance:', data);
      dispatch(addSelectedCoursesInstance(data1));
      dispatch(addSelectedCoursesInstanceReviews(data2));
    } catch (error) {
      // 에러 처리
      console.error('Failed to fetch selected course:', error);
    }
  };

  const colors = {
    first: '#DF94C2',
    second: '#FFBDC1',
  };

  const rating = calcRating(course.ratingSum, course.ratingCount);

  const placeNames = course.places.map(place => place.name);

  return (
    <TouchableOpacity
      style={[
        styles.button,
        disabled && styles.disabled,
        buttonStyle,
        {
          backgroundColor:
            index && index % 2 === 1 ? colors.first : colors.second,
        },
      ]}
      onPress={handlePress}
      activeOpacity={0.7}
      disabled={disabled}>
      <View style={styles.imgContainer}>
        <ImageBackground
          source={{uri: `${course.places[0].image}`}}
          style={styles.img}></ImageBackground>
      </View>
      <ScrollView
        horizontal
        nestedScrollEnabled={true}
        showsHorizontalScrollIndicator={false}
        style={styles.textContainer}>
        {placeNames.map((placeName, index) => (
          <Text key={index} style={[styles.text, textStyle && textStyle]}>
            {placeName + ' - '}
          </Text>
        ))}
      </ScrollView>
      <StyledView>
        <RatingStar
          iconSize={12}
          iconStyle={{margin: -8}}
          rating={rating}
          activeColor="white"
          inactiveColor="grey"
        />
        <RatingText style={{paddingHorizontal: 3}}>{rating}</RatingText>
        <RatingText style={{fontSize: 8, marginTop: 4, marginRight: 10}}>
          ({course.ratingCount})
        </RatingText>
      </StyledView>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  button: {
    flexDirection: 'row',
    width: '90%',
    backgroundColor: '#FFBDC1',
    borderRadius: 3,
    alignItems: 'center',
    elevation: 4,
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  textContainer: {
    flexDirection: 'row',
    alignContent: 'center',
    marginHorizontal: '5%',
  },
  text: {
    fontFamily: 'SeoulNamsanM',
    color: 'white',
    fontSize: 12,
  },
  icon: {
    color: 'white',
    fontSize: 13,
  },
  disabled: {
    opacity: 0.6, // 투명도를 줄여서 비활성화된 것처럼 보이게 함
    // 다른 스타일도 추가할 수 있음
  },
  imgContainer: {
    height: 30,
    width: 30,
    borderRadius: 15,
    overflow: 'hidden',
  },
  img: {
    flex: 1,
    resizeMode: 'cover',
  },
});

const StyledView = styled(View)`
  flex-direction: row;
  align-items: center;
`;

const RatingText = styled(Text)`
  color: white;
  font-size: 13px;
  margin-left: 0px;
  margin-right: 0px;
`;

export default PopularCourseItem;
