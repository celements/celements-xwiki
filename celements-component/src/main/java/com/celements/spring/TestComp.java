package com.celements.spring;

import org.xwiki.component.annotation.Component;

@Component("lol")
public class TestComp implements TestRole {

  @Override
  public String getter() {
    return "got it!";
  }

}
