package gitlet;

import java.io.File;
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
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

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
    public static final File STAGE_DIR = join(CWD, ".gitlet", "stage");

    public static void initRepo() {
        if (GITLET_DIR.exists()) {
            message("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        initDirectoriesAndFiles();
        Commit initCommit = new Commit();
        initCommit.saveCommit();
        setRef("HEAD", initCommit.getID());
        setRef("master", initCommit.getID());
    }

    /** Helper method to initRepo() that generates necessary directories and files. */
    private static void initDirectoriesAndFiles() {
        GITLET_DIR.mkdir();
        COMMITS_DIR.mkdir();
        REFS_DIR.mkdir();
        createNewRefsFile("HEAD");
        createNewRefsFile("master");
        BLOBS_DIR.mkdir();
        STAGE_DIR.mkdir();
    }

    /** Adds the specified file in our repository to the staging area .gitlet/stage. */
    public static void stageFile(String fileName) {
        File fileToStage = new File(fileName);
        if (!fileToStage.exists()) {
            message("File does not exist.");
            return;
        }
        byte[] blobContents = readContents(fileToStage);
        String blobName = sha1(List.of(blobContents));
        File blobToStage = join(STAGE_DIR, blobName);
        try {
            blobToStage.createNewFile();
            writeContents(blobToStage, List.of(blobContents));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    /** Creates a blank file in .gitlet/refs. */
    private static void createNewRefsFile(String fileName) {
        try {
            join(REFS_DIR, fileName).createNewFile();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /** Returns the commitID contained in .gitlet/refs/fileName. */
    private static String getRef(String fileName) {
        return readContentsAsString(join(REFS_DIR, fileName));
    }

    /** Sets the contents of .gitlet/refs/fileName to be commitID string. */
    private static void setRef(String fileName, String commitID) {
        writeContents(join(REFS_DIR, fileName), commitID);
    }

}
