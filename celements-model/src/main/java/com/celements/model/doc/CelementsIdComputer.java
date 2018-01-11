package com.celements.model.doc;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;

@ComponentRole
public interface CelementsIdComputer {

  /**
   * @return computes the id for the given document and language
   * @throws IdComputationException
   *           if unable to compute an id
   */
  long computeDocumentId(@NotNull DocumentReference docRef, @Nullable String lang)
      throws IdComputationException;

  /**
   * @return computes the id for the given document, language and collision count
   * @throws IdComputationException
   *           if unable to compute an id
   */
  long computeDocumentId(@NotNull DocumentReference docRef, @Nullable String lang,
      int collisionCount) throws IdComputationException;

  /**
   * @return computes the id for the given document
   * @throws IdComputationException
   *           if unable to compute an id
   */
  long computeDocumentId(@NotNull XWikiDocument doc) throws IdComputationException;

  public class IdComputationException extends Exception {

    private static final long serialVersionUID = 1L;

    protected IdComputationException(String message) {
      super(message);
    }

    protected IdComputationException(Throwable cause) {
      super(cause);
    }

    protected IdComputationException(String message, Throwable cause) {
      super(message, cause);
    }

  }

}
