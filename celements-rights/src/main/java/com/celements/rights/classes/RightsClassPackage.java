package com.celements.rights.classes;

import java.util.List;
import java.util.function.Supplier;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

import com.celements.model.classes.AbstractClassPackage;
import com.celements.model.classes.ClassDefinition;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;

@Component(RightsClassPackage.NAME)
public class RightsClassPackage extends AbstractClassPackage {

  public static final String NAME = "rights";

  @Requirement
  private List<RightsClassDefinition> classDefsMutable;

  private final Supplier<ImmutableList<RightsClassDefinition>> classDefs = Suppliers
      .memoize(() -> ImmutableList.copyOf(classDefsMutable));

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public List<? extends ClassDefinition> getClassDefinitions() {
    return classDefs.get();
  }
}
