package com.celements.spring;

import org.junit.Test;

public class SpringTest /* extends AbstractComponentTest */ {

  @Test
  public void test() {
    SpringCtx ctx = new SpringCtx();
    ctx.asdf().greet();
    System.out.println(ctx.context.getBean(TestRole.class, "lol").getter());
    System.out.println(ctx.context.getBean(TestRole.class, "lal").getter());
  }

}
