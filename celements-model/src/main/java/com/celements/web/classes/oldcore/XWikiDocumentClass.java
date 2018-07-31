package com.celements.web.classes.oldcore;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.classes.PseudoClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.classes.fields.ref.DocumentReferenceField;

@Singleton
@Component(XWikiDocumentClass.CLASS_DEF_HINT)
public class XWikiDocumentClass extends PseudoClassDefinition {

  public static final String CLASS_NAME = "XWikiDocumentClass";
  public static final String CLASS_FN = CLASS_SPACE + "." + CLASS_NAME;
  public static final String CLASS_DEF_HINT = CLASS_FN;

  public static final ClassField<DocumentReference> FIELD_DOC_REF = new DocumentReferenceField.Builder(
      CLASS_FN, "docRef").build();

  public static final ClassField<String> FIELD_TITLE = new StringField.Builder(CLASS_FN,
      "title").build();

  public static final ClassField<String> FIELD_CONTENT = new StringField.Builder(CLASS_FN,
      "content").build();

  @Override
  public String getName() {
    return CLASS_FN;
  }

  @Override
  protected String getClassDocName() {
    return CLASS_NAME;
  }

}
