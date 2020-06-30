package org.luncert.codescanner.exception;

public class InvalidConfigException extends RuntimeException {
  
  public InvalidConfigException(String msg) {
    super(msg);
  }
  
  public InvalidConfigException(Throwable e) {
    super(e);
  }
}
