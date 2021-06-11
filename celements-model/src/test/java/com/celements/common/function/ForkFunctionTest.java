package com.celements.common.function;

import static org.junit.Assert.*;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang.math.NumberUtils;
import org.junit.Before;
import org.junit.Test;

public class ForkFunctionTest {

  @Before
  public void setUp() throws Exception {}

  @Test
  public void test() {
    Stream<String> input = Stream.of("1", "2.0", "3.0", "4");
    DoubleStream numbers = input
        .flatMap(new ForkFunction<String, Number>()
            .when(NumberUtils::isDigits)
            .thenMap(Integer::parseInt)
            .elseMap(Double::parseDouble)
            .stream())
        .mapToDouble(Number::doubleValue);
    assertEquals(10, numbers.sum(), 0);
  }

  @Test
  public void test_elseFilter() {
    Stream<String> input = Stream.of("1", "A", "2", "C", "D", "3", "C", "4");
    StringBuilder letters = new StringBuilder();
    IntStream numbers = input
        .flatMap(new ForkFunction<String, Integer>()
            .when(NumberUtils::isDigits)
            .thenMap(Integer::parseInt)
            .elseFilter(letters::append)
            .stream())
        .mapToInt(Integer::intValue);
    assertEquals(10, numbers.sum());
    assertEquals("ACDC", letters.toString());
  }

}
