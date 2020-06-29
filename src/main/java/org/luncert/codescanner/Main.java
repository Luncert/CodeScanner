package org.luncert.codescanner;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

public class Main {
  
  private static final String DEFAULT_CONFIG_NAME = "CodeScanner.yml";
  
  public static void main(String[] args) throws FileNotFoundException {
    String configPath = DEFAULT_CONFIG_NAME;
    if (args.length >= 1) {
      configPath = args[0];
    }
    
    System.out.println("Config file path: " + configPath);
  
    Yaml yaml = new Yaml();
    Map conf = yaml.loadAs(new FileInputStream(configPath), Map.class);
    
    CodePool codePool = new CodePool();
    codePool.loadWithConfig(conf);
    
    CodeScannerHolder holder = new CodeScannerHolder(conf);
    List<Message> messageList = holder.process(codePool);
    
    MessagePrinter.process(messageList);
  }
}
