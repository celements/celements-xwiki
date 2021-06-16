package com.celements.spring.component;

import static java.util.stream.Collectors.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Service;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.annotation.ComponentDescriptorFactory;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.logging.CommonsLoggingLogger;
import org.xwiki.component.manager.ComponentEventManager;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentManagerInitializer;
import org.xwiki.component.manager.ComponentRepositoryException;

import com.celements.spring.SpringContextManager;

/**
 * TODO compare class with {@link EmbeddableComponentManager} and analyse missing stuff
 */
@Service // TODO test if working as spring service
public class SpringComponentManager implements ComponentManager {

  private final GenericApplicationContext springContext;

  private final ComponentDescriptorFactory factory;
  private final ComponentAnnotationLoader loader;

  private ComponentEventManager eventManager;

  public SpringComponentManager() {
    springContext = SpringContextManager.get();
    this.factory = new ComponentDescriptorFactory();
    this.loader = new ComponentAnnotationLoader();
  }

  // TODO review: is this working here?
  /**
   * Load all component annotations and register them as components.
   */
  public void initialize(ClassLoader classLoader) {
    loader.enableLogging(new CommonsLoggingLogger(loader.getClass()));
    loader.initialize(this, classLoader);
    // Extension point to allow component to manipulate ComponentManager initialized state.
    // TODO needed?
    try {
      List<ComponentManagerInitializer> initializers = this
          .lookupList(ComponentManagerInitializer.class);
      for (ComponentManagerInitializer initializer : initializers) {
        initializer.initialize(this);
      }
    } catch (ComponentLookupException e) {
      // Should never happen
      // logger.error("Failed to lookup ComponentManagerInitializer components", e);
    }
  }

  @Override
  public <T> boolean hasComponent(Class<T> role) {
    return hasComponent(role, "default");
  }

  @Override
  public <T> boolean hasComponent(Class<T> role, String hint) {
    return getPossibleLookupBeanNames(role, hint).anyMatch(beanName -> {
      try {
        return springContext.getBean(beanName, role) != null;
      } catch (BeansException exc) {
        return false;
      }
    });
  }

  @Override
  public <T> T lookup(Class<T> role) throws ComponentLookupException {
    try {
      return springContext.getBean(role);
    } catch (BeansException exc) {
      throw new ComponentLookupException("lookup - failed for role [" + role + "]", exc);
    }
  }

  @Override
  public <T> T lookup(Class<T> role, String hint) throws ComponentLookupException {
    Throwable cause = null;
    for (String value : getPossibleLookupBeanNames(role, hint).collect(toList())) {
      try {
        return springContext.getBean(value, role);
      } catch (BeansException exc) {
        if (cause == null) {
          cause = exc;
        }
      }
    }
    throw new ComponentLookupException("lookup - failed for role [" + role
        + "] and hint [" + hint + "]", cause);
  }

  private Stream<String> getPossibleLookupBeanNames(Class<?> role, String hint) {
    if ((hint == null) || hint.isEmpty() || hint.equals("default")) {
      return Stream.of(uniqueBeanName(role, "default"));
    } else {
      return Stream.of(
          uniqueBeanName(role, hint), // default bean name for xwiki components
          hint); // fallback in case we look up spring beans over this interface
    }
  }

  @Override
  public <T> Map<String, T> lookupMap(Class<T> role) throws ComponentLookupException {
    boolean includeNonSingletons = false; // TODO ?
    boolean allowEagerInit = false; // TODO ?
    try {
      return springContext.getBeansOfType(role, includeNonSingletons, allowEagerInit);
    } catch (BeansException exc) {
      throw new ComponentLookupException("lookupMap - failed for role [" + role + "]", exc);
    }
  }

  @Override
  public <T> List<T> lookupList(Class<T> role) throws ComponentLookupException {
    // TODO is returned map ordered? otherwise maybe use ctx.getBeanNamesForType(role)
    // @Order annotation exists in spring
    // XWiki component priority?
    return lookupMap(role).values().stream().collect(Collectors.toList());
  }

  @Override
  public <T> void registerComponent(ComponentDescriptor<T> descriptor)
      throws ComponentRepositoryException {
    registerComponent(descriptor, null);
  }

