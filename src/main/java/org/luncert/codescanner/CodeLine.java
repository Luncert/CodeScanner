package org.luncert.codescanner;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeLine {
  
  private int lineNum;
  
  private String source;
}
