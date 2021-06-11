package com.celements.spring;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

@Component("lal")
public class TestComp2 implements TestRole {

  @Requirement("lol")
  private TestRole other;

  @Override
  public String getter() {
    return other.getter() + " tadaa";
  }

}
