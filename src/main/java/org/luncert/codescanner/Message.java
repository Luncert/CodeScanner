package org.luncert.codescanner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
  
  public static final Message NULL = new Message(-1, -1, -1, null, null, null);
  
  private int lineNum;
  
  // inclusive
  private int start;
  
  // inclusive
  private int end;
  
  private String codeFileName;
  
  private MessageType type;
  
  private String description;
}
