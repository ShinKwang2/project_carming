package com.carming.backend.member.domain.valid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ValidNumbersTest {

    final int RANGE = 10;
    final int COUNT = 6;

    @Test
    @DisplayName("인증번호의 String 값 확인하기")
    void getValidTicketNumber() {
        //given
        List<Integer> numbers = ValidNumberFactory.pickNumbers(RANGE, COUNT);
        String randomNumber = numbers.stream()
                .map(String::valueOf)
                .reduce((x, y) -> x + y)
                .get();

        //when
        ValidNumbers validTicket = new ValidNumbers(numbers);
        String ticketNumber = validTicket.getValidNumbers();

        //then
        assertThat(ticketNumber).isEqualTo(randomNumber);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 7, -1, -100})
    @DisplayName("인증번호의 사이즈는 6이다")
    void validTicketSize(int count) {
        //given
        final int WRONG_COUNT = count;
        List<Integer> numbers = ValidNumberFactory.pickNumbers(RANGE, WRONG_COUNT);

        //expected
        assertThatThrownBy(() -> new ValidNumbers(numbers))
                .isInstanceOf(IllegalArgumentException.class);
    }
}