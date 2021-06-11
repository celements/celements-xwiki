package com.celements.spring;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentEventManager;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;

public class SpringCtx implements ComponentManager {

  AnnotationConfigApplicationContext context;

  public SpringCtx() {
    context = new AnnotationConfigApplicationContext("com.celements.spring"); // TODO remove if
                                                                              // manual wiring
    // context.refresh();
  }

  @SuppressWarnings("unused")
  public void init() {
    ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
    // loader.getComponentsDescriptors(getClass())
    // loader.initialize(this, classLoader);
  }

  public TestBean asdf() {
    return context.getBean(TestBean.class);
  }

  @Override
  public <T> boolean hasComponent(Class<T> role) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public <T> boolean hasComponent(Class<T> role, String roleHint) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public <T> T lookup(Class<T> role) throws ComponentLookupException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> T lookup(Class<T> role, String roleHint) throws ComponentLookupException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> void release(T component) throws ComponentLifecycleException {
    // TODO Auto-generated method stub

  }

  @Override
  public <T> Map<String, T> lookupMap(Class<T> role) throws ComponentLookupException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> List<T> lookupList(Class<T> role) throws ComponentLookupException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> void registerComponent(ComponentDescriptor<T> descriptor)
      throws ComponentRepositoryException {
    registerComponent(descriptor, null);
  }

  @Override
  public <T> void registerComponent(ComponentDescriptor<T> descriptor, T component)
      throws ComponentRepositoryException {
    switch (descriptor.getInstantiationStrategy()) {
      case SINGLETON:
        // TODO register bean instance instead, see javadoc of
        // TODO use
        // org.xwiki.component.embed.EmbeddableComponentManager.createInstance(ComponentDescriptor<T>)
        context.getBeanFactory().registerSingleton(descriptor.getRoleHint(), component);
        break;
      case PER_LOOKUP:
        break;
      default:
        throw new ComponentRepositoryException(
            "invalid strategy: " + descriptor.getInstantiationStrategy());
    }
  }

  @Override
  public void unregisterComponent(Class<?> role, String roleHint) {
    // TODO Auto-generated method stub

  }

  @Override
  public <T> ComponentDescriptor<T> getComponentDescriptor(Class<T> role, String roleHint) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> List<ComponentDescriptor<T>> getComponentDescriptorList(Class<T> role) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ComponentEventManager getComponentEventManager() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setComponentEventManager(ComponentEventManager eventManager) {
    // TODO Auto-generated method stub

  }

  @Override
  public ComponentManager getParent() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setParent(ComponentManager parentComponentManager) {
    // TODO Auto-generated method stub
  }

}
