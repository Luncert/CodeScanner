package org.luncert.codescanner;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.luncert.codescanner.exception.NoSourceConfiguredException;
import org.luncert.codescanner.impl.InvalidConfigException;
import org.luncert.codescanner.impl.LoadSourceException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CodePool implements Iterable<CodeFile> {
  
  private List<CodeFile> codeFileList = new ArrayList<>();
  
  @SuppressWarnings("unchecked")
  void loadWithConfig(Map conf) {
    if (conf.containsKey("rootPath")) {
      loadFromFS((String) conf.get("rootPath"));
    } else if (conf.containsKey("git")) {
      Object gitConf = conf.get("git");
      if (!(gitConf instanceof Map)) {
        throw new InvalidConfigException("git config should be a map");
      }
      
      try {
        loadFromGit((Map) gitConf);
      } catch (GitAPIException | IOException e) {
        throw new LoadSourceException(e);
      }
    } else {
      throw new NoSourceConfiguredException();
    }
  }
  
  private void loadFromGit(Map<String, String> gitConf) throws GitAPIException, IOException {
    // clone repo into memory
    InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription());
    
    CloneCommand cmd = Git.cloneRepository();
    cmd.setURI(gitConf.get("url"));
    cmd.setBranch(gitConf.getOrDefault("branch", "master"));
    cmd.setDirectory(repo.getDirectory());
    cmd.call();
    
    // get commit tree
    Ref head = repo.getRef("HEAD");
    RevWalk walk = new RevWalk(repo);
    RevCommit commit = walk.parseCommit(head.getObjectId());
    RevTree tree = commit.getTree();
    
    // traverse
    TreeWalk treeWalk = new TreeWalk(repo);
    treeWalk.addTree(tree);
    treeWalk.setRecursive(true);
    while (treeWalk.next()) {
      ObjectId id = treeWalk.getObjectId(0);
      ObjectLoader loader = repo.open(id);
      codeFileList.add(new CodeFile(treeWalk.getPathString(), loader.openStream()));
    }
  }
  
  private void loadFromFS(String rootPath) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public Iterator<CodeFile> iterator() {
    return codeFileList.iterator();
  }
}
