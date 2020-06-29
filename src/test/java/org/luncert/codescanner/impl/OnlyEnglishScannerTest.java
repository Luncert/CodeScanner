package org.luncert.codescanner.impl;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.luncert.codescanner.CodeFile;
import org.luncert.codescanner.Message;

import java.io.ByteArrayInputStream;
import java.util.List;

@RunWith(JUnit4.class)
public class OnlyEnglishScannerTest {
  
  @Test
  public void test() {
    CodeFile codeFile = prepareTestData();
    OnlyEnglishScanner scanner = new OnlyEnglishScanner();
    List<Message> messageList = scanner.process(codeFile);
    
    Assert.assertEquals(1, messageList.size());
    
    Message errMsg = messageList.get(0);
    Assert.assertEquals(101, errMsg.getLineNum());
    Assert.assertEquals(4, errMsg.getStart());
    Assert.assertEquals(5, errMsg.getEnd());
  }
  
  private CodeFile prepareTestData() {
    StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      buffer.append(RandomStringUtils.randomAlphabetic(128))
          .append("\r\n");
    }
    buffer.append("asd测试").append("\r\n");
    return new CodeFile("test-file", new ByteArrayInputStream(buffer.toString().getBytes()));
  }
}
