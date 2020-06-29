package org.luncert.codescanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CodeFile implements Iterable<CodeLine> {
  
  private List<String> buffer = new ArrayList<>();
  
  private String filePath;
  private InputStream inputStream;
  
  public CodeFile(File file) throws FileNotFoundException {
    filePath = file.getAbsolutePath();
    inputStream = new FileInputStream(file);
  }
  
  public CodeFile(String filePath, InputStream inputStream) {
    this.filePath = filePath;
    this.inputStream = inputStream;
  }
  
  @Override
  public Iterator<CodeLine> iterator() {
    return inputStream != null ? new FirstCodeFileIterator() : new BufferedCodeFileIterator();
  }
  
  private class FirstCodeFileIterator implements Iterator<CodeLine> {
    
    private BufferedReader reader = new BufferedReader(
        new InputStreamReader(CodeFile.this.inputStream));
    
    private int lineCount = 1;
    
    private String latestLine = null;
    
    @Override
    public boolean hasNext() {
      try {
        String line = reader.readLine();
        
        if (line != null) {
          CodeFile.this.buffer.add(line);
          latestLine = line;
          return true;
        }
        
        latestLine = null;
        return false;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  
    @Override
    public CodeLine next() {
      return new CodeLine(lineCount++, latestLine);
    }
  }
  
  private class BufferedCodeFileIterator implements Iterator<CodeLine> {
    
    private int lineCount = 1;
    
    private Iterator<String> iterator = CodeFile.this.buffer.iterator();
  
    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }
  
    @Override
    public CodeLine next() {
      return new CodeLine(lineCount++, iterator.next());
    }
  }
}
