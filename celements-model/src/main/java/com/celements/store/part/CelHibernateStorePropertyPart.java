package com.celements.store.part;

import java.io.Serializable;

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

//TODO CELDEV-626 - CelHibernateStore refactoring
public class CelHibernateStorePropertyPart {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelHibernateStore.class);

  private final CelHibernateStore store;

  public CelHibernateStorePropertyPart(CelHibernateStore store) {
    this.store = Preconditions.checkNotNull(store);
  }

  public void loadXWikiProperty(PropertyInterface property, XWikiContext context,
      boolean bTransaction) throws XWikiException {
    logXProperty("loadXProperty - start", property);
    try {
      if (bTransaction) {
        store.checkHibernate(context);
        bTransaction = store.beginTransaction(false, context);
      }
      Session session = store.getSession(context);

      try {
        session.load(property, (Serializable) property);
        // In Oracle, empty string are converted to NULL. Since an undefined property is not found
        // at all, it is safe to assume that a retrieved NULL value should actually be an empty
        // string.
        if (property instanceof BaseStringProperty) {
          BaseStringProperty stringProperty = (BaseStringProperty) property;
          if (stringProperty.getValue() == null) {
            stringProperty.setValue("");
          }
        }
      } catch (ObjectNotFoundException exc) {
        LOGGER.warn("loadXWikiProperty - no data for property: {}", property, exc);
      }

      // TODO: understand why collections are lazy loaded
      // Let's force reading lists if there is a list
      // This seems to be an issue since Hibernate 3.0
      // Without this test ViewEditTest.testUpdateAdvanceObjectProp fails
      if (property instanceof ListProperty) {
        ((ListProperty) property).getList();
      }

      if (bTransaction) {
        store.endTransaction(context, false, false);
      }
    } catch (Exception e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
          "Exception while loading property " + property, e);

    } finally {
      try {
        if (bTransaction) {
          store.endTransaction(context, false, false);
        }
      } catch (Exception e) {
        LOGGER.error("failed commit/rollback for {}", property, e);
      }
    }
    logXProperty("loadXProperty - end", property);
  }

  public void saveXWikiProperty(final PropertyInterface property, final XWikiContext context,
      final boolean runInOwnTransaction) throws XWikiException {
    logXProperty("saveXProperty - start", property);
    // Clone runInOwnTransaction so the value passed is not altered.
    boolean bTransaction = runInOwnTransaction;
    try {
      if (bTransaction) {
        store.checkHibernate(context);
        bTransaction = store.beginTransaction(context);
      }

      final Session session = store.getSession(context);

      final Query query = session.createQuery(
          "select prop.name from BaseProperty as prop where prop.id.id = :id and prop.id.name= :name");
      query.setLong("id", property.getId());
      query.setString("name", property.getName());

      if (query.uniqueResult() == null) {
        session.save(property);
      } else {
        session.update(property);
      }

      if (bTransaction) {
        store.endTransaction(context, true);
      }
    } catch (Exception e) {
      // Something went wrong, collect some information.
      String propertyStr = property.toString();

      // Try to roll back the transaction if this is in it's own transaction.
      try {
        if (bTransaction) {
          store.endTransaction(context, false);
        }
      } catch (Exception ee) {
        // Not a lot we can do here if there was an exception committing and an exception rolling
        // back.

        // not a lot sure, but at least f...ing log it!
        LOGGER.error("failed commit and rollback for {}", propertyStr, ee);
      }

      // Throw the exception.
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
          "Exception while saving property: " + propertyStr, e);
    }
    logXProperty("saveXProperty - end", property);
  }

  private void logXProperty(String msg, PropertyInterface property) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("{}: {}", msg, property);
    }
  }

}
