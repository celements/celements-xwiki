package com.celements.web.classes.oldcore;

import java.util.Date;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import com.celements.marshalling.Marshaller;
import com.celements.marshalling.XWikiUserMarshaller;
import com.celements.model.classes.PseudoClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.CustomStringField;
import com.celements.model.classes.fields.DateField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.classes.fields.number.IntField;
import com.celements.model.classes.fields.ref.DocumentReferenceField;
import com.xpn.xwiki.user.api.XWikiUser;

@Singleton
@Component(XWikiDocumentClass.CLASS_DEF_HINT)
public class XWikiDocumentClass extends PseudoClassDefinition {

  public static final String CLASS_NAME = "XWikiDocumentClass";
  public static final String CLASS_FN = CLASS_SPACE + "." + CLASS_NAME;
  public static final String CLASS_DEF_HINT = CLASS_FN;

  public static final ClassField<DocumentReference> FIELD_DOC_REF = new DocumentReferenceField.Builder(
      CLASS_FN, "documentReference").build();

  public static final ClassField<DocumentReference> FIELD_PARENT_REF = new DocumentReferenceField.Builder(
      CLASS_FN, "parentReference").build();

  public static final ClassField<String> FIELD_LANGUAGE = new StringField.Builder(CLASS_FN,
      "language").build();

  public static final ClassField<String> FIELD_DEFAULT_LANGUAGE = new StringField.Builder(CLASS_FN,
      "defaultLanguage").build();

  public static final ClassField<Integer> FIELD_TRANSLATION = new IntField.Builder(CLASS_FN,
      "translation").build();

  private static final Marshaller<XWikiUser> USER_MARSHALLER = new XWikiUserMarshaller();

  public static final ClassField<XWikiUser> FIELD_CREATOR = new CustomStringField.Builder<>(
      CLASS_FN, "creator", USER_MARSHALLER).build();

  public static final ClassField<XWikiUser> FIELD_AUTHOR = new CustomStringField.Builder<>(CLASS_FN,
      "author", USER_MARSHALLER).build();

  public static final ClassField<XWikiUser> FIELD_CONTENT_AUTHOR = new CustomStringField.Builder<>(
      CLASS_FN, "contentAuthor", USER_MARSHALLER).build();

  public static final ClassField<Date> FIELD_CREATION_DATE = new DateField.Builder(CLASS_FN,
      "creationDate").build();

  public static final ClassField<Date> FIELD_UPDATE_DATE = new DateField.Builder(CLASS_FN,
      "updateDate").build();

  public static final ClassField<Date> FIELD_CONTENT_UPDATE_DATE = new DateField.Builder(CLASS_FN,
      "contentUpdateDate").build();

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
