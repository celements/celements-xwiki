package com.celements.model.classes.fields;

import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;

import java.util.Arrays;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.apache.commons.codec.digest.MessageDigestAlgorithms;

import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.meta.PasswordMetaClass;

@Immutable
public final class PasswordField extends StringField {

  public enum StorageType {
    Hash,
    Clear;
  }

  protected static final String CLASS_FIELD_NAME_STORAGE_TYPE = "storageType";

  private final StorageType storageType;
  private final String hashAlgorithm;

  public static class Builder extends StringField.Builder {

    private StorageType storageType;
    private String hashAlgorithm;

    public Builder(@NotNull String classDefName, @NotNull String name) {
      super(classDefName, name);
    }

    @Override
    public Builder getThis() {
      return this;
    }

    public Builder storageType(@Nullable StorageType val) {
      storageType = val;
      return getThis();
    }

    public Builder hashAlgorithm(@Nullable String val) {
      hashAlgorithm = val;
      return getThis();
    }

    @Override
    public PasswordField build() {
      return new PasswordField(getThis());
    }

  }

  protected PasswordField(@NotNull Builder builder) {
    super(builder);
    this.storageType = firstNonNull(builder.storageType, StorageType.Hash);
    this.hashAlgorithm = firstNonNull(builder.hashAlgorithm, MessageDigestAlgorithms.SHA_512);
    checkArgument(Arrays.asList(MessageDigestAlgorithms.values()).contains(hashAlgorithm),
        "illegal hash algorithm");
  }

  @NotNull
  public StorageType getStorageType() {
    return storageType;
  }

  @NotNull
  public String getHashAlgorithm() {
    return hashAlgorithm;
  }

  @Override
  protected StringClass getStringPropertyClass() {
    PasswordClass element = new PasswordClass();
    element.setStringValue(CLASS_FIELD_NAME_STORAGE_TYPE, getStorageType().name());
    element.setStringValue(PasswordMetaClass.ALGORITHM_KEY, getHashAlgorithm());
    return element;
  }

}
