package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Carson Crow
 */
public class Repository {

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The commits folder within .gitlet. */
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");
    /** The location of our refs directory, which contains file pointers to the branches and HEAD. */
    public static final File REFS_DIR = join(CWD, ".gitlet", "refs");
    /** The location of our blobs directory, which stores the snapshots of our files pointed to by commits,
     *  named by hash. */
    public static final File BLOBS_DIR = join(CWD, ".gitlet", "blobs");

    /** The location of our blobs that are in the staging area, ready to be committed. */
    public static final File ADD_STAGE_DIR = join(CWD, ".gitlet", "add-stage");
    public static final File RM_STAGE_DIR = join(CWD, ".gitlet", "rm-stage");

    public static void initRepo() {
        if (GITLET_DIR.exists()) {
            message("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        initDirectories();
        Commit initCommit = new Commit();
        initCommit.saveCommit();
        setRef("HEAD", initCommit.getID());
        setRef("master", initCommit.getID());
        setRef("current", "master");
    }

    /** Helper method to initRepo() that generates necessary directories. */
    private static void initDirectories() {
        GITLET_DIR.mkdir();
        COMMITS_DIR.mkdir();
        REFS_DIR.mkdir();
        BLOBS_DIR.mkdir();
        ADD_STAGE_DIR.mkdir();
        RM_STAGE_DIR.mkdir();
    }

    /** Adds the specified file in our repository to the staging area .gitlet/stage. */
    public static void stageFileForAddition(String fileName) {
        File fileToStage = new File(fileName);
        if (!fileToStage.exists()) {
            message("File does not exist.");
            return;
        }
        createFileWithContents(join(ADD_STAGE_DIR, fileName), readContentsAsString(fileToStage));
    }

    public static void stageFileForRemoval(String fileName) {
        // Remove file from .gitlet/add-stage
        File f = join(ADD_STAGE_DIR, fileName);
        boolean stagedForAddition = false;
        if (f.exists()) {
            stagedForAddition = true;
            if (!f.isDirectory()) {
                f.delete();
            }
        }


        File fileToStage = new File(fileName);
        if (!fileToStage.exists()) {
            message("File does not exist.");
            return;
        }
        createFileWithContents(join(RM_STAGE_DIR, fileName), readContentsAsString(fileToStage));
    }

    /** Moves the files in .gitlet/stage to .gitlet/blobs, and creates a new Commit pointing to these
     * blobs with the given commitMessage.
     */
    public static void commitStagedFiles(String commitMessage) {
        if (commitMessage.isBlank()) {
            message("Please enter a commit message.");
            return;
        }

        // Check that there are files staged for committing
        List<String> stagedFileNames = plainFilenamesIn(ADD_STAGE_DIR);
        if (stagedFileNames == null || stagedFileNames.size() == 0) {
            message("No changes added to the commit.");
            return;
        }

        // Create list of Blob objects from staged files
        List<Blob> stagedBlobs = new ArrayList<>();
        for (String fileName : stagedFileNames) {
            Blob b = new Blob(fileName, readContentsAsString(join(ADD_STAGE_DIR, fileName)));
            stagedBlobs.add(b);
        }

        // Add staged Blobs to .gitlet/blobs folder, keep track of ones that don't already exist
        List<String> newBlobIDs = new ArrayList<>();
        for (Blob b : stagedBlobs) {
            File newBlobFile = join(BLOBS_DIR, b.getID());
            if (!newBlobFile.exists()) {
                newBlobIDs.add(b.getID());
                b.saveBlob();
            }
        }

        // Create and save the new commit, and set reference files accordingly
        Commit newCommit = new Commit(getRef("HEAD"), commitMessage, newBlobIDs);
        newCommit.saveCommit();
        setRef("HEAD", newCommit.getID());
        setRef(getRef("current"), newCommit.getID());
        clearAddStage();
    }

    /** Deletes the specified file from .gitlet/stage. */
    public static void clearAddStage(String fileName) {
        File f = join(ADD_STAGE_DIR, fileName);
        if (!f.exists()) {
            message("No reason to remove the file.");
            return;
        }
        if (!f.isDirectory()) {
            f.delete();
        }
    }

    /** Prints the history of the Commit pointed to by HEAD. */
    public static void printHeadLog() {
        Commit head = readCommitFromFile(getRef("HEAD"));
        while (!head.getParents().isEmpty()) {
            System.out.println(head);
            head = readCommitFromFile(head.getParents().get(0));
        }
        System.out.println(head);
    }

    /** Prints information for all commits in .gitlet/commits. */
    public static void printGlobalLog() {
        List<String> commits = plainFilenamesIn(COMMITS_DIR);
        assert commits != null;
        for (String ID : commits) {
            System.out.println(readCommitFromFile(ID));
        }
    }

    /** Finds all commits with the given message and prints their IDs. */
    public static void printCommitsWithMessage(String message) {
        List<String> commits = plainFilenamesIn(COMMITS_DIR);
        assert commits != null;
        boolean messageNotFound = true;
        for (String ID : commits) {
            Commit c = readCommitFromFile(ID);
            if (c.getMessage().equals(message)) {
                messageNotFound = false;
                System.out.println(ID);
            }
        }
        if (messageNotFound) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void printStatus() {
        System.out.println("\n=== Branches ===");
        String currentBranch = getRef("current");
        List<String> branches = plainFilenamesIn(REFS_DIR);
        assert branches != null;
        Collections.sort(branches);
        for (String branch : branches) {
            if (!branch.equals("current") && !branch.equals("HEAD")) {
                if (branch.equals(currentBranch)) {
                    branch = "*" + branch;
                }
                System.out.println(branch);
            }
        }

        System.out.println("\n=== Staged Files ===");
        List<String> stagedFiles = plainFilenamesIn(ADD_STAGE_DIR);
        assert stagedFiles != null;
        Collections.sort(stagedFiles);
        for (String file : stagedFiles) {
            System.out.println(file);
        }

        System.out.println("\n=== Removed Files ===");

        System.out.println("\n=== Modifications Not Staged For Commit ===");

        System.out.println("\n=== Untracked Files ===");
    }

    /** Deletes all files in .gitlet/add-stage. */
    private static void clearAddStage() {
        List<String> stagedFileNames = plainFilenamesIn(ADD_STAGE_DIR);
        assert stagedFileNames != null;
        for (String name : stagedFileNames) {
            File f = join(ADD_STAGE_DIR, name);
            if (!f.isDirectory()) {
                f.delete();
            }
        }
    }

    /** Helper method that creates the file f and writes contents to it. */
    private static void createFileWithContents(File f, String contents) {
        try {
            f.createNewFile();
            writeContents(f, contents);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /** Returns the commitID contained in .gitlet/refs/fileName. */
    private static String getRef(String fileName) {
        return readContentsAsString(join(REFS_DIR, fileName));
    }

    /** Creates a file at .gitlet/refs/fileName if it doesn't already exist, and sets its
     * contents to be commitID string. */
    private static void setRef(String fileName, String commitID) {
        File f = join(REFS_DIR, fileName);
        if (!f.exists()) {
            try {
                f.createNewFile();
                writeContents(f, commitID);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        writeContents(join(REFS_DIR, fileName), commitID);
    }

    /** Returns the Commit object stored in the file .gitlet/commits. */
    private static Commit readCommitFromFile(String commitID) {
        if (!join(Repository.COMMITS_DIR, commitID).exists()) {
            throw error("Cannot read commit, no such commitID in .gitlet/commits");
        }
        return readObject(join(Repository.COMMITS_DIR, commitID), Commit.class);
    }

    /** Returns the Blob object stored in the file .gitlet/blobs. */
    private static Blob readBlobFromFile(String blobID) {
        if (!join(Repository.BLOBS_DIR, blobID).exists()) {
            throw error("Cannot read blob, no such blobID in .gitlet/blobs");
        }
        return readObject(join(Repository.BLOBS_DIR, blobID), Blob.class);
    }

}
