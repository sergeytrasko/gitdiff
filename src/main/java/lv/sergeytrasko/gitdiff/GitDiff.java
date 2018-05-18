package lv.sergeytrasko.gitdiff;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GitDiff {

    public static void main(String[] args) throws GitAPIException, IOException {
        String repoUrl = "https://github.com/ctco/cukes.git";
        String sourceCommitId = "8bb8c1ff7ce7cae6de549015248bfdc5f23cfd80";
        String targetCommitId = "c71b14e1488dfc75740fe31ca7aa091ec52d0010";
        File gitDir = new File("target/tmp");
        if (gitDir.exists()) {
            FileUtils.deleteDirectory(gitDir);
        }
        Git.cloneRepository().
                setURI(repoUrl).
                setDirectory(gitDir).
                call();

        Repository repository = new FileRepositoryBuilder().
                setGitDir(new File(gitDir, ".git")).
                readEnvironment().
                build();
        Git git = Git.wrap(repository);
        List<DiffEntry> diff = git.diff().
                setOldTree(treeParser(sourceCommitId + "^{tree}", repository)).
                setNewTree(treeParser(targetCommitId + "^{tree}", repository)).
                setPathFilter(PathFilter.create("cukes-samples")).
                call();

        for (DiffEntry diffEntry : diff) {
            System.out.println(diffEntry.getNewPath() + ", " + diffEntry.getChangeType());
            printContent(repository, targetCommitId, diffEntry.getNewPath());
        }
    }

    private static CanonicalTreeParser treeParser(String commitId, Repository repository) throws IOException {
        CanonicalTreeParser parser = new CanonicalTreeParser();
        ObjectId objectId = repository.resolve(commitId);
        parser.reset(repository.newObjectReader(), objectId);
        return parser;
    }

    private static void printContent(Repository repository, String commitId, String path) throws IOException {
        RevWalk revWalk = new RevWalk(repository);
        RevCommit commit = revWalk.parseCommit(repository.resolve(commitId));
        RevTree tree = commit.getTree();

        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(path));
            if (!treeWalk.next()) {
                throw new IllegalStateException("Did not find expected file '" + path + "'");
            }

            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);
            loader.copyTo(System.out);
        }

    }
}
