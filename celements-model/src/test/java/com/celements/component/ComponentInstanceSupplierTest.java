package com.celements.component;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.ExceptionAsserter;
import com.celements.model.access.IModelAccessFacade;
import com.celements.store.DocumentCacheStore;
import com.google.common.base.Supplier;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

public class ComponentInstanceSupplierTest extends AbstractComponentTest {

  @Test
  public void test_get() {
    Supplier<IModelAccessFacade> supplier = new ComponentInstanceSupplier<>(
        IModelAccessFacade.class);
    IModelAccessFacade modelAccess = supplier.get();
    assertSame(Utils.getComponent(IModelAccessFacade.class), modelAccess);
    assertSame(supplier.get(), modelAccess);
  }

  @Test
  public void test_get_withHint() {
    Supplier<XWikiStoreInterface> supplier = new ComponentInstanceSupplier<>(
        XWikiStoreInterface.class, DocumentCacheStore.COMPONENT_NAME);
    XWikiStoreInterface store = supplier.get();
    assertSame(Utils.getComponent(XWikiStoreInterface.class, DocumentCacheStore.COMPONENT_NAME),
        store);
    assertSame(supplier.get(), store);
  }

  @Test
  public void test_get_notComponent() {
    Throwable cause = new ExceptionAsserter<IllegalArgumentException>(
        IllegalArgumentException.class) {

      @Override
      protected void execute() throws Exception {
        new ComponentInstanceSupplier<>(Date.class).get();
      }
    }.evaluate().getCause();
    assertSame(ComponentLookupException.class, cause.getClass());
  }

  @Test
  public void test_NPE() {
    new ExceptionAsserter<NullPointerException>(NullPointerException.class) {

      @Override
      protected void execute() throws Exception {
        new ComponentInstanceSupplier<>(null);
      }
    }.evaluate();
  }

}
