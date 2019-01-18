package com.celements.convert.bean;

import java.util.List;

import org.xwiki.model.reference.DocumentReference;

import com.google.common.base.Optional;

public class TestBean {

  private DocumentReference docRef;
  private String myString;
  private Integer myInt;
  private Boolean myBool;
  private DocumentReference myDocRef;
  private List<String> myListMS;
  private String mySingleList;

  public DocumentReference getDocumentReference() {
    return docRef;
  }

  public void setDocumentReference(DocumentReference docRef) {
    this.docRef = docRef;
  }

  public String getMyString() {
    return myString;
  }

  public void setMyString(String myString) {
    this.myString = myString;
  }

  public Integer getMyInt() {
    return myInt;
  }

  public void setMyInt(Integer myInt) {
    this.myInt = myInt;
  }

  public Boolean getMyBool() {
    return myBool;
  }

  public void setMyBool(Boolean myBool) {
    this.myBool = myBool;
  }

  public Optional<DocumentReference> getMyDocRef() {
    return Optional.fromNullable(myDocRef);
  }

  public void setMyDocRef(DocumentReference myDocRef) {
    this.myDocRef = myDocRef;
  }

  public List<String> getMyListMS() {
    return myListMS;
  }

  public void setMyListMS(List<String> myListMS) {
    this.myListMS = myListMS;
  }

  public String getMySingleList() {
    return mySingleList;
  }

  public void setMySingleList(String mySingleList) {
    this.mySingleList = mySingleList;
  }

}
