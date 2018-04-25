package com.celements.store.part;

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

  public void loadXWikiProperty(PropertyInterface property, XWikiContext context,
      boolean bTransaction) throws XWikiException {
    logXProperty("loadXProperty - start", property);
    try {
      if (bTransaction) {
        store.checkHibernate(context);
        bTransaction = store.beginTransaction(false, context);
      }
      Session session = store.getSession(context);
      session.load(property, (Serializable) property);
      executePostLoadActions(property);
    } catch (ObjectNotFoundException exc) {
      LOGGER.warn("loadXProperty - no data for {}: {}", property.getId(), property, exc);
    } catch (HibernateException | XWikiException exc) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT, "loadXProperty - failed for "
              + property.getId() + " :" + property, exc);
    } finally {
      try {
        if (bTransaction) {
          store.endTransaction(context, false, false);
        }
      } catch (HibernateException exc) {
        LOGGER.error("loadXProperty - failed rollback for {}: {}", property.getId(), property, exc);
      }
    }
    logXProperty("loadXProperty - end", property);
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

  public void saveXWikiProperty(PropertyInterface property, XWikiContext context,
      boolean bTransaction) throws XWikiException {
    logXProperty("saveXProperty - start", property);
    boolean commit = false;
    try {
      if (bTransaction) {
        store.checkHibernate(context);
        bTransaction = store.beginTransaction(context);
      }
      updateOrSaveProperty(property, context);
      commit = true;
    } catch (HibernateException | XWikiException exc) {
      // something went wrong, collect some information
      String propertyStr = property.getId() + " :" + property;
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT, "saveXProperty - failed for "
              + propertyStr, exc);
    } finally {
      try {
        if (bTransaction) {
          store.endTransaction(context, commit);
        }
      } catch (HibernateException exc) {
        LOGGER.error("saveXProperty - failed {} for {}: {}", (commit ? "commit" : "rollback"),
            property.getId(), property, exc);
      }
    }
    logXProperty("saveXProperty - end", property);
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

  private void logXProperty(String msg, PropertyInterface property) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("{}: {}", msg, property);
    }
  }

}
