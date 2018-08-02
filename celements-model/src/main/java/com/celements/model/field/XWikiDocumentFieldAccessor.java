package com.celements.model.field;

import static com.celements.web.classes.oldcore.XWikiDocumentClass.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.*;

import java.text.MessageFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.EntityReference;

import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * {@link FieldAccessor} for accessing {@link XWikiDocument} properties
 */
@Component(XWikiDocumentFieldAccessor.NAME)
public class XWikiDocumentFieldAccessor implements FieldAccessor<XWikiDocument> {

  private final static Logger LOGGER = LoggerFactory.getLogger(XWikiDocumentFieldAccessor.class);

  public static final String NAME = "xdoc";

  @Requirement(CLASS_DEF_HINT)
  private ClassDefinition xwikiDocPseudoClass;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public <V> Optional<V> getValue(XWikiDocument doc, ClassField<V> field)
      throws FieldAccessException {
    checkNotNull(doc);
    checkField(field);
    Optional<V> value = getDocFieldValue(doc, field);
    LOGGER.info("getValue: '{}' for '{}' from '{}'", value.orNull(), field,
        doc.getDocumentReference());
    return value;
  }

  @SuppressWarnings("unchecked")
  private <V> Optional<V> getDocFieldValue(XWikiDocument doc, ClassField<V> field) {
    V value;
    if (field == FIELD_DOC_REF) {
      value = (V) doc.getDocumentReference();
    } else if (field == FIELD_PARENT_REF) {
      value = (V) doc.getParentReference();
    } else if (field == FIELD_LANGUAGE) {
      value = (V) emptyToNull(doc.getLanguage());
    } else if (field == FIELD_DEFAULT_LANGUAGE) {
      value = (V) emptyToNull(doc.getDefaultLanguage());
    } else if (field == FIELD_TRANSLATION) {
      value = (V) (Boolean) (doc.getTranslation() != 0);
    } else if (field == FIELD_CREATOR) {
      value = (V) emptyToNull(doc.getCreator());
    } else if (field == FIELD_AUTHOR) {
      value = (V) emptyToNull(doc.getAuthor());
    } else if (field == FIELD_CONTENT_AUTHOR) {
      value = (V) emptyToNull(doc.getContentAuthor());
    } else if (field == FIELD_CREATION_DATE) {
      value = (V) doc.getCreationDate();
    } else if (field == FIELD_UPDATE_DATE) {
      value = (V) doc.getDate();
    } else if (field == FIELD_CONTENT_UPDATE_DATE) {
      value = (V) doc.getContentUpdateDate();
    } else if (field == FIELD_TITLE) {
      value = (V) emptyToNull(doc.getTitle().trim());
    } else if (field == FIELD_CONTENT) {
      value = (V) emptyToNull(doc.getContent().trim());
    } else {
      throw new IllegalArgumentException("undefined field: " + field);
    }
    return Optional.fromNullable(value);
  }

  @Override
  public <V> boolean setValue(XWikiDocument doc, ClassField<V> field, V value)
      throws FieldAccessException {
    checkNotNull(doc);
    checkField(field);
    boolean dirty = setDocFieldValue(doc, field, value);
    if (dirty) {
      LOGGER.info("setValue: '{}' for '{}' from '{}'", value, field, doc.getDocumentReference());
    }
    return dirty;
  }

  private <V> boolean setDocFieldValue(XWikiDocument doc, ClassField<V> field, V value) {
    boolean dirty = !Objects.equal(value, getDocFieldValue(doc, field).orNull());
    if (dirty) {
      if (field == FIELD_DOC_REF) {
        throw new IllegalArgumentException("docRef should never be set");
      } else if (field == FIELD_PARENT_REF) {
        doc.setParentReference((EntityReference) value);
      } else if (field == FIELD_LANGUAGE) {
        doc.setLanguage((String) value);
      } else if (field == FIELD_DEFAULT_LANGUAGE) {
        doc.setDefaultLanguage((String) value);
      } else if (field == FIELD_TRANSLATION) {
        doc.setTranslation((boolean) value ? 1 : 0);
      } else if (field == FIELD_CREATOR) {
        doc.setCreator((String) value);
      } else if (field == FIELD_AUTHOR) {
        doc.setAuthor((String) value);
      } else if (field == FIELD_CONTENT_AUTHOR) {
        doc.setContentAuthor((String) value);
      } else if (field == FIELD_CREATION_DATE) {
        doc.setCreationDate((Date) value);
      } else if (field == FIELD_UPDATE_DATE) {
        doc.setDate((Date) value);
      } else if (field == FIELD_CONTENT_UPDATE_DATE) {
        doc.setContentUpdateDate((Date) value);
      } else if (field == FIELD_TITLE) {
        doc.setTitle((String) value);
      } else if (field == FIELD_CONTENT) {
        doc.setContent((String) value);
      } else {
        throw new IllegalArgumentException("undefined field: " + field);
      }
    }
    return dirty;
  }

  private void checkField(ClassField<?> field) throws FieldAccessException {
    checkNotNull(field);
    if (!xwikiDocPseudoClass.equals(field.getClassDef())) {
      throw new FieldAccessException(MessageFormat.format(
          "uneligible for ''{0}'', it's of class ''{1}''", xwikiDocPseudoClass,
          field.getClassDef()));
    }
  }

}
