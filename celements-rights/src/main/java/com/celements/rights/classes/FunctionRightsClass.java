package com.celements.rights.classes;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.list.single.GroupSingleListField;

@Component(FunctionRightsClass.CLASS_DEF_HINT)
public class FunctionRightsClass extends AbstractClassDefinition implements RightsClassDefinition {

  public static final String SPACE_NAME = "Celements";
  public static final String DOC_NAME = "FunctionRightsClass";
  public static final String CLASS_DEF_HINT = SPACE_NAME + "." + DOC_NAME;
  public static final ClassReference CLASS_REF = new ClassReference(SPACE_NAME, DOC_NAME);

  public static final ClassField<DocumentReference> FIELD_GROUP = new GroupSingleListField.Builder(
      CLASS_REF, "group").usesList(true).build();

  public FunctionRightsClass() {
    super(CLASS_REF);
  }

  @Override
  public boolean isInternalMapping() {
    return true;
  }

}
