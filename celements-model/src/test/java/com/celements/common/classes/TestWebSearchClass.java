package com.celements.common.classes;

import java.util.List;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.celements.marshalling.ReferenceMarshaller;
import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.BooleanField;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.classes.fields.list.CustomListField;
import com.celements.model.classes.fields.list.StringListField;
import com.celements.model.classes.fields.number.FloatField;
import com.google.common.base.Splitter;

public class TestWebSearchClass extends AbstractClassDefinition {

  public static final String SPACE_NAME = "Celements2";
  public static final String DOC_NAME = "WebSearchConfigClass";
  public static final String CLASS_DEF_HINT = SPACE_NAME + "." + DOC_NAME;

  public static ClassField<List<String>> FIELD_PACKAGES = new StringListField.Builder(
      CLASS_DEF_HINT, "packages").values(Splitter.on("|").splitToList(
          "content|attachment|menu|chronoblog|image|blog")).multiSelect(true).separator(
              ",").build();

  public static ClassField<Boolean> FIELD_LINKED_DOCS_ONLY = new BooleanField.Builder(
      CLASS_DEF_HINT, "linkedDocsOnly").displayType("yesno").build();

  public static ClassField<Float> FIELD_FUZZY_SEARCH = new FloatField.Builder(CLASS_DEF_HINT,
      "fuzzySearch").build();

  public static ClassField<List<DocumentReference>> FIELD_DOCS = new CustomListField.Builder<>(
      CLASS_DEF_HINT, "docs", new ReferenceMarshaller<>(DocumentReference.class)).multiSelect(
          true).separator(",").build();

  public static ClassField<List<DocumentReference>> FIELD_DOCS_BLACK_LIST = new CustomListField.Builder<>(
      CLASS_DEF_HINT, "docsBlackList", new ReferenceMarshaller<>(
          DocumentReference.class)).multiSelect(true).separator(",").build();

  public static ClassField<List<SpaceReference>> FIELD_SPACES = new CustomListField.Builder<>(
      CLASS_DEF_HINT, "spaces", new ReferenceMarshaller<>(SpaceReference.class)).multiSelect(
          true).separator(",").build();

  public static ClassField<List<SpaceReference>> FIELD_SPACES_BLACK_LIST = new CustomListField.Builder<>(
      CLASS_DEF_HINT, "spacesBlackList", new ReferenceMarshaller<>(
          SpaceReference.class)).multiSelect(true).separator(",").build();

  public static ClassField<List<String>> FIELD_PAGETYPES = new StringListField.Builder<>(
      CLASS_DEF_HINT, "pageTypes").multiSelect(true).separator(",").build();

  public static ClassField<List<String>> FIELD_PAGETYPES_BLACK_LIST = new StringListField.Builder<>(
      CLASS_DEF_HINT, "pageTypesBlackList").multiSelect(true).separator(",").build();

  public static ClassField<List<String>> FIELD_SORT_FIELDS = new StringListField.Builder(
      CLASS_DEF_HINT, "sortFields").multiSelect(true).separator(",").build();

  public static ClassField<String> FIELD_RESULT_ITEM_RENDER_SCRIPT = new StringField.Builder(
      CLASS_DEF_HINT, "resultItemRenderScript").build();

  public static ClassField<Boolean> FIELD_ADVANCED_SEARCH = new BooleanField.Builder(CLASS_DEF_HINT,
      "advancedSearch").displayType("yesno").defaultValue(0).build();

  @Override
  public String getName() {
    return CLASS_DEF_HINT;
  }

  @Override
  public boolean isInternalMapping() {
    return false;
  }

  @Override
  protected String getClassSpaceName() {
    return SPACE_NAME;
  }

  @Override
  protected String getClassDocName() {
    return DOC_NAME;
  }

}
