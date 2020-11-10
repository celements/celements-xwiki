package com.celements.model.classes;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

public interface ClassIdentity {

  /**
   * @return the class reference
   */
  @NotNull
  ClassReference getClassReference();

  /**
   * @return the class definition if it exists
   */
  @NotNull
  Optional<ClassDefinition> getClassDefinition();

  @NotNull
  DocumentReference getDocRef();

  @NotNull
  DocumentReference getDocRef(@NotNull WikiReference wikiRef);

  boolean isValidObjectClass();

  @NotNull
  String serialize();

}
