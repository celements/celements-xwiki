package com.celements.model.field.internal;

import static com.google.common.base.Strings.*;

import java.util.Collection;
import java.util.Optional;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.ClassReference;

import com.celements.model.context.ModelContext;
import com.celements.model.field.FieldAccessException;
import com.celements.model.field.FieldAccessor;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * {@link FieldAccessor} for accessing {@link BaseObject} properties
 */
@Component(XObjectInternalFieldAccessor.NAME)
public class XObjectInternalFieldAccessor implements InternalFieldAccessor<BaseObject> {

  public static final String NAME = "xobject";

  @Requirement
  private ModelContext context;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Optional<Object> get(BaseObject obj, String fieldName) {
    try {
      return Optional.ofNullable(obj)
          .flatMap(o -> getAndNormalizeValue(o, fieldName));
    } catch (ClassCastException | IllegalArgumentException exc) {
      throw createException("failed to get value", obj, fieldName, exc);
    }
  }

  private Optional<Object> getAndNormalizeValue(BaseObject obj, String fieldName) {
    return Optional.ofNullable((BaseProperty) obj.safeget(fieldName))
        .map(BaseProperty::getValue)
        .map(value -> (value instanceof String)
            ? emptyToNull(value.toString().trim()) // avoid comparing empty string to null
            : value);
  }

  @Override
  public boolean set(BaseObject obj, String fieldName, Object newValue) {
    Object currentValue = get(obj, fieldName).orElse(null);
    if (!Objects.equal(newValue, currentValue)) {
      try {
        normalizeAndSetValue(obj, fieldName, newValue);
        return true;
      } catch (ClassCastException | IllegalArgumentException exc) {
        throw createException("failed to set value '" + newValue + "'", obj, fieldName, exc);
      }
    }
    return false;
  }

  private void normalizeAndSetValue(BaseObject obj, String fieldName, Object value) {
    if (value instanceof Collection) {
      value = Joiner.on('|').join((Collection<?>) value);
    }
    obj.set(fieldName, value, context.getXWikiContext());
  }

  private FieldAccessException createException(String message, BaseObject obj, String fieldName,
      Throwable cause) {
    return new FieldAccessException(message + " on field '" + new ClassReference(
        obj.getXClassReference()).serialize() + "." + fieldName + "'", cause);
  }

}
