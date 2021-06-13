package com.celements.common.function;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static org.junit.Assert.*;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang.math.NumberUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import one.util.streamex.StreamEx;

public class ForkFunctionTest {

  private List<String> inputNumbers = ImmutableList.of("1", "2.0", "3", "4.0");

  @Before
  public void setUp() throws Exception {}

  @Test
  public void test_when_then_else() {
    Stream<Number> numbers = inputNumbers.stream()
        .flatMap(new ForkFunction<String, Number>()
            .when(NumberUtils::isDigits)
            .thenMap(Integer::parseInt)
            .elseMap(Double::parseDouble)
            .stream());
    assertSum(10, numbers);
  }

  @Test
  public void test_noWhen() {
    Stream<Number> numbers = inputNumbers.stream()
        .flatMap(new ForkFunction<String, Number>()
            .thenMap(Integer::parseInt)
            .elseMap(Double::parseDouble)
            .stream());
    assertSum(10, numbers);
  }

  @Test
  public void test_noThen() {
    Stream<Number> numbers = inputNumbers.stream()
        .flatMap(new ForkFunction<String, Number>()
            .when(NumberUtils::isDigits)
            .elseMap(Double::parseDouble)
            .stream());
    assertSum(6, numbers);
  }

  @Test
  public void test_noElse() {
    Stream<Number> numbers = inputNumbers.stream()
        .flatMap(new ForkFunction<String, Number>()
            .when(NumberUtils::isDigits)
            .thenMap(Integer::parseInt)
            .stream());
    assertSum(4, numbers);
  }

  @Test
  public void test_elseFilter() throws ParseException {
    Stream<String> input = StreamEx.of(inputNumbers).zipWith("ACDC".chars(),
        (letter, number) -> Stream.of(letter, number).map(Object::toString))
        .flatMap(x -> x);
    StringBuilder letters = new StringBuilder();
    Stream<Number> numbers = input
        .flatMap(new ForkFunction<String, Number>()
            .when(NumberUtils::isNumber)
            .thenMap(rethrowFunction(NumberFormat.getInstance()::parse))
            .elseFilter(letters::append)
            .stream());
    assertSum(4, numbers);
    assertEquals("ACDC", letters.toString());
  }

  private void assertSum(double sum, Stream<Number> numbers) {
    assertEquals(sum, numbers.mapToDouble(Number::doubleValue).sum(), 0);
  }

}
