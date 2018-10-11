package com.celements.model.object;

import static com.google.common.base.Preconditions.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.ClassReference;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.PseudoClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.field.FieldAccessor;
import com.celements.model.field.FieldGetterFunction;
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
  public abstract AbstractObjectFetcher<?, D, O> clone();

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
  public O firstAssert() {
    Optional<O> ret = first();
    checkArgument(ret.isPresent(), "empty - %s", this);
    return ret.get();
  }

  @Override
  public O unique() {
    Iterator<O> iter = iter().iterator();
    checkArgument(iter.hasNext(), "empty - %s", this);
    O ret = iter.next();
    checkArgument(!iter.hasNext(), "non unique - %s", this);
    return ret;
  }

  @Override
  public List<O> list() {
    return iter().toList();
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

  protected Set<? extends ClassIdentity> getObjectClasses() {
    Set<? extends ClassIdentity> classes = getQuery().getObjectClasses();
    if (classes.isEmpty()) {
      classes = getBridge().getDocClasses(getDocument()).toSet();
    }
    return classes;
  }

  protected FluentIterable<O> getObjects(ClassIdentity classId) {
    FluentIterable<O> objects = getBridge().getObjects(getDocument(), classId);
    objects = objects.filter(Predicates.and(getQuery().getRestrictions(classId)));
    if (clone) {
      LOGGER.debug("{} clone objects", this);
      objects = objects.transform(new ObjectCloner());
    }
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("{} fetched for {}: {}", this, classId, objects);
    } else if (LOGGER.isInfoEnabled()) {
      LOGGER.info("{} fetched for {} {} objects", this, classId, objects.size());
    }
    return objects;
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
  public <T> FieldFetcher<T> fetchField(final ClassField<T> field) {
    final FluentIterable<O> objects = clone().filter(field.getClassDef()).iter();
    return new FieldFetcher<T>() {

      @Override
      public Optional<T> first() {
        return iter().first();
      }

      @Override
      public List<T> list() {
        return iter().toList();
      }

      @Override
      public Set<T> set() {
        return iter().toSet();
      }

      @Override
      public FluentIterable<T> iter() {
        return iterNullable().filter(Predicates.notNull());
      }

      @Override
      public FluentIterable<T> iterNullable() {
        FluentIterable<T> iter;
        if (isValidObjectClass(field.getClassDef())) {
          FieldAccessor<O> accessor = getBridge().getObjectFieldAccessor();
          iter = objects.transform(new FieldGetterFunction<>(accessor, field));
        } else {
          FieldAccessor<D> accessor = getBridge().getDocumentFieldAccessor();
          iter = FluentIterable.from(accessor.getValue(getDocument(), field).asSet());
        }
        return iter;
      }
    };
  }

  private boolean isValidObjectClass(ClassIdentity classId) {
    return (classId instanceof ClassReference) || !(classId instanceof PseudoClassDefinition);
  }

}
