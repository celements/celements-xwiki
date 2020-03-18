package com.celements.model.access.exception;

import org.xwiki.model.reference.DocumentReference;

public class TranslationCreateException extends DocumentAccessException {

  private static final long serialVersionUID = 5883823053588063378L;

  public TranslationCreateException(DocumentReference docRef, String lang) {
    super(docRef, lang);
  }

  public TranslationCreateException(DocumentReference docRef, String lang, Throwable cause) {
    super(docRef, lang, cause);
  }

}
