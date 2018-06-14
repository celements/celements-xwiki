package com.celements.model.object;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.FluentIterable.*;

import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.field.FieldSetter;
import com.celements.model.object.restriction.FieldRestriction;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

@NotThreadSafe
public abstract class AbstractObjectEditor<R extends AbstractObjectEditor<R, D, O>, D, O> extends
    AbstractObjectHandler<R, D, O> implements ObjectEditor<D, O> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectEditor.class);

  protected AbstractObjectEditor(@NotNull D doc) {
    super(doc);
  }

  @Override
  public Map<ClassIdentity, O> create() {
    return create(false);
  }

  @Override
  public Map<ClassIdentity, O> createIfNotExists() {
    return create(true);
  }

  private Map<ClassIdentity, O> create(boolean ifNotExists) {
    return from(getQuery().getObjectClasses()).toMap(new ObjectCreateFunction(ifNotExists));
  }

  @Override
  public O createFirst() {
    return createFirst(false);
  }

  @Override
  public O createFirstIfNotExists() {
    return createFirst(true);
  }

  private O createFirst(boolean ifNotExists) {
    Optional<ClassIdentity> classId = from(getQuery().getObjectClasses()).first();
    checkArgument(classId.isPresent(), "no class defined");
    return new ObjectCreateFunction(ifNotExists).apply(classId.get());
  }

  private class ObjectCreateFunction implements Function<ClassIdentity, O> {

    private final boolean ifNotExists;

    ObjectCreateFunction(boolean ifNotExists) {
      this.ifNotExists = ifNotExists;
    }

    @Override
    public O apply(ClassIdentity classId) {
      O obj = null;
      if (ifNotExists) {
        obj = fetch().filter(classId).first().orNull();
      }
      if (obj == null) {
        obj = getBridge().createObject(getDocument(), classId);
        for (FieldRestriction<O, ?> restriction : getQuery().getFieldRestrictions(classId)) {
          setField(obj, restriction);
        }
        LOGGER.info("{} created object {} for {}", AbstractObjectEditor.this,
            getBridge().getObjectNumber(obj), classId);
      }
      return obj;
    }

    <T> void setField(O obj, FieldRestriction<O, T> restriction) {
      T value = from(restriction.getValues()).first().get();
      getBridge().getFieldAccessor().setValue(obj, restriction.getField(), value);
      LOGGER.debug("{} set field {} on created object to value", AbstractObjectEditor.this,
          restriction.getField(), value);
    }

  }

  @Override
  public List<O> delete() {
    return from(fetch().list()).filter(new ObjectDeletePredicate()).toList();
  }

  @Override
  public Optional<O> deleteFirst() {
    Optional<O> obj = fetch().first();
    if (obj.isPresent() && new ObjectDeletePredicate().apply(obj.get())) {
      return obj;
    }
    return Optional.absent();
  }

  private class ObjectDeletePredicate implements Predicate<O> {

    @Override
    public boolean apply(O obj) {
      boolean success = getBridge().deleteObject(getDocument(), obj);
      LOGGER.info("{} deleted object {} for {}: {}", AbstractObjectEditor.this,
          getBridge().getObjectNumber(obj), getBridge().getObjectClass(obj), success);
      return success;
    }
  }

  @Override
  public abstract AbstractObjectFetcher<?, D, O> fetch();

  @Override
  public <T> FieldSetter<O, T> setField(ClassField<T> field) {
    return new FieldSetter<>(getFieldAccessor(), fetch().filter(field.getClassDef()), field);
  }

  @Override
  public abstract AbstractObjectEditor<?, D, O> clone();

}
