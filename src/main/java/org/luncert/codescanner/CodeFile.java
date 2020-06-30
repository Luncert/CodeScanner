package org.luncert.codescanner;

import lombok.extern.slf4j.Slf4j;
import org.luncert.codescanner.exception.CodeScannerException;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CodeFile implements Iterable<CodeLine> {
  
  private String filePath;
  private List<String> buffer = Collections.emptyList();
  private BufferedReader source;
  
  public CodeFile(File file) throws FileNotFoundException {
    this(file.getAbsolutePath(), new FileInputStream(file));
  }
  
  public CodeFile(String filePath, InputStream inputStream) {
    this.filePath = filePath;
    source = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
  }
  
  /**
   * get range
   * @param lineIndex index of line, starts from 0
   * @param startIndex start inclusive
   * @param endIndex end exclusive
   * @return
   */
  public String getRange(int lineIndex, int startIndex, int endIndex) {
    checkIfBufferReady();
    
    if (lineIndex < 0 || lineIndex >= buffer.size()) {
      throw new CodeScannerException("invalid line number " + lineIndex);
    }
    
    // line number starts from 1
    String line = buffer.get(lineIndex);
    return line.substring(startIndex, endIndex);
  }
  
  public String getName() {
    return filePath;
  }
  
  private void checkIfBufferReady() {
    if (buffer.equals(Collections.emptyList())) {
      buffer = source.lines().collect(Collectors.toList());
    }
  }
  
  @Override
  public @Nonnull Iterator<CodeLine> iterator() {
    checkIfBufferReady();
    return new BufferedCodeFileIterator();
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
