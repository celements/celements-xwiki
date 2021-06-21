package com.celements.spring;

import java.util.function.Supplier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.xwiki.component.annotation.ComponentAnnotationLoader;

public final class SpringContextManager {

  private static final SpringContextManager INSTANCE = new SpringContextManager();

  private final AnnotationConfigApplicationContext context;

  private SpringContextManager() {
    // TODO remove if manual wiring
    context = new AnnotationConfigApplicationContext("com.celements.spring");
    // context.refresh();
  }

  public AnnotationConfigApplicationContext getContext() {
    return this.context;
  }

  public static Supplier<GenericApplicationContext> supply() {
    return INSTANCE::getContext;
  }

  public static GenericApplicationContext get() {
    return INSTANCE.getContext();
  }

  @SuppressWarnings("unused")
  public void init() {
    ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
    // loader.getComponentsDescriptors(getClass())
    // loader.initialize(this, classLoader);
  }

}
