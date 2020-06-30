package org.luncert.codescanner;

import com.google.common.collect.ImmutableList;
import org.luncert.codescanner.impl.RegexpScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class CodeScannerHolder {
  
  private final List<ICodeScanner> codeScannerChain = ImmutableList.<ICodeScanner>builder()
      .add(new RegexpScanner())
      .build();
  
  CodeScannerHolder(Map conf) {
    codeScannerChain.forEach(scanner -> scanner.init(conf));
  }
  
  List<Message> process(CodePool codePool) {
    List<Message> messageList = new ArrayList<>();
    
    for (CodeFile file : codePool) {
      for (ICodeScanner scanner : codeScannerChain) {
        messageList.addAll(scanner.process(file));
      }
    }
    
    return messageList;
  }
}
