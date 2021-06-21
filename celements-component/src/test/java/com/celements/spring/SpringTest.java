package com.celements.spring;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.xwiki.script.service.ScriptService;

import com.celements.spring.component.SpringComponentManager;

public class SpringTest /* extends AbstractComponentTest */ {

  SpringComponentManager cm;

  @Before
  public void setup() {
    cm = SpringContextManager.get().getBean(SpringComponentManager.NAME,
        SpringComponentManager.class);
  }

  @Test
  public void test_init() throws Exception {
    cm.initialize();
    assertTrue(cm.hasComponent(ScriptService.class, "model"));
    assertSame(
        SpringContextManager.get().getBean(cm.uniqueBeanName(ScriptService.class, "model")),
        cm.lookup(ScriptService.class, "model"));
  }

  @Test
  public void test() {
    GenericApplicationContext ctx = SpringContextManager.get();
    ctx.getBean(TestBean.class).greet();
    System.out.println(ctx.getBean("lol", TestRole.class).getter());
    System.out.println(ctx.getBean("lal", TestRole.class).getter());
    for (String beanName : ctx.getBeanDefinitionNames()) {
      System.out.println(beanName);
    }
    System.out.println(cm.hasComponent(TestRole.class, "lol"));
  }

}
