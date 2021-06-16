package com.celements.spring;

import static java.util.stream.Collectors.*;

import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;

public class SpringTest /* extends AbstractComponentTest */ {

  @Test
  public void test() {
    GenericApplicationContext ctx = SpringContextManager.get();
    ctx.getBean(TestBean.class).greet();
    System.out.println(ctx.getBean("lol", TestRole.class).getter());
    System.out.println(ctx.getBean("lal", TestRole.class).getter());
    System.out.println(Stream.of(ctx.getBeanDefinitionNames()).collect(toList()));
  }

}
