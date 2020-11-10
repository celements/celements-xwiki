package com.celements.model.object.restriction;

import static com.celements.common.MoreObjectsCel.*;
import static com.google.common.collect.ImmutableSet.*;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.concurrent.NotThreadSafe;

import com.celements.model.classes.ClassIdentity;

@NotThreadSafe
public class ObjectQuery<O> {

  private Set<Predicate<O>> restrictions = new LinkedHashSet<>();

  public ObjectQuery() {}

  public ObjectQuery(Stream<? extends Predicate<O>> stream) {
    stream.forEach(this::add);
  }

  public ObjectQuery<O> add(Predicate<O> restr) {
    if (restr != null) {
      restrictions.add(restr);
    }
    return this;
  }

  public Stream<Predicate<O>> streamRestrictions() {
    return restrictions.stream();
  }

  public Predicate<O> predicate(ClassIdentity classId) {
    return streamRestrictions()
        .filter(restr -> tryCast(restr, ClassRestriction.class)
            .map(classRestr -> classRestr.getClassIdentity().equals(classId))
            .orElse(true))
        .reduce((p1, p2) -> p1.and(p2))
        .orElse(o -> true);
  }

  public Set<ClassIdentity> getObjectClasses() {
    return streamRestrictions()
        .flatMap(tryCast(ClassRestriction.class))
        .map(ClassRestriction::getClassIdentity)
        .collect(toImmutableSet());
  }

  public Set<FieldRestriction<O, ?>> getFieldRestrictions(ClassIdentity classId) {
    return streamRestrictions()
        .flatMap(tryCast(getFieldRestrictionClass()))
        .filter(fieldRestr -> fieldRestr.getClassIdentity().equals(classId))
        .collect(toImmutableSet());
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

}
