package com.celements.store.part;

import static com.xpn.xwiki.XWikiException.*;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.celements.store.CelHibernateStore;
import com.google.common.base.Preconditions;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.PropertyInterface;

public class CelHibernateStorePropertyPart {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelHibernateStore.class);

  private final CelHibernateStore store;

  public CelHibernateStorePropertyPart(CelHibernateStore store) {
    this.store = Preconditions.checkNotNull(store);
  }

  public void loadXWikiProperty(final PropertyInterface property, final XWikiContext context,
      boolean bTransaction) throws XWikiException {
    StoreTransactionExecutor executor = new StoreTransactionExecutor(store) {

      @Override
      protected void call(Session session) throws HibernateException, XWikiException {
        session.load(property, (Serializable) property);
        executePostLoadActions(property);
      }
    };
    try {
      executor.withTransaction(bTransaction).execute(context);
    } catch (ObjectNotFoundException exc) {
      LOGGER.warn("loadXProperty - no data for {}: {}", property.getId(), property, exc);
    } catch (HibernateException | XWikiException exc) {
      throw new XWikiException(MODULE_XWIKI_STORE, ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
          "loadXProperty - failed for " + property.getId() + " :" + property, exc);
    }
  }

  private void executePostLoadActions(PropertyInterface property) {
    if (property instanceof BaseStringProperty) {
      // In Oracle, empty string are converted to NULL. Since an undefined property is not found
      // at all, it's safe to assume that a retrieved NULL value should actually be an empty string
      BaseStringProperty stringProperty = (BaseStringProperty) property;
      if (stringProperty.getValue() == null) {
        stringProperty.setValue("");
      }
    }
    if (property instanceof ListProperty) {
      // Force read list properties. This seems to be an issue since Hibernate 3.0. Without this
      // test ViewEditTest.testUpdateAdvanceObjectProp fails
      // TODO: understand why collections are lazy loaded.
      ((ListProperty) property).getList();
    }
  }

  public void saveXWikiProperty(final PropertyInterface property, final XWikiContext context,
      boolean bTransaction) throws XWikiException {
    StoreTransactionExecutor executor = new StoreTransactionExecutor(store) {

      @Override
      protected void call(Session session) throws HibernateException, XWikiException {
        updateOrSaveProperty(property, context);
      }
    };
    try {
      executor.withTransaction(bTransaction).withCommit().execute(context);
    } catch (HibernateException | XWikiException exc) {
      String propertyStr = property.getId() + " :" + property;
      throw new XWikiException(MODULE_XWIKI_STORE, ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT,
          "saveXProperty - failed for " + propertyStr, exc);
    }
  }

  private void updateOrSaveProperty(PropertyInterface property, XWikiContext context)
      throws HibernateException {
    Session session = store.getSession(context);
    if (existsProperty(property, session)) {
      session.update(property);
    } else {
      session.save(property);
    }
  }

  private boolean existsProperty(PropertyInterface property, Session session)
      throws HibernateException {
    Query query = session.createQuery("select prop.name from BaseProperty as prop "
        + "where prop.id.id = :id and prop.id.name= :name");
    query.setLong("id", property.getId());
    query.setString("name", property.getName());
    return query.uniqueResult() != null;
  }
}
