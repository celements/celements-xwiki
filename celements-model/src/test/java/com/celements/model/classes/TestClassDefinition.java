package com.celements.model.classes;

import java.util.List;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.classes.fields.BooleanField;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.classes.fields.list.StringListField;
import com.celements.model.classes.fields.list.single.StringSingleListField;
import com.celements.model.classes.fields.number.IntField;
import com.celements.model.classes.fields.ref.DocumentReferenceField;

@Singleton
@Component(TestClassDefinition.NAME)
public class TestClassDefinition extends AbstractClassDefinition implements
    TestClassDefinitionRole {

  public static final String SPACE_NAME = "Classes";
  public static final String DOC_NAME = "TestClass";
  public static final String NAME = SPACE_NAME + "." + DOC_NAME;
  public static final ClassReference CLASS_REF = new ClassReference(SPACE_NAME, DOC_NAME);

  public static final ClassField<String> FIELD_MY_STRING = getFieldMyString();
  public static final ClassField<Integer> FIELD_MY_INT = getFieldMyInt();
  public static final ClassField<Boolean> FIELD_MY_BOOL = getFieldMyBool();
  public static final ClassField<DocumentReference> FIELD_MY_DOCREF = getFieldMyDocRef();
  public static final ClassField<List<String>> FIELD_MY_LIST_MS = getFieldMyList(true);
  public static final ClassField<List<String>> FIELD_MY_LIST_SS = getFieldMyList(false);
  public static final ClassField<String> FIELD_MY_SINGLE_LIST = getFieldMySingleList();
  public static final ClassField<String> FIELD_LANG = new StringField.Builder(CLASS_REF, "lang")
      .build();

  public TestClassDefinition() {
    super(CLASS_REF);
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean isInternalMapping() {
    return true;
  }

  private static ClassField<String> getFieldMyString() {
    return new StringField.Builder(CLASS_REF, "myString").size(30).build();
  }

  private static ClassField<Integer> getFieldMyInt() {
    return new IntField.Builder(CLASS_REF, "myInt").size(30).build();
  }

  private static ClassField<Boolean> getFieldMyBool() {
    return new BooleanField.Builder(CLASS_REF, "myBool").displayType("asdf").build();
  }

  private static ClassField<DocumentReference> getFieldMyDocRef() {
    return new DocumentReferenceField.Builder(CLASS_REF, "myDocRef").size(30).build();
  }

  private static ClassField<List<String>> getFieldMyList(boolean multiSelect) {
    return new StringListField.Builder<>(CLASS_REF, "myList" + (multiSelect ? "MS" : "SS"))
        .multiSelect(multiSelect).build();
  }

  private static ClassField<String> getFieldMySingleList() {
    return new StringSingleListField.Builder(CLASS_REF, "mySingleList").build();
  }

}
