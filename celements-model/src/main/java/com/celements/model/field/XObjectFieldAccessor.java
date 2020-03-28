package com.celements.model.field;

import static com.celements.web.classes.oldcore.XWikiObjectClass.*;
import static com.google.common.base.Preconditions.*;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.ClassReference;

import com.celements.model.access.IModelAccessFacade;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.google.common.base.Optional;
import com.xpn.xwiki.objects.BaseObject;

/**
 * {@link FieldAccessor} for accessing {@link BaseObject} properties
 */
@Component(XObjectFieldAccessor.NAME)
public class XObjectFieldAccessor implements FieldAccessor<BaseObject> {

  private final static Logger LOGGER = LoggerFactory.getLogger(XObjectFieldAccessor.class);

  public static final String NAME = "xobject";

  @Requirement(CLASS_DEF_HINT)
  private ClassDefinition xObjClassDef;

  @Requirement
  private IModelAccessFacade modelAccess;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public <V> Optional<V> getValue(BaseObject obj, ClassField<V> field) throws FieldAccessException {
    Optional<V> value;
    if (field.getClassDef().equals(xObjClassDef)) {
      value = Optional.of(getXObjFieldValue(obj, field));
    } else {
      checkClassRef(obj, field);
      value = modelAccess.getFieldValue(obj, field);
    }
    LOGGER.info("getValue: '{}' for '{}' from '{} - {} - {}'", value.orNull(), field,
        obj.getDocumentReference(), obj.getXClassReference(), obj.getNumber());
    return value;
  }

  @SuppressWarnings("unchecked")
  private <V> V getXObjFieldValue(BaseObject obj, ClassField<V> field) throws FieldAccessException {
    V value;
    if (field == FIELD_ID) {
      value = (V) (Long) obj.getId();
    } else if (field == FIELD_DOC_REF) {
      value = (V) obj.getDocumentReference();
    } else if (field == FIELD_CLASS_REF) {
      value = (V) new ClassReference(obj.getXClassReference());
    } else if (field == FIELD_NUMBER) {
      value = (V) (Integer) obj.getNumber();
    } else {
      throw new FieldAccessException("undefined field: " + field);
    }
    return value;
  }

  @Override
  public <V> boolean setValue(BaseObject obj, ClassField<V> field, V value)
      throws FieldAccessException {
    checkClassRef(obj, field);
    boolean dirty = modelAccess.setProperty(obj, field, value);
    if (dirty) {
      LOGGER.info("setValue: '{}' for '{}' from '{} - {} - {}'", value, field,
          obj.getDocumentReference(), obj.getXClassReference(), obj.getNumber());
    }
    return dirty;
  }

  private void checkClassRef(BaseObject obj, ClassField<?> field) throws FieldAccessException {
    checkNotNull(obj);
    checkNotNull(field);
    if (!field.getClassDef().isValidObjectClass()) {
      throw new FieldAccessException(MessageFormat.format(
          "BaseObject uneligible for pseudo class field ''{0}''", field));
    }
    ClassReference classRef = new ClassReference(obj.getXClassReference());
    if (!classRef.equals(field.getClassDef().getClassReference())) {
      throw new FieldAccessException(MessageFormat.format(
          "BaseObject uneligible for ''{0}'', it's of class ''{1}''", field, classRef));
    }
  }

}
