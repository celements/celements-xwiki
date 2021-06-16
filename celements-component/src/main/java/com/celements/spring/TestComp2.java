package com.celements.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.xwiki.component.annotation.Requirement;

@org.springframework.stereotype.Component("lal")
@org.xwiki.component.annotation.Component("lal")
public class TestComp2 implements TestRole {

  @Autowired
  @Qualifier("lol")
  @Requirement("lol")
  private TestRole other;

  @Override
  public String getter() {
    return other.getter() + " tadaa";
  }

}
