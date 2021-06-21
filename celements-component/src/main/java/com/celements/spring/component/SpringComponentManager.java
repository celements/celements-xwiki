package com.celements.spring.component;

import static java.util.stream.Collectors.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Service;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentRole;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.logging.CommonsLoggingLogger;
import org.xwiki.component.manager.ComponentEventManager;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentManagerInitializer;
import org.xwiki.component.manager.ComponentRepositoryException;

/**
 * TODO compare class with {@link EmbeddableComponentManager} and analyse missing stuff
 */
@Service(SpringComponentManager.NAME)
public class SpringComponentManager implements ComponentManager {

  public static final String NAME = "springify";

  private final AtomicBoolean initialised = new AtomicBoolean(false);

  private Map<String, ComponentDescriptor<?>> descriptors = new ConcurrentHashMap<>();

  private final GenericApplicationContext springContext;

  private final ComponentAnnotationLoader descriptorLoader;

  private ComponentEventManager eventManager;

  @Inject
  public SpringComponentManager(GenericApplicationContext context) {
    springContext = context;
    descriptorLoader = new ComponentAnnotationLoader();
    descriptorLoader.enableLogging(new CommonsLoggingLogger(SpringComponentManager.class));
  }

  /**
   * Load all component annotations and register them as components.
   */
  // TODO Post
  public void initialize() {
    if (!initialised.getAndSet(true)) {
      try {
        ClassLoader classLoader = this.getClass().getClassLoader();
        for (ComponentDescriptor<?> descriptor : descriptorLoader
            .loadDeclaredDescriptors(classLoader)) {
          registerComponent(descriptor);
        }
        // Extension point to allow component to manipulate ComponentManager initialized state.
        lookupList(ComponentManagerInitializer.class).stream()
            .forEach(initializer -> initializer.initialize(this));
        // springContext.refresh(); TODO not allowed?
      } catch (ClassNotFoundException | IOException | ComponentLookupException
          | ComponentRepositoryException | BeansException exc) {
        throw new RuntimeException("failed to initialise component manager", exc);
      }
    } else {
      throw new IllegalStateException("already initialised");
    }
  }

  @Override
  public <T> boolean hasComponent(Class<T> role) {
    return hasComponent(role, null);
  }

  @Override
  public <T> boolean hasComponent(Class<T> role, String hint) {
    return hasComponent(uniqueBeanName(role, hint));
  }

  private boolean hasComponent(String beanName) {
    return descriptors.containsKey(beanName);
  }

  @Override
  public <T> T lookup(Class<T> role) throws ComponentLookupException {
    return lookup(role, null);
  }

  @Override
  public <T> T lookup(Class<T> role, String hint) throws ComponentLookupException {
    Throwable cause = null;
    String beanName = uniqueBeanName(role, hint);
    if (hasComponent(beanName)) {
      try {
        return springContext.getBean(beanName, role);
      } catch (BeansException exc) {
        cause = exc;
      }
    }
    throw new ComponentLookupException("lookup - failed for role [" + role
        + "] and hint [" + hint + "]", cause);
  }

  private <T> T lookup(ComponentDescriptor<T> descriptor) throws ComponentLookupException {
    return lookup(descriptor.getRole(), descriptor.getRoleHint());
  }

  @Override
  public <T> Map<String, T> lookupMap(Class<T> role) throws ComponentLookupException {
    return streamDescriptors(role).collect(toMap(
        ComponentDescriptor::getRoleHint,
        rethrow(this::lookup)));
  }

  @Override
  public <T> List<T> lookupList(Class<T> role) throws ComponentLookupException {
    return streamDescriptors(role).map(rethrow(this::lookup)).collect(toList());
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
      // springContext.refresh(); // TODO not allowed?
      descriptors.put(beanName, descriptor);
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
    String beanName = uniqueBeanName(role, hint);
    ComponentDescriptor<?> descriptor = descriptors.remove(beanName);
    if (descriptor != null) {
      try {
        springContext.removeBeanDefinition(beanName);
        // springContext.refresh(); // TODO not allowed?
      } catch (BeansException exc) {
        // TODO log
      }
      getEventManager().ifPresent(em -> em.notifyComponentUnregistered(descriptor));
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
        // springContext.refresh(); // TODO not allowed?
      } catch (BeansException exc) {
        throw new ComponentLifecycleException("release - failed for class ["
            + component.getClass() + "]", exc);
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> ComponentDescriptor<T> getComponentDescriptor(Class<T> role, String hint) {
    return (ComponentDescriptor<T>) descriptors.get(uniqueBeanName(role, hint));
  }

  @Override
  public <T> List<ComponentDescriptor<T>> getComponentDescriptorList(Class<T> role) {
    return streamDescriptors(role).collect(toList());
  }

  @SuppressWarnings("unchecked")
  private <T> Stream<ComponentDescriptor<T>> streamDescriptors(Class<T> role) {
    return descriptors.values().stream()
        .filter(descriptor -> descriptor.getRole().equals(role))
        .map(descriptor -> (ComponentDescriptor<T>) descriptor);
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

  public String uniqueBeanName(ComponentDescriptor<?> descriptor) {
    return uniqueBeanName(descriptor.getRole(), descriptor.getRoleHint());
  }

  public String uniqueBeanName(Class<?> role, String hint) {
    if ((hint == null) || hint.trim().isEmpty()) {
      hint = DefaultComponentRole.HINT;
    }
    return role.getName() + "_" + hint;
  }

  // TODO move to celements-commons
  @FunctionalInterface
  public interface ThrowingFunction<T, R, E extends Exception> {

    R apply(T t) throws E;
  }

  public static <T, R, E extends Exception> Function<T, R> rethrow(
      ThrowingFunction<T, R, E> function) throws E {
    return t -> {
      try {
        return function.apply(t);
      } catch (Exception exception) {
        sneakyThrow(exception);
        throw new RuntimeException("sneakyThrow always throws");
      }
    };
  }

  @SuppressWarnings("unchecked")
  private static <E extends Throwable> void sneakyThrow(Exception exception) throws E {
    throw (E) exception;
  }

}
