package com.celements.web.classes.oldcore;

import java.util.Collections;
import java.util.List;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.list.StaticListField;

@Singleton
@Component(XWikiTagClass.CLASS_DEF_HINT)
public class XWikiTagClass extends AbstractClassDefinition implements IOldCoreClassDef {

  public static final String CLASS_NAME = "TagClass";
  public static final String CLASS_FN = CLASS_SPACE + "." + CLASS_NAME;
  public static final String CLASS_DEF_HINT = CLASS_FN;

  public static final ClassField<List<String>> FIELD_TAGS = new StaticListField.Builder(CLASS_FN,
      "tags").size(30).multiSelect(true).displayType("input").separator("|,").values(
          Collections.<String>emptyList())/* .relationalStorage(true) */.build();

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
