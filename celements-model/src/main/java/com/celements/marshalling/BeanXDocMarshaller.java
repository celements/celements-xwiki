package com.celements.marshalling;

import static com.google.common.base.Preconditions.*;

import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.DocumentReference;

import com.celements.convert.bean.XDocBeanLoader;
import com.celements.convert.bean.XDocBeanLoader.BeanLoadException;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.object.restriction.ObjectRestriction;
import com.celements.model.util.ReferenceSerializationMode;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

/**
 * <p>
 * a utility class for marshalling a generic bean T for a given xdoc. It uses the
 * {@link ReferenceMarshaller} together with the {@link XDocBeanLoader} internally.
 * </p>
 * IMPORTANT: the bean must have a #getDocumentReference method defined in order for
 * {@link #serialize(T)} to work.
 *
 * @param <T>
 *          type of bean to marshall
 */
@Immutable
public final class BeanXDocMarshaller<T> extends AbstractMarshaller<T> {

  private final ClassIdentity classId;
  private final ReferenceMarshaller<DocumentReference> docRefMarshaller;
  private final List<ObjectRestriction<BaseObject>> restrictions;

  private XDocBeanLoader<T> loader;

  public static class Builder<T> {

    private final Class<T> token;
    private final ClassIdentity classId;
    private final ImmutableList.Builder<ObjectRestriction<BaseObject>> restrictions;
    private ReferenceSerializationMode mode;

    public Builder(@NotNull Class<T> token, @NotNull ClassIdentity classId) {
      this.token = token;
      this.classId = classId;
      this.restrictions = new ImmutableList.Builder<>();
    }

    public Builder<T> addRestriction(ObjectRestriction<BaseObject> restriction) {
      restrictions.add(restriction);
      return this;
    }

    public Builder<T> mode(ReferenceSerializationMode mode) {
      this.mode = mode;
      return this;
    }

    public BeanXDocMarshaller<T> build() {
      return new BeanXDocMarshaller<>(token, classId, new ReferenceMarshaller<>(
          DocumentReference.class, mode), restrictions.build());
    }

  }

  private BeanXDocMarshaller(@NotNull Class<T> token, ClassIdentity classId,
      @NotNull ReferenceMarshaller<DocumentReference> docRefMarshaller,
      @NotNull List<ObjectRestriction<BaseObject>> restrictions) {
    super(token);
    this.classId = checkNotNull(classId);
    this.docRefMarshaller = docRefMarshaller;
    this.restrictions = restrictions;
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
      try {
        instance = getBeanLoader().load(docRef.get(), restrictions);
      } catch (BeanLoadException exc) {
        LOGGER.info("unable to load bean '{}' for '{}'", getToken(), docRef, exc);
      }
    } else {
      LOGGER.info("unable to resolve doc for '{}'", val);
    }
    return Optional.fromNullable(instance);
  }

  @SuppressWarnings("unchecked")
  private XDocBeanLoader<T> getBeanLoader() {
    if (loader == null) {
      // init lazily due to static usages of Marshallers for ClassFields
      loader = Utils.getComponent(XDocBeanLoader.class);
      loader.initialize(getToken(), classId);
    }
    return loader;
  }

}
