package com.celements.model.object;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.field.FieldGetter;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;

@NotThreadSafe
public abstract class AbstractObjectFetcher<R extends AbstractObjectFetcher<R, D, O>, D, O> extends
    AbstractObjectHandler<R, D, O> implements ObjectFetcher<D, O> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectFetcher.class);

  private boolean clone;

  protected AbstractObjectFetcher(@NotNull D doc) {
    super(doc);
    this.clone = true;
  }

  @Override
  public boolean exists() {
    return count() > 0;
  }

  @Override
  public int count() {
    return iter().size();
  }

  @Override
  public Optional<O> first() {
    return iter().first();
  }

  @Override
  public List<O> list() {
    return iter().toList();
  }

  @Override
  public Set<O> set() {
    return iter().toSet();
  }

  @Override
  public FluentIterable<O> iter() {
    FluentIterable<O> iter = FluentIterable.of();
    for (ClassIdentity classId : getObjectClasses()) {
      iter = iter.append(getObjects(classId));
    }
    return iter;
  }

  @Override
  public Map<ClassIdentity, List<O>> map() {
    ImmutableMap.Builder<ClassIdentity, List<O>> builder = ImmutableMap.builder();
    for (ClassIdentity classId : getObjectClasses()) {
      builder.put(classId, getObjects(classId).toList());
    }
    return builder.build();
  }

  private Set<? extends ClassIdentity> getObjectClasses() {
    Set<? extends ClassIdentity> classes = getQuery().getObjectClasses();
    if (classes.isEmpty()) {
      classes = getBridge().getDocClasses(getDocument()).toSet();
    }
    return classes;
  }

  private FluentIterable<O> getObjects(ClassIdentity classId) {
    FluentIterable<O> objIter = getBridge().getObjects(getDocument(), classId);
    objIter = objIter.filter(Predicates.and(getQuery().getRestrictions(classId)));
    if (clone) {
      LOGGER.debug("{} clone objects", this);
      objIter = objIter.transform(new ObjectCloner());
    }
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("{} fetched for {}: {}", this, classId, objIter);
    } else if (LOGGER.isInfoEnabled()) {
      LOGGER.info("{} fetched for {} {} objects", this, classId, objIter.size());
    }
    return objIter;
  }

  /**
   * disables cloning for the fetcher. use with caution!
   */
  protected R disableCloning() {
    clone = false;
    return getThis();
  }

  private class ObjectCloner implements Function<O, O> {

    @Override
    public O apply(O obj) {
      return getBridge().cloneObject(obj);
    }
  }

  @Override
  public <T> FieldGetter<O, T> fetchField(ClassField<T> field) {
    return new FieldGetter<>(getBridge().getFieldAccessor(), this.clone().filter(
        field.getClassDef()), field);
  }

  @Override
  public abstract AbstractObjectFetcher<?, D, O> clone();

}
