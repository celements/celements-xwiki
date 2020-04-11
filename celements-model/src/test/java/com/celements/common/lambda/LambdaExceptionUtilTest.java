package com.celements.common.lambda;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static java.util.stream.Collectors.*;

import java.util.stream.Stream;

import org.junit.Test;

public class LambdaExceptionUtilTest {

  @Test
  public void test_Function_rethrow() throws ClassNotFoundException {
    Stream.of(Object.class.getName()).map(rethrowFunction(Class::forName)).collect(toList());
  }

  @Test(expected = ClassNotFoundException.class)
  public void test_Function_rethrow_fail() throws ClassNotFoundException {
    Stream.of("INVALID").map(rethrowFunction(Class::forName)).collect(toList());
  }

  @Test
  public void test_Predicate_rethrow() throws ClassNotFoundException {
    Stream.of(Object.class.getName()).filter(rethrowPredicate(n -> (Class.forName(n) != null)))
        .collect(toList());
  }

  @Test(expected = ClassNotFoundException.class)
  public void test_Predicate_rethrow_fail() throws ClassNotFoundException {
    Stream.of("INVALID").filter(rethrowPredicate(n -> (Class.forName(n) != null)))
        .collect(toList());
  }

  @Test
  public void test_Consumer_rethrow() throws ClassNotFoundException {
    Stream.of(Object.class.getName()).forEach(rethrowConsumer(Class::forName));
  }

  @Test(expected = ClassNotFoundException.class)
  public void test_Consumer_rethrow_fail() throws ClassNotFoundException {
    Stream.of("INVALID").forEach(rethrowConsumer(Class::forName));
  }

  @Test
  public void test_Supplier_rethrow() throws ClassNotFoundException {
    rethrowSupplier(() -> Class.forName(Object.class.getName())).get();
  }

  @Test(expected = ClassNotFoundException.class)
  public void test_Supplier_rethrow_fail() throws ClassNotFoundException {
    rethrowSupplier(() -> Class.forName("INVALID")).get();
  }

  @Test
  public void test_Runnable_rethrow() throws ClassNotFoundException {
    rethrowRunnable(() -> Class.forName(Object.class.getName())).run();
  }

  @Test(expected = ClassNotFoundException.class)
  public void test_Runnable_rethrow_fail() throws ClassNotFoundException {
    rethrowRunnable(() -> Class.forName("INVALID")).run();
  }

}
