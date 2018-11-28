package com.celements.web.classes.oldcore;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ClassReference;

import com.celements.model.classes.PseudoClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.number.IntField;
import com.celements.model.classes.fields.ref.ClassReferenceField;

@Singleton
@Component(BaseObjectClass.CLASS_DEF_HINT)
public class BaseObjectClass extends PseudoClassDefinition {

  public static final String CLASS_NAME = "BaseObjectClass";
  public static final String CLASS_FN = CLASS_SPACE + "." + CLASS_NAME;
  public static final String CLASS_DEF_HINT = CLASS_FN;

  public static final ClassField<ClassReference> FIELD_CLASS_REF = new ClassReferenceField.Builder(
      CLASS_FN, "classReference").build();

  public static final ClassField<Integer> FIELD_NUMBER = new IntField.Builder(CLASS_FN,
      "number").build();

  @Override
  public String getName() {
    return CLASS_FN;
  }

  @Override
  protected String getClassDocName() {
    return CLASS_NAME;
  }

}
