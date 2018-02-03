package com.celements.test;

import java.util.Arrays;
import java.util.List;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.classes.fields.list.StaticListField;

@Singleton
@Component(TestMultiSelectClass.CLASS_DEF_HINT)
public class TestMultiSelectClass extends AbstractClassDefinition {

  public static final String CLASS_SPACE = "Test";
  public static final String CLASS_NAME = "MultiSelectClass";
  public static final String CLASS_FN = CLASS_SPACE + "." + CLASS_NAME;
  public static final String CLASS_DEF_HINT = CLASS_FN;

  public static final ClassField<String> FIELD_STRING = new StringField.Builder(CLASS_FN,
      "mystring").build();

  public static final ClassField<List<String>> FIELD_LIST = new StaticListField.Builder(CLASS_FN,
      "mylist").multiSelect(true).size(5).values(Arrays.asList("A", "B", "C", "D")).build();

  public static final ClassField<String> FIELD_UNMAPPED_1 = new StringField.Builder(CLASS_FN,
      "unmapped1").build();

  public static final ClassField<String> FIELD_UNMAPPED_2 = new StringField.Builder(CLASS_FN,
      "unmapped2").build();

  @Override
  public String getName() {
    return CLASS_FN;
  }

  @Override
  public boolean isInternalMapping() {
    return true;
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
