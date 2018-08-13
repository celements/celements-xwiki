package com.celements.common.reflect;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;
import org.xwiki.model.reference.EntityReference;

import com.celements.common.test.ExceptionAsserter;
import com.google.common.base.Supplier;

public class ReflectiveInstanceSupplierTest {

  @Test
  public void test_get() {
    Supplier<Date> supplier = new ReflectiveInstanceSupplier<>(Date.class);
    Date date = supplier.get();
    assertNotSame(supplier.get(), date);
  }

  @Test
  public void test_get_IAE() {
    Throwable cause = new ExceptionAsserter<IllegalArgumentException>(
        IllegalArgumentException.class) {

      @Override
      protected void execute() throws Exception {
        new ReflectiveInstanceSupplier<>(EntityReference.class).get();
      }
    }.evaluate().getCause();
    assertSame(InstantiationException.class, cause.getClass());
  }

  @Test
  public void test_NPE() {
    new ExceptionAsserter<NullPointerException>(NullPointerException.class) {

      @Override
      protected void execute() throws Exception {
        new ReflectiveInstanceSupplier<>(null);
      }
    }.evaluate();
  }

}
