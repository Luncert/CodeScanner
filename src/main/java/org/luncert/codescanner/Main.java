package org.luncert.codescanner;

import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

@Slf4j
public class Main {
  
  private static final String DEFAULT_CONFIG_NAME = "CodeScanner.yml";
  
  public static void main(String[] args) throws FileNotFoundException {
    String configPath = DEFAULT_CONFIG_NAME;
    if (args.length >= 1) {
      configPath = args[0];
    }
    
    log.info("Using config file: {}", configPath);
  
    Yaml yaml = new Yaml();
    Map conf = yaml.loadAs(new FileInputStream(configPath), Map.class);
    
    CodeScannerHolder holder = new CodeScannerHolder(conf);
    
    CodePool codePool = new CodePool();
    codePool.loadWithConfig(conf);
    List<Message> messageList = holder.process(codePool);
    
    MessagePrinter.process(codePool, messageList);
  }
}
