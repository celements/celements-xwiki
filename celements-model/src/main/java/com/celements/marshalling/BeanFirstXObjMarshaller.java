package com.celements.marshalling;

import static com.google.common.base.Preconditions.*;

import java.lang.reflect.Method;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.DocumentReference;

import com.celements.convert.ConversionException;
import com.celements.convert.bean.BeanClassDefConverter;
import com.celements.convert.bean.XObjectBeanConverter;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.util.ModelUtils;
import com.celements.model.util.ReferenceSerializationMode;
import com.google.common.base.Optional;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

@Immutable
public final class BeanFirstXObjMarshaller<T> extends AbstractMarshaller<T> {

  private final ClassIdentity classId;
  private final ReferenceMarshaller<DocumentReference> docRefMarshaller;

  private BeanClassDefConverter<BaseObject, T> converter;

  public BeanFirstXObjMarshaller(@NotNull Class<T> token, ClassIdentity classId) {
    this(token, classId, null);
  }

  public BeanFirstXObjMarshaller(@NotNull Class<T> token, ClassIdentity classId,
      @Nullable ReferenceSerializationMode mode) {
    super(token);
    this.classId = checkNotNull(classId);
    this.docRefMarshaller = new ReferenceMarshaller<>(DocumentReference.class, mode);
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
    Optional<DocumentReference> docRef = docRefMarshaller.resolve(val);
    if (docRef.isPresent()) {
      Optional<BaseObject> obj = XWikiObjectFetcher.on(getModelAccess().getOrCreateDocument(
          docRef.get())).filter(classId).first();
      if (obj.isPresent()) {
        try {
          instance = getConverter().apply(obj.get());
        } catch (ConversionException exc) {
          LOGGER.info("unable to convert '{}' to '{}'", val, getToken());
        }
      }
    }
    return Optional.fromNullable(instance);
  }

  @SuppressWarnings("unchecked")
  private BeanClassDefConverter<BaseObject, T> getConverter() {
    if (converter == null) {
      converter = Utils.getComponent(BeanClassDefConverter.class, XObjectBeanConverter.NAME);
      converter.initialize(checkNotNull(getToken()));
      ClassDefinition classDef = Utils.getComponent(ClassDefinition.class,
          getModelUtils().serializeRefLocal(classId.getDocRef()));
      converter.initialize(checkNotNull(classDef));
    }
    return converter;
  }

  protected static IModelAccessFacade getModelAccess() {
    return Utils.getComponent(IModelAccessFacade.class);
  }

  protected static ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

}
