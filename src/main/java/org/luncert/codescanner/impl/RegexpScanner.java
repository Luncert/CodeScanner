package org.luncert.codescanner.impl;

import org.luncert.codescanner.CodeFile;
import org.luncert.codescanner.CodeLine;
import org.luncert.codescanner.ICodeScanner;
import org.luncert.codescanner.Message;
import org.luncert.codescanner.MessageType;
import org.luncert.codescanner.exception.InvalidConfigException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexpScanner implements ICodeScanner {
  
  private static final String ERROR_MESSAGE = "Regexp matched";
  
  private Pattern regexpPattern;
  
  @Override
  public void init(Map conf) {
    String exp = (String) conf.get("regexp-scanner");
    if (exp == null) {
      throw new InvalidConfigException("key regexp-scanner is not configured");
    }
  
    try {
      regexpPattern = Pattern.compile(exp);
    } catch (PatternSyntaxException e) {
      throw new InvalidConfigException(e);
    }
  }
  
  public List<Message> process(CodeFile codeFile) {
    List<Message> messageList = new ArrayList<>();
    
    Message lastMessage = Message.NULL;
    
    for (CodeLine line : codeFile) {
      String source = line.getSource();
      Matcher matcher = regexpPattern.matcher(source);
      while (matcher.find()) {
        int start = matcher.start(); // range starts from 0
        if (!mergeToLastMessage(lastMessage, line.getLineNum(), start)) {
          lastMessage = Message.builder()
              .lineNum(line.getLineNum())
              .start(start)
              .end(start)
              .type(MessageType.ERROR)
              .codeFileName(codeFile.getName())
              .description(ERROR_MESSAGE)
              .build();
          messageList.add(lastMessage);
        }
      }
    }
    
    return messageList;
  }
  
  private boolean mergeToLastMessage(Message lastMessage, int lineNum, int start) {
    if (lastMessage.getLineNum() == lineNum && lastMessage.getEnd() == start - 1) {
      lastMessage.setEnd(start);
      return true;
    }
    
    return false;
  }
}
