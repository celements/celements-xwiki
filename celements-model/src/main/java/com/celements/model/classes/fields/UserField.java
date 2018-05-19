package com.celements.model.classes.fields;

import javax.validation.constraints.NotNull;

import com.celements.marshalling.XWikiUserMarshaller;
import com.xpn.xwiki.user.api.XWikiUser;

public class UserField extends CustomStringField<XWikiUser> {

  private static final XWikiUserMarshaller MARSHALLER = new XWikiUserMarshaller();

  public static class Builder extends CustomStringField.Builder<Builder, XWikiUser> {

    public Builder(@NotNull String classDefName, @NotNull String name) {
      super(classDefName, name, MARSHALLER);
    }

    @Override
    public UserField build() {
      return new UserField(getThis());
    }

  }

  protected UserField(@NotNull Builder builder) {
    super(builder);
  }

}
