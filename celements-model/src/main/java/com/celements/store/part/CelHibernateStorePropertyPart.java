package com.celements.store.part;

import static com.google.common.base.Preconditions.*;

import java.io.Serializable;
import java.util.Date;

import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.celements.store.CelHibernateStore;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.PropertyInterface;

public class CelHibernateStorePropertyPart {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelHibernateStore.class);

  private final CelHibernateStore store;

  public CelHibernateStorePropertyPart(CelHibernateStore store) {
    this.store = checkNotNull(store);
  }

  public void loadXWikiProperty(PropertyInterface property, XWikiContext context,
      boolean bTransaction) throws XWikiException, HibernateException {
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
    } finally {
      if (bTransaction) {
        store.endTransaction(context, false, false);
      }
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
    } else if (property instanceof DateProperty) {
      // avoid returning java.sql.Timestamp since Timestamp.equals(Date) always returns false
      DateProperty dateProperty = ((DateProperty) property);
      Date value = (Date) dateProperty.getValue();
      if ((value != null) && (value.getClass() != Date.class)) {
        dateProperty.setValue(new Date(value.getTime()));
      }
    } else if (property instanceof ListProperty) {
      // Force read list properties. This seems to be an issue since Hibernate 3.0. Without this
      // test ViewEditTest.testUpdateAdvanceObjectProp fails
      // TODO: understand why collections are lazy loaded.
      ((ListProperty) property).getList();
    }
  }

  public void saveXWikiProperty(PropertyInterface property, XWikiContext context,
      boolean bTransaction) throws XWikiException, HibernateException {
    boolean commit = false;
    try {
      if (bTransaction) {
        store.checkHibernate(context);
        bTransaction = store.beginTransaction(context);
      }
      updateOrSaveProperty(property, context);
      commit = true;
    } finally {
      if (bTransaction) {
        store.endTransaction(context, commit);
      }
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
