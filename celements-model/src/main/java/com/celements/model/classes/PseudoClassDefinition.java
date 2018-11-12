package com.celements.model.classes;

public abstract class PseudoClassDefinition extends AbstractClassDefinition {

  public static final String CLASS_SPACE = "PseudoClass";

  @Override
  public boolean isBlacklisted() {
    // pseudo classes should never be created
    return true;
  }

  @Override
  public boolean isValidObjectClass() {
    return false;
  }

  @Override
  public boolean isInternalMapping() {
    return false;
  }

  @Override
  protected String getClassSpaceName() {
    return CLASS_SPACE;
  }

}