  @Override
  public <T> void registerComponent(ComponentDescriptor<T> descriptor, T component)
      throws ComponentRepositoryException {
    String beanName = uniqueBeanName(descriptor);
    try {
      if (component != null) {
        // according to method contract, if an instance is provided it should never be created from
        // the descriptor, irrespective of the instantiation strategy. the component must be fully
        // initialized already.
        if (descriptor.getInstantiationStrategy() == ComponentInstantiationStrategy.PER_LOOKUP) {
          // TODO let's log this, it shouldn't happen outside of tests!
        }
        springContext.getBeanFactory().registerSingleton(beanName, component);
      } else {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
            .genericBeanDefinition(descriptor.getImplementation());
        // TODO required?
        // builder.addPropertyValue("property1", "propertyValue");
        // builder.setInitMethodName("initialize"); // perhaps for initializables ?
        builder.setScope(getSpringBeanScope(descriptor));
        // TODO use in a post bean factory:
        // org.xwiki.component.embed.EmbeddableComponentManager.createInstance(ComponentDescriptor<T>)
        springContext.registerBeanDefinition(beanName, builder.getBeanDefinition());
      }
      springContext.refresh(); // TODO required?
      getEventManager().ifPresent(em -> em.notifyComponentRegistered(descriptor));
    } catch (BeansException exc) {
      throw new ComponentRepositoryException("registerComponent - failed for descriptor ["
          + descriptor + "]", exc);
    }
  }

  private String getSpringBeanScope(ComponentDescriptor<?> descriptor)
      throws ComponentRepositoryException {
    switch (descriptor.getInstantiationStrategy()) {
      case SINGLETON:
        return BeanDefinition.SCOPE_SINGLETON;
      case PER_LOOKUP:
        return BeanDefinition.SCOPE_PROTOTYPE;
      default:
        throw new ComponentRepositoryException(
            "registerComponent - invalid strategy for descriptor [" + descriptor + "]");
    }
  }

  @Override
  public void unregisterComponent(Class<?> role, String hint) {
    try {
      springContext.removeBeanDefinition(uniqueBeanName(role, hint));
      springContext.refresh(); // TODO required?
      ComponentDescriptor<?> descriptor = getComponentDescriptor(role, hint);
      if (descriptor != null) {
        getEventManager().ifPresent(em -> em.notifyComponentUnregistered(descriptor));
      }
    } catch (BeansException exc) {
      // TODO log
    }
  }

  @Override
  public <T> void release(T component) throws ComponentLifecycleException {
    if (component != null) {
      try {
        // TODO is this sufficient ? (test with&without bean definition)
        springContext.getBeanFactory().destroyBean(component);
        // TODO flawed this can only release components with xwiki annoations
        // GenericApplicationContext ctx = SpringCtx.get();
        // loader.streamComponentsDescriptors(component.getClass())
        // .filter(descriptor -> hasComponent(descriptor.getRole(), descriptor.getRoleHint()))
        // .map(this::uniqueBeanName)
        // .filter(beanName -> ctx.getBean(beanName) == component)
        // .forEach(beanName -> ctx.getDefaultListableBeanFactory().destroySingleton(beanName));
        springContext.refresh(); // TODO required?
      } catch (BeansException exc) {
        throw new ComponentLifecycleException("release - failed for class ["
            + component.getClass() + "]", exc);
      }
    }
  }

  // TODO component descriptors don't work with spring beans -> fast fail if annotations missing?
  @Override
  public <T> ComponentDescriptor<T> getComponentDescriptor(Class<T> role, String hint) {
    try {
      BeanDefinition beanDef = springContext.getBeanDefinition(uniqueBeanName(role, hint));
      return factory.createComponentDescriptor(getComponentClass(lookup(role, hint)), role, hint);
    } catch (ComponentLookupException exc) {
      return null;
    }
  }

  // TODO component descriptors don't work with spring beans -> fast fail if annotations missing?
  @Override
  public <T> List<ComponentDescriptor<T>> getComponentDescriptorList(Class<T> role) {
    Stream<ComponentDescriptor<T>> descriptors;
    try {
      descriptors = lookupList(role).stream()
          .flatMap(component -> factory.createComponentDescriptorsAsStream(
              getComponentClass(component), role))
          .filter(descriptor -> hasComponent(descriptor.getRole(), descriptor.getRoleHint()));
    } catch (ComponentLookupException exc) {
      descriptors = Stream.empty();
    }
    return descriptors.collect(toList());
  }

  @SuppressWarnings("unchecked")
  private <T> Class<? extends T> getComponentClass(T component) {
    return (Class<? extends T>) component.getClass();
  }

  @Override
  public ComponentEventManager getComponentEventManager() {
    return eventManager;
  }

  public Optional<ComponentEventManager> getEventManager() {
    return Optional.ofNullable(eventManager);
  }

  @Override
  public void setComponentEventManager(ComponentEventManager eventManager) {
    this.eventManager = eventManager;
  }

  @Override
  public ComponentManager getParent() {
    return null;
  }

  @Override
  public void setParent(ComponentManager parentComponentManager) {
    if (parentComponentManager != null) {
      throw new UnsupportedOperationException("parent component manager not supported");
    }
  }

  private String uniqueBeanName(ComponentDescriptor<?> descriptor) {
    return uniqueBeanName(descriptor.getRole(), descriptor.getRoleHint());
  }

  // TODO long naming :(
  private String uniqueBeanName(Class<?> role, String hint) {
    return role.getName() + "-" + hint;
  }

}
