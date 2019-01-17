package com.celements.marshalling;

import static com.google.common.base.Preconditions.*;

import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.DocumentReference;

import com.celements.convert.ConversionException;
import com.celements.convert.bean.BeanClassDefConverter;
import com.celements.convert.bean.XObjectBeanConverter;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.object.ObjectBridge;
import com.celements.model.object.restriction.ClassRestriction;
import com.celements.model.object.restriction.ObjectRestriction;
import com.celements.model.object.xwiki.XWikiObjectBridge;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.util.ModelUtils;
import com.celements.model.util.ReferenceSerializationMode;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

@Immutable
public final class BeanXObjMarshaller<T> extends AbstractMarshaller<T> {

  private final ClassIdentity classId;
  private final ReferenceMarshaller<DocumentReference> docRefMarshaller;
  private final List<ObjectRestriction<BaseObject>> xObjRestrictions;

  private BeanClassDefConverter<BaseObject, T> converter;

  public static class Builder<T> {

    private final Class<T> token;
    private final ClassIdentity classId;
    private final ImmutableList.Builder<ObjectRestriction<BaseObject>> xObjRestrictions;
    private ReferenceSerializationMode mode;

    public Builder(@NotNull Class<T> token, @NotNull ClassIdentity classId) {
      this.token = token;
      this.classId = classId;
      this.xObjRestrictions = new ImmutableList.Builder<>();
      xObjRestrictions.add(new ClassRestriction<>(getXObjectBridge(), classId));
    }

    public Builder<T> addRestriction(ObjectRestriction<BaseObject> restriction) {
      xObjRestrictions.add(restriction);
      return this;
    }

    public Builder<T> mode(ReferenceSerializationMode mode) {
      this.mode = mode;
      return this;
    }

    public BeanXObjMarshaller<T> build() {
      return new BeanXObjMarshaller<>(token, classId, new ReferenceMarshaller<>(
          DocumentReference.class, mode), xObjRestrictions.build());
    }

  }

  private BeanXObjMarshaller(@NotNull Class<T> token, ClassIdentity classId,
      @NotNull ReferenceMarshaller<DocumentReference> docRefMarshaller,
      @NotNull List<ObjectRestriction<BaseObject>> xObjRestrictions) {
    super(token);
    this.classId = checkNotNull(classId);
    this.docRefMarshaller = docRefMarshaller;
    this.xObjRestrictions = xObjRestrictions;
  }

  @Override
  public String serialize(T val) {
    try {
      return docRefMarshaller.serialize(getDocRefInvokable().invoke(val));
    } catch (ReflectiveOperationException | ClassCastException exc) {
      throw new IllegalArgumentException("unable to serialize provided entity, "
          + "requiring 'getDocumentReference' invokable on it: " + getToken(), exc);
    }
  }

  @SuppressWarnings("unchecked")
  private Invokable<T, DocumentReference> getDocRefInvokable() throws NoSuchMethodException {
    Method method = getToken().getMethod("getDocumentReference");
    Invokable<T, ?> invokable = TypeToken.of(getToken()).method(method);
    return (Invokable<T, DocumentReference>) invokable;
  }

  @Override
  public Optional<T> resolve(String val) {
    checkNotNull(val);
    T instance = null;
    Optional<BaseObject> xObj = resolveXObject(val);
    if (xObj.isPresent()) {
      try {
        instance = getXObjToBeanConverter().apply(xObj.get());
      } catch (ConversionException exc) {
        LOGGER.info("unable to convert '{}' to '{}'", xObj, getToken(), exc);
      }
    } else {
      LOGGER.info("unable to resolve xObj for '{}' and filters '{}'", val, xObjRestrictions);
    }
    return Optional.fromNullable(instance);
  }

  private Optional<BaseObject> resolveXObject(String val) {
    Optional<BaseObject> xObj = Optional.absent();
    Optional<DocumentReference> docRef = docRefMarshaller.resolve(val);
    if (docRef.isPresent()) {
      XWikiDocument doc = getModelAccess().getOrCreateDocument(docRef.get());
      xObj = XWikiObjectFetcher.on(doc).filter(xObjRestrictions).first();
    }
    return xObj;
  }

  @SuppressWarnings("unchecked")
  private BeanClassDefConverter<BaseObject, T> getXObjToBeanConverter() {
    if (converter == null) {
      converter = Utils.getComponent(BeanClassDefConverter.class, XObjectBeanConverter.NAME);
      converter.initialize(getToken());
      converter.initialize(getClassDefinition());
    }
    return converter;
  }

  private ClassDefinition getClassDefinition() {
    String classDefHint = getModelUtils().serializeRefLocal(classId.getDocRef());
    return Utils.getComponent(ClassDefinition.class, classDefHint);
  }

  private static XWikiObjectBridge getXObjectBridge() {
    return (XWikiObjectBridge) Utils.getComponent(ObjectBridge.class, XWikiObjectBridge.NAME);
  }

  private static IModelAccessFacade getModelAccess() {
    return Utils.getComponent(IModelAccessFacade.class);
  }

  private static ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

}
