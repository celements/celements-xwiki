package com.celements.convert.bean;

import static com.google.common.base.MoreObjects.*;
import static java.text.MessageFormat.*;

import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;

import com.celements.convert.ConversionException;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.object.restriction.ObjectRestriction;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.util.ModelUtils;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultXDocBeanLoader<T> implements XDocBeanLoader<T> {

  protected static final Logger LOGGER = LoggerFactory.getLogger(XDocBeanLoader.class);

  @Requirement
  private ModelUtils modelUtils;

  @Requirement(XObjectBeanConverter.NAME)
  private BeanClassDefConverter<BaseObject, T> converter;

  @Override
  public void initialize(Class<T> token, ClassIdentity classId) {
    converter.initialize(token);
    converter.initialize(getClassDefinition(classId));
  }

  private ClassDefinition getClassDefinition(ClassIdentity classId) {
    if (classId instanceof ClassDefinition) {
      return (ClassDefinition) classId;
    } else {
      return Utils.getComponent(ClassDefinition.class, modelUtils.serializeRefLocal(
          classId.getDocRef()));
    }
  }

  private ClassIdentity getClassId() {
    return converter.getClassDef();
  }

  @Override
  public T load(XWikiDocument doc) throws BeanLoadException {
    return load(doc, null);
  }

  @Override
  public T load(XWikiDocument doc, Collection<ObjectRestriction<BaseObject>> restrictions)
      throws BeanLoadException {
    XWikiObjectFetcher fetcher = XWikiObjectFetcher.on(doc).filter(getClassId()).filter(
        firstNonNull(restrictions, Collections.<ObjectRestriction<BaseObject>>emptyList()));
    try {
      return converter.apply(fetcher.firstAssert());
    } catch (IllegalArgumentException | ConversionException exc) {
      String msg = format("given doc [{0}] invalid for loading [{1}]", doc, getClassId());
      throw new BeanLoadException(msg, exc);
    }
  }

}
