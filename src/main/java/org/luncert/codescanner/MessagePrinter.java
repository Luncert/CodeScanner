package org.luncert.codescanner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MessagePrinter {
  
  static void process(CodePool codePool, List<Message> messageList) {
    Map<MessageType, List<Message>> groupedMessages = messageList.stream()
        .collect(Collectors.groupingBy(Message::getType));
    
    List<Message> warningMsgList = groupedMessages.getOrDefault(MessageType.WARNING, Collections.emptyList());
    List<Message> errorMsgList = groupedMessages.getOrDefault(MessageType.ERROR, Collections.emptyList());
    
    System.out.println("--------------------");
    System.out.print("Warning: " + warningMsgList.size());
    System.out.println(" Error: " + errorMsgList.size());
  
    AtomicInteger count = new AtomicInteger(1);
  
    warningMsgList.forEach(msg -> printMessage(codePool,
        fillZero(messageList.size(), count.getAndIncrement()), msg));
    errorMsgList.forEach(msg -> printMessage(codePool,
        fillZero(messageList.size(), count.getAndIncrement()), msg));
  }
  
  private static String fillZero(int max, int v) {
    int tmp = v;
    while (tmp > 9) {
      max = max / 10;
      tmp = tmp / 10;
    }
    
    StringBuilder builder = new StringBuilder();
    while (max > 9) {
      max = max / 10;
      builder.append('0');
    }
    return builder.append(v).toString();
  }
  
  private static void printMessage(CodePool codePool, String count, Message msg) {
    Objects.requireNonNull(msg.getCodeFileName());
    
    String text = codePool.getCodeFile(msg.getCodeFileName())
        .getRange(msg.getLineNum() - 1, msg.getStart(), msg.getEnd() + 1);
    System.out.println(new StringBuilder()
        .append('[').append(count).append(']')
        .append(' ').append(msg.getType())
        .append(" - ").append(msg.getDescription()).append(" - ")
        .append(msg.getCodeFileName())
        .append(" (").append(msg.getLineNum())
        .append(':').append(msg.getStart()).append("): ")
        .append('`').append(text).append('`')
        .toString());
  }
}
