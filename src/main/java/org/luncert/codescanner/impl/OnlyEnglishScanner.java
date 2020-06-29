package org.luncert.codescanner.impl;

import org.luncert.codescanner.CodeFile;
import org.luncert.codescanner.CodeLine;
import org.luncert.codescanner.ICodeScanner;
import org.luncert.codescanner.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OnlyEnglishScanner implements ICodeScanner {
  
  private static final Pattern PATTERN = Pattern.compile("[^a-zA-Z0-9\\s=.,\\\"\\\\?!:~`@#$%^&*()_\\-+';<>/]");
  
  private static final String ERROR_MESSAGE = "";
  
  public List<Message> process(CodeFile codeFile) {
    List<Message> messageList = new ArrayList<>();
    
    Message lastMessage = Message.NULL;
    
    for (CodeLine line : codeFile) {
      String source = line.getSource();
      Matcher matcher = PATTERN.matcher(source);
      while (matcher.find()) {
        int start = matcher.start() + 1; // col number starts from 1
        if (!mergeToLastMessage(lastMessage, line.getLineNum(), start)) {
          lastMessage = Message.builder()
              .lineNum(line.getLineNum())
              .start(start)
              .end(start)
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
