package com.celements.model.classes.fields.list;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import com.celements.marshalling.AccessLevelMarshaller;
import com.celements.rights.access.EAccessLevel;
import com.xpn.xwiki.objects.classes.LevelsClass;

@Immutable
public final class AccessRightLevelsField extends EnumListField<EAccessLevel> {

  public static final String DEFAULT_SEPARATOR = ",|";

  public static class Builder extends EnumListField.Builder<EAccessLevel> {

    public Builder(@NotNull String classDefName, @NotNull String name) {
      super(classDefName, name, new AccessLevelMarshaller());
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public AccessRightLevelsField build() {
      separator(DEFAULT_SEPARATOR);
      return new AccessRightLevelsField(getThis());
    }

  }

  protected AccessRightLevelsField(@NotNull Builder builder) {
    super(builder);
  }

  @Override
  protected LevelsClass getListClass() {
    return new LevelsClass();
  }

}
