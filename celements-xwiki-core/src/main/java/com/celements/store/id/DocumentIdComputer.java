package com.celements.store.id;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

@ComponentRole
public interface DocumentIdComputer {

  /**
   * @return computes the document id for the given document and language
   */
  long compute(@NotNull DocumentReference docRef, String lang);

}
