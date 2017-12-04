package com.celements.model.doc;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

@ComponentRole
public interface DocumentIdComputer {

  /**
   * @return computes the id for the given document
   * @throws DocumentIdComputationException
   *           if unable to compute an id
   */
  long computeId(@NotNull DocumentReference docRef, @NotNull String lang)
      throws DocumentIdComputationException;

  public class DocumentIdComputationException extends Exception {

    private static final long serialVersionUID = 1L;

    protected DocumentIdComputationException(String message) {
      super(message);
    }

    protected DocumentIdComputationException(String message, Throwable cause) {
      super(message, cause);
    }

  }

}
