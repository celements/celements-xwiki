package com.celements.model.classes.fields.list;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

import com.celements.marshalling.DefaultMarshaller;
import com.xpn.xwiki.objects.classes.GroupsClass;

/**
 * @deprecated instead use {@link GroupListField}
 */
@Deprecated
@Immutable
public final class ListOfGroupsField extends ListField<String> {

  private final Boolean usesList;

  public static class Builder extends ListField.Builder<Builder, String> {

    private Boolean usesList;

    @Deprecated
    public Builder(@NotNull String classDefName, @NotNull String name) {
      super(classDefName, name, new DefaultMarshaller());
      separator(",");
    }

    public Builder(@NotNull ClassReference classRef, @NotNull String name) {
      super(classRef, name, new DefaultMarshaller());
      separator(",");
    }

    @Override
    public Builder getThis() {
      return this;
    }

    public Builder usesList(@NotNull Boolean usesList) {
      this.usesList = usesList;
      return getThis();
    }

    @Override
    public ListOfGroupsField build() {
      return new ListOfGroupsField(getThis());
    }

  }

  protected ListOfGroupsField(@NotNull Builder builder) {
    super(builder);
    this.usesList = builder.usesList;
  }

  public Boolean getUsesList() {
    return usesList;
  }

  @Override
  protected GroupsClass getListClass() {
    GroupsClass element = new GroupsClass();
    if (usesList != null) {
      element.setUsesList(usesList);
    }
    return element;
  }

}
