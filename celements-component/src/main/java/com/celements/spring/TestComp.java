package com.celements.spring;

@org.springframework.stereotype.Component("lol")
@org.xwiki.component.annotation.Component("lol")
public class TestComp implements TestRole {

  @Override
  public String getter() {
    return "got it!";
  }

}
