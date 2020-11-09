package com.celements.model.field.internal;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.ClassReference;

import com.celements.model.context.ModelContext;
import com.celements.model.field.FieldAccessException;
import com.celements.model.field.FieldAccessor;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.xpn.xwiki.XWikiException;
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
          .flatMap(o -> getBaseProperty(o, fieldName))
          .flatMap(this::getAndNormalizeValue);
    } catch (ClassCastException exc) {
      throw createException("failed to cast value", obj, fieldName, exc);
    }
  }

  private Optional<BaseProperty> getBaseProperty(BaseObject obj, String fieldName) {
    try {
      return Optional.ofNullable((BaseProperty) obj.get(fieldName));
    } catch (XWikiException | ClassCastException exc) {
      // shouldn't happen since XWE is never thrown in BaseObject.get()
      // and BaseObjects should only ever contain BaseProperties
      throw createException("should not happen", obj, fieldName, exc);
    }
  }

  private Optional<Object> getAndNormalizeValue(BaseProperty prop) {
    Object value = prop.getValue();
    if (value instanceof String) {
      // avoid comparing empty string to null
      value = Strings.emptyToNull(value.toString().trim());
    } else if (value instanceof Date) {
      // avoid returning Timestamp since Timestamp.equals(Date) always returns false
      value = new Date(((Date) value).getTime());
    }
    return Optional.ofNullable(value);
  }

  @Override
  public boolean set(BaseObject obj, String fieldName, Object newValue) {
    Object currentValue = get(obj, fieldName).orElse(null);
    if (!Objects.equal(newValue, currentValue)) {
      try {
        obj.set(fieldName, normalizeValue(newValue), context.getXWikiContext());
        return true;
      } catch (ClassCastException exc) {
        throw createException("failed to set value '" + newValue + "'", obj, fieldName, exc);
      }
    }
    return false;
  }

  private Object normalizeValue(Object value) {
    if (value instanceof Collection) {
      value = Joiner.on('|').join((Collection<?>) value);
    }
    return value;
  }

  private FieldAccessException createException(String message, BaseObject obj, String fieldName,
      Throwable cause) {
    return new FieldAccessException(message + " on field '" + new ClassReference(
        obj.getXClassReference()).serialize() + "." + fieldName + "'", cause);
  }

}
