package com.celements.common.function;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Stream;

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
            .when(this::isInt)
            .thenMap(Integer::parseInt)
            .elseMap(Double::parseDouble)
            .stream());
    assertSum(10, numbers);
  }

  @Test
  public void test_noWhen() throws ParseException {
    Stream<Number> numbers = inputNumbers.stream()
        .flatMap(new ForkFunction<String, Number>()
            .thenMap(s -> 1)
            .elseMap(s -> 0)
            .stream());
    assertSum(inputNumbers.size(), numbers);
  }

  @Test
  public void test_noThen() {
    Stream<Number> numbers = inputNumbers.stream()
        .flatMap(new ForkFunction<String, Number>()
            .when(this::isInt)
            .elseMap(Double::parseDouble)
            .stream());
    assertSum(6, numbers);
  }

  @Test
  public void test_noElse() {
    Stream<Number> numbers = inputNumbers.stream()
        .flatMap(new ForkFunction<String, Number>()
            .when(this::isInt)
            .thenMap(Integer::parseInt)
            .stream());
    assertSum(4, numbers);
  }

  @Test
  public void test_elseFilter() throws ParseException {
    Stream<String> input = StreamEx.of(inputNumbers)
        .filter(this::isInt)
        .zipWith(Stream.of("W", "C"), (x, y) -> Stream.of(x, y))
        .flatMap(x -> x);
    StringBuilder letters = new StringBuilder();
    Stream<Number> numbers = input
        .flatMap(new ForkFunction<String, Number>()
            .when(this::isInt)
            .thenMap(Integer::parseInt)
            .elseFilter(letters::append)
            .stream());
    assertSum(4, numbers);
    assertEquals("WC", letters.toString());
  }

  private boolean isInt(String str) {
    return str.matches("\\d+");
  }

  private void assertSum(double sum, Stream<Number> numbers) {
    assertEquals(sum, numbers.mapToDouble(Number::doubleValue).sum(), 0);
  }

}
