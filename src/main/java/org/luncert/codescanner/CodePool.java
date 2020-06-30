package org.luncert.codescanner;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.luncert.codescanner.exception.NoSourceConfiguredException;
import org.luncert.codescanner.exception.InvalidConfigException;
import org.luncert.codescanner.exception.LoadSourceException;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.luncert.codescanner.Constants.CONF_KEY_AUTH;
import static org.luncert.codescanner.Constants.CONF_KEY_BRANCH;
import static org.luncert.codescanner.Constants.CONF_KEY_DELETE_ON_EXIT;
import static org.luncert.codescanner.Constants.CONF_KEY_GIT;
import static org.luncert.codescanner.Constants.CONF_KEY_ROOT_PATH;
import static org.luncert.codescanner.Constants.CONF_KEY_URL;

@Slf4j
public class CodePool implements Iterable<CodeFile> {
  
  private Map<String, CodeFile> codeFileMap = new HashMap<>();
  
  @SuppressWarnings("unchecked")
  void loadWithConfig(Map conf) {
    if (conf.containsKey(CONF_KEY_ROOT_PATH)) {
      loadFromFS((String) conf.get(CONF_KEY_ROOT_PATH), conf);
    } else if (conf.containsKey(CONF_KEY_GIT)) {
      Object gitConf = conf.get(CONF_KEY_GIT);
      if (!(gitConf instanceof Map)) {
        throw new InvalidConfigException("git config should be a map");
      }
      
      loadFromGit((Map) gitConf);
    } else {
      throw new NoSourceConfiguredException();
    }
  }
  
  private void loadFromGit(Map<String, Object> gitConf) {
    String url = (String) gitConf.get(CONF_KEY_URL);
    Objects.requireNonNull(url);
    
    // load repo info from fs, map data: git url -> absolute path in fs
    try {
      Repository repo;
      Map<String, String> repoInfoMap = loadRepoInfoMap();
      String repoPath = repoInfoMap.get(url);
      File repoDirectory = repoPath == null ? null : new File(repoPath);
      
      if (repoDirectory != null && repoDirectory.exists()) {
        repo = loadLocalRepository(repoDirectory);
      } else {
        repo = cloneRemoteRepository(gitConf);
        
        if (!(boolean) gitConf.get(CONF_KEY_DELETE_ON_EXIT)) {
          repoInfoMap.put(url, repo.getWorkTree().getAbsolutePath());
          saveRepoInfo(repoInfoMap);
        }
      }
      
      traverseRepo(gitConf, repo);
      
      // delete repo if required
      // TODO: update repo info map in .repoInfo
      if ((boolean) gitConf.get(CONF_KEY_DELETE_ON_EXIT)) {
        FileUtils.deleteDirectory(repo.getWorkTree());
        log.info("Deleted repo files in {}", repo.getWorkTree().getAbsolutePath());
      }
    } catch (GitAPIException | IOException e) {
      throw new LoadSourceException(e);
    }
  }
  
  private void loadFromFS(String rootPath, Map config) {
    Objects.requireNonNull(rootPath);
    try {
      Repository repo = loadLocalRepository(new File(rootPath));
      traverseRepo(config, repo);
    } catch (IOException e) {
      throw new LoadSourceException(e);
    }
  }
  
  private void traverseRepo(Map gitConf, Repository repo) throws IOException {
    // get commit tree
    Ref head = repo.getRef("HEAD");
    RevWalk walk = new RevWalk(repo);
    RevCommit commit = walk.parseCommit(head.getObjectId());
    RevTree tree = commit.getTree();
    
    // traverse
    TreeWalk treeWalk = new TreeWalk(repo);
    treeWalk.addTree(tree);
    
    treeWalk.setRecursive(true);
    
    // set root path
    String rootPath = (String) gitConf.get(CONF_KEY_ROOT_PATH);
    if (rootPath != null) {
      treeWalk.setFilter(PathFilter.create(rootPath));
    }
    
    while (treeWalk.next()) {
      ObjectId id = treeWalk.getObjectId(0);
      ObjectLoader loader = repo.open(id);
      codeFileMap.put(treeWalk.getPathString(), new CodeFile(treeWalk.getPathString(), loader.openStream()));
    }
    
    repo.close();
  }
  
  private Map<String, String> loadRepoInfoMap() throws IOException {
    File repoInfo = Paths.get(FileUtils.getTempDirectoryPath(), "CodeScanner", ".repoInfo").toFile();
    
    Map<String, String> repoInfoMap;
    
    if (repoInfo.exists()) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(repoInfo)));
      repoInfoMap = reader.lines().collect(Collectors.toMap(
          (line) -> line.substring(0, line.indexOf('=')),
          (line) -> line.substring(line.indexOf('=') + 1)));
    } else {
      repoInfoMap = new HashMap<>();
    }
    
    return repoInfoMap;
  }
  
  private void saveRepoInfo(Map<String, String> repoInfoMap) throws IOException {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry entry : repoInfoMap.entrySet()) {
      // '=' may occur in git url, so we put the key (git url) after the value (repo path)
      builder.append(entry.getValue()).append('=').append(entry.getKey()).append('\n');
    }
    
    File repoInfo = Paths.get(FileUtils.getTempDirectoryPath(), "CodeScanner", ".repoInfo").toFile();
    FileUtils.writeStringToFile(repoInfo, builder.toString(), Charset.forName("UTF8"));
  }
  
  private Repository loadLocalRepository(File repoDirectory) throws IOException {
    FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
    repositoryBuilder.setMustExist(true);
    repositoryBuilder.setGitDir(repoDirectory);
    Repository repo = repositoryBuilder.build();
    
    // pull newest version
    Git git = new Git(repo);
    PullCommand cmd = git.pull();
    cmd.setRebase(true);
    
    log.info("loaded local repository in {}", repoDirectory.getAbsolutePath());
    
    return repo;
  }
  
  private Repository cloneRemoteRepository(Map<String, Object> gitConf) throws GitAPIException {
    String url = (String) gitConf.get(CONF_KEY_URL);
    
    File workspace = getTempWorkspace();
    
    // clone repo into memory
    CloneCommand cmd = Git.cloneRepository();
    cmd.setURI(url);
    cmd.setBranch((String) gitConf.getOrDefault(CONF_KEY_BRANCH, "master"));
    cmd.setDirectory(workspace);
    
    // set credential
    Map<String, String> auth = (Map) gitConf.get(CONF_KEY_AUTH);
    if (auth != null) {
      if (auth.containsKey("token")) {
        String token = auth.get("token");
        Objects.requireNonNull(token, "auth token must be not null");
        cmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider("token", token));
      } else {
        String account = auth.get("account");
        String password = auth.get("password");
        Objects.requireNonNull(account);
        Objects.requireNonNull(password);
        cmd.setCredentialsProvider(
            new UsernamePasswordCredentialsProvider(account, password));
      }
    }
    
    Git git = cmd.call();
    
    log.info("{} -> {} = OK", url, workspace.getAbsolutePath());
    
    return git.getRepository();
  }
  
  private File getTempWorkspace() {
    String workspace = FileUtils.getTempDirectoryPath();
    return Paths.get(workspace, "CodeScanner", UUID.randomUUID().toString()).toFile();
  }
  
  @Override
  public @Nonnull Iterator<CodeFile> iterator() {
    return codeFileMap.values().iterator();
  }
  
  CodeFile getCodeFile(String codeFileName) {
    return codeFileMap.get(codeFileName);
  }
  
  /**
   * @return size of code files
   */
  int size() {
    return codeFileMap.size();
  }
}
