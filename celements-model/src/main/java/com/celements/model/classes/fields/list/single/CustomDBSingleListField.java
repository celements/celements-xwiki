package com.celements.model.classes.fields.list.single;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

import com.celements.marshalling.Marshaller;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.ListClass;

@Immutable
public class CustomDBSingleListField<T> extends SingleListField<T> {

  private final String sql;

  public static class Builder<B extends Builder<B, T>, T> extends SingleListField.Builder<B, T> {

    private String sql;

    @Deprecated
    public Builder(@NotNull String classDefName, @NotNull String name,
        @NotNull Marshaller<T> marshaller) {
      super(classDefName, name, marshaller);
    }

    public Builder(@NotNull ClassReference classRef, @NotNull String name,
        @NotNull Marshaller<T> marshaller) {
      super(classRef, name, marshaller);
    }

    @Override
    @SuppressWarnings("unchecked")
    public B getThis() {
      return (B) this;
    }

    public B sql(@Nullable String val) {
      sql = val;
      return getThis();
    }

    @Override
    public CustomDBSingleListField<T> build() {
      return new CustomDBSingleListField<>(getThis());
    }

  }

  protected CustomDBSingleListField(@NotNull Builder<?, T> builder) {
    super(builder);
    this.sql = builder.sql;
  }

  public String getSql() {
    return sql;
  }

  @Override
  protected ListClass getListClass() {
    DBListClass element = new DBListClass();
    if (sql != null) {
      element.setSql(sql);
    }
    return element;
  }

}
