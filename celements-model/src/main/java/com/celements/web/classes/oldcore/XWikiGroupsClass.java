package com.celements.web.classes.oldcore;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.ref.DocumentReferenceField;

@Singleton
@Component(XWikiGroupsClass.CLASS_DEF_HINT)
public class XWikiGroupsClass extends AbstractClassDefinition implements IOldCoreClassDef {

  public static final String CLASS_NAME = "XWikiGroups";
  public static final String CLASS_FN = CLASS_SPACE + "." + CLASS_NAME;
  public static final String CLASS_DEF_HINT = CLASS_FN;

  public static final ClassField<DocumentReference> FIELD_MEMBER = new DocumentReferenceField.Builder(
      CLASS_FN, "member").prettyName("Member").size(30).build();

  @Override
  public String getName() {
    return CLASS_FN;
  }

  @Override
  public boolean isInternalMapping() {
    return false;
  }

  @Override
  protected String getClassSpaceName() {
    return CLASS_SPACE;
  }

  @Override
  protected String getClassDocName() {
    return CLASS_NAME;
  }

}
