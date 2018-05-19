package com.celements.web.classes.oldcore;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.BooleanField;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.StringField;

@Singleton
@Component(XWikiUsersClass.CLASS_DEF_HINT)
public class XWikiUsersClass extends AbstractClassDefinition implements IOldCoreClassDef {

  public static final String CLASS_NAME = "XWikiUsers";
  public static final String CLASS_FN = CLASS_SPACE + "." + CLASS_NAME;
  public static final String CLASS_DEF_HINT = CLASS_FN;

  public static final ClassField<String> FIELD_FIRST_NAME = new StringField.Builder(CLASS_FN,
      "first_name").prettyName("First Name").size(30).build();

  public static final ClassField<String> FIELD_LAST_NAME = new StringField.Builder(CLASS_FN,
      "last_name").prettyName("Last Name").size(30).build();

  public static final ClassField<String> FIELD_EMAIL = new StringField.Builder(CLASS_FN,
      "email").prettyName("e-Mail").size(30).build();

  public static final ClassField<String> FIELD_PASSWORD = new StringField.Builder(CLASS_FN,
      "password").prettyName("Password").size(10).build();

  public static final ClassField<String> FIELD_VALID_KEY = new StringField.Builder(CLASS_FN,
      "validkey").prettyName("Validation Key").size(10).build();

  public static final ClassField<Boolean> FIELD_ACTIVE = new BooleanField.Builder(CLASS_FN,
      "active").prettyName("Active").displayType("active").build();

  // XXX class is incomplete, extend if needed:
  // addTextField("default_language", "Default Language", 30);
  // addTextField("company", "Company", 30);
  // addTextField("blog", "Blog", 60);
  // addTextField("blogfeed", "Blog Feed", 60);
  // addTextAreaField("comment", "Comment", 40, 5);
  // addStaticListField("imtype", "IM Type", "---|AIM|Yahoo|Jabber|MSN|Skype|ICQ");
  // addTextField("imaccount", "imaccount", 30);
  // addStaticListField("editor", "Default Editor", "---|Text|Wysiwyg");
  // addStaticListField("usertype", "User type", "Simple|Advanced");
  // addBooleanField("accessibility", "Enable extra accessibility features", "yesno");
  // addTextField("skin", "skin", 30);
  // addStaticListField("pageWidth", "Preferred page width", "default|640|800|1024|1280|1600");
  // addTextField("avatar", "Avatar", 30);
  // addTextField("phone", "Phone", 30);
  // addTextAreaField("address", "Address", 40, 3);

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
