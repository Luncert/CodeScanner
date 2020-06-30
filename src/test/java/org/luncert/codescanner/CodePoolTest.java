package org.luncert.codescanner;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.Map;

@RunWith(JUnit4.class)
public class CodePoolTest {
  
  @Test
  public void testLoadFromGit() {
    Map<String, String> gitConf = new HashMap<>();
    gitConf.put("url", "https://github.com/Luncert/redis-cli");
    gitConf.put("branch", "master");
    gitConf.put("rootPath", "data");
    
    Map conf = new HashMap();
    conf.put("git", gitConf);
    
    CodePool codePool = new CodePool();
    codePool.loadWithConfig(conf);
    Assert.assertNotEquals(0, codePool.size());
  }
}
