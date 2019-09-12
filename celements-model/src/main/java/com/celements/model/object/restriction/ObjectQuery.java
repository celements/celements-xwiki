package com.celements.model.object.restriction;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.concurrent.NotThreadSafe;

import com.celements.model.classes.ClassIdentity;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;

@NotThreadSafe
public class ObjectQuery<O> {

  private Set<Predicate<O>> restrictions = new LinkedHashSet<>();

  public ObjectQuery() {
  }

  public ObjectQuery(Iterable<? extends Predicate<O>> iter) {
    this.addAll(iter);
  }

  public void add(Predicate<O> restr) {
    restrictions.add(restr);
  }

  public void addAll(Iterable<? extends Predicate<O>> iter) {
    for (Predicate<O> restr : iter) {
      this.add(restr);
    }
  }

  public FluentIterable<Predicate<O>> getRestrictions() {
    return FluentIterable.from(restrictions);
  }

  public Predicate<O> predicate(ClassIdentity classId) {
    return restrictions.stream()
        .filter(new ClassPredicate(classId))
        .reduce((p1, p2) -> p1.and(p2))
        .orElse(Predicates.alwaysTrue());
  }

  public Set<ClassIdentity> getObjectClasses() {
    return getRestrictions()
        .filter(getClassRestrictionClass())
        .transform(ClassRestriction::getClassIdentity)
        .toSet();
  }

  @SuppressWarnings("unchecked")
  private Class<ClassRestriction<O>> getClassRestrictionClass() {
    return (Class<ClassRestriction<O>>) (Class<?>) ClassRestriction.class;
  }

  public FluentIterable<FieldRestriction<O, ?>> getFieldRestrictions(ClassIdentity classId) {
    return getRestrictions().filter(getFieldRestrictionClass()).filter(new ClassPredicate(classId));
  }

  @SuppressWarnings("unchecked")
  private Class<FieldRestriction<O, ?>> getFieldRestrictionClass() {
    return (Class<FieldRestriction<O, ?>>) (Class<?>) FieldRestriction.class;
  }

  @Override
  public int hashCode() {
    return Objects.hash(restrictions);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ObjectQuery) {
      ObjectQuery<?> other = (ObjectQuery<?>) obj;
      return Objects.equals(this.restrictions, other.restrictions);
    }
    return false;
  }

  @Override
  public String toString() {
    return "ObjectQuery " + restrictions;
  }

  private class ClassPredicate implements com.google.common.base.Predicate<Predicate<?>> {

    private final ClassIdentity classId;

    ClassPredicate(ClassIdentity classId) {
      this.classId = classId;
    }

    @Override
    public boolean apply(Predicate<?> restr) {
      if (restr instanceof ClassRestriction) {
        return ((ClassRestriction<?>) restr).getClassIdentity().equals(classId);
      }
      return true;
    }

  }

}
