package com.celements.configuration;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.test.MockConfigurationSource;

import com.celements.common.test.AbstractComponentTest;

public class ConfigSourceUtilsTest extends AbstractComponentTest {

  private MockConfigurationSource cfgSrcMock;

  @Before
  public void prepareTest() throws Exception {
    cfgSrcMock = getConfigurationSource();
  }

  @Test
  public void test_getStringListProperty_null() throws Exception {
    String key = "some.key";
    cfgSrcMock.setProperty(key, null);
    List<String> ret = ConfigSourceUtils.getStringListProperty(cfgSrcMock, key);
    assertEquals(0, ret.size());
  }

  @Test
  public void test_getStringListProperty_empty() throws Exception {
    String key = "some.key";
    cfgSrcMock.setProperty(key, "");
    List<String> ret = ConfigSourceUtils.getStringListProperty(cfgSrcMock, key);
    assertEquals(0, ret.size());
  }

  @Test
  public void test_getStringListProperty_blank() throws Exception {
    String key = "some.key";
    cfgSrcMock.setProperty(key, "   ");
    List<String> ret = ConfigSourceUtils.getStringListProperty(cfgSrcMock, key);
    assertEquals(0, ret.size());
  }

  @Test
  public void test_getStringListProperty_string() throws Exception {
    String key = "some.key";
    cfgSrcMock.setProperty(key, "A");
    List<String> ret = ConfigSourceUtils.getStringListProperty(cfgSrcMock, key);
    assertEquals(1, ret.size());
    assertEquals("A", ret.get(0));
  }

  @Test
  public void test_getStringListProperty_int() throws Exception {
    String key = "some.key";
    cfgSrcMock.setProperty(key, 3);
    List<String> ret = ConfigSourceUtils.getStringListProperty(cfgSrcMock, key);
    assertEquals(1, ret.size());
    assertEquals("3", ret.get(0));
  }

  @Test
  public void test_getStringListProperty_list() throws Exception {
    String key = "some.key";
    cfgSrcMock.setProperty(key, Arrays.asList("", "A", null, 3, "   ", "B"));
    List<String> ret = ConfigSourceUtils.getStringListProperty(cfgSrcMock, key);
    assertEquals(3, ret.size());
    assertEquals("A", ret.get(0));
    assertEquals("3", ret.get(1));
    assertEquals("B", ret.get(2));
  }

}
