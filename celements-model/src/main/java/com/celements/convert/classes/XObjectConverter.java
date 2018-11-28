package com.celements.convert.classes;

import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;

import com.celements.convert.ConversionException;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.field.FieldAccessException;
import com.celements.model.field.FieldAccessor;
import com.celements.model.field.XObjectFieldAccessor;
import com.celements.web.classes.oldcore.BaseObjectClass;
import com.celements.web.classes.oldcore.XWikiDocumentClass;
import com.xpn.xwiki.objects.BaseObject;

public abstract class XObjectConverter<T> extends AbstractClassDefConverter<BaseObject, T> {

  @Requirement(XObjectFieldAccessor.NAME)
  private FieldAccessor<BaseObject> xObjAccessor;

  @Override
  public FieldAccessor<BaseObject> getFromFieldAccessor() {
    return xObjAccessor;
  }

  @Override
  public T apply(T instance, BaseObject obj) throws ConversionException {
    instance = super.apply(instance, obj);
    if (obj != null) {
      try {
        ClassField<DocumentReference> docRefField = XWikiDocumentClass.FIELD_DOC_REF;
        getToFieldAccessor().setValue(instance, docRefField, obj.getDocumentReference());
        getToFieldAccessor().setValue(instance, BaseObjectClass.FIELD_CLASS_REF, new ClassReference(
            obj.getXClassReference()));
        getToFieldAccessor().setValue(instance, BaseObjectClass.FIELD_NUMBER, obj.getNumber());
      } catch (FieldAccessException exc) {
        handle(exc);
      }
    }
    return instance;
  }

}
