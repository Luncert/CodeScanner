package org.luncert.codescanner;

import java.util.List;
import java.util.Map;

public interface ICodeScanner {

  default void init(Map conf) {}
  
  List<Message> process(CodeFile codeFile);
}
