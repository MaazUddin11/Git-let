package gitlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.HashSet;


/** The structure of gitlet.
 * @author Maaz Uddin, Zubin Koticha
 */
public class FileStructure {

    /** Initializes a new FileStructure. */
    public FileStructure() {
    }

    /** Initialize gitlet with an empty stagingArea and an empty directory
     * of trackedFiles. */
    public void init() {
        File dir = new File(".gitlet");
        boolean successful = dir.mkdir();
        if (successful) {
            File stage = new File(".gitlet/stagingArea");
            stage.mkdir();
            File tracked = new File(".gitlet/trackedFiles");
            tracked.mkdir();
            File removed = new File(".gitlet/removedFiles");
            removed.mkdir();
            Commit initialCommit = new Commit("initial commit", null);
            initialCommit.setHash();
            head = initialCommit.getHash();
            currentBranch = "master";
            branchPointer.put(currentBranch, initialCommit);
            commits.put(head, initialCommit);
            serialize("initialCommit", initialCommit);
            serialize("branchPointers", branchPointer);
            serialize("commits", commits);
            serialize("head", head);
            serialize("currentBranch", currentBranch);
        } else {
            System.out.println("A gitlet version-control "
                    + "system already exists in the current directory.");
        }
    }

    /** Adds the file named FILENAME. */
    public void add(String fileName) {
        commits = (Hashtable<String, Commit>) deserialize(".gitlet/commits");
        Commit latest = getHead();
        File removed;
        for (String name : Utils.plainFilenamesIn(".gitlet/removedFiles")) {
            if (fileName.equals(name)) {
                removed = new File(".gitlet/removedFiles/" + name);
                removed.delete();
                return;
            }
        }
        HashMap<String, String> files = latest.getFileData();
        if (files.keySet().contains(fileName)) {
            File tempFile = new File(fileName);
            byte[] bytes = Utils.readContents(tempFile);
            if (files.get(fileName).equals(Utils.sha1(bytes))) {
                return;
            }
        }
        try {
            File sourceFile = new File(fileName);
            File destinationFile = new File(".gitlet/stagingArea/" + fileName);
            byte[] fileByteStream = Utils.readContents(sourceFile);
            Utils.writeContents(destinationFile, fileByteStream);
        } catch (IllegalArgumentException e) {
            System.err.println("File does not exist.");
        }
    }

    /** Commits the files with name MESSAGE. */
    public void commit(String message) {
        commitDeser();
        File stage = new File(".gitlet/stagingArea");
        File removed = new File(".gitlet/removedFiles");
        ArrayList<String> removedFiles;
        removedFiles = new ArrayList<>(Utils.plainFilenamesIn(removed));
        if (Utils.plainFilenamesIn(stage).isEmpty()
                && removedFiles.isEmpty()) {
            System.out.println("No changes added to the commit."); return;
        } else if (message.equals("")) {
            System.out.println("Please enter a commit message."); return;
        }
        Commit headCommit = commits.get(head);
        Commit latest = new Commit(message, commits.get(head));
        HashMap<String, String> parentFiles = headCommit.getFileData();
        String tempHash;
        for (String name : parentFiles.keySet()) {
            tempHash = parentFiles.get(name);
            latest.addFileData(name, tempHash);
        }
        if (!Utils.plainFilenamesIn(stage).isEmpty()) {
            File targetFile;
            byte[] targetBytes;
            for (String s : Utils.plainFilenamesIn(stage)) {
                targetFile = new File(s);
                targetBytes = Utils.readContents(targetFile);
                tempHash = Utils.sha1(targetBytes);
                latest.addFileData(s, tempHash); trackFiles(s);
            }
        }
        if (!removedFiles.isEmpty()) {
            ArrayList<String> toRemove = new ArrayList<>();
            for (String name : latest.getFileData().keySet()) {
                if (removedFiles.contains(name)) {
                    removed = new File(".gitlet/removedFiles/" + name);
                    removed.delete(); toRemove.add(name);
                }
            }
            for (String name : toRemove) {
                latest.getFileData().remove(name);
            }
        }
        String hashVal;
        for (String name : latest.getFileData().keySet()) {
            hashVal = latest.getFileData().get(name);
            if (!Utils.plainFilenamesIn(".gitlet").contains(hashVal)) {
                File sourceFile = new File(".gitlet/trackedFiles/" + name);
                File targetFile = new File(".gitlet/" + hashVal);
                byte[] sourceBytes = Utils.readContents(sourceFile);
                latest.addNameByte(name, sourceBytes);
                Utils.writeContents(targetFile, sourceBytes);

            }
        }
        latest.setHash();
        head = latest.getHash();
        branchPointer.put(currentBranch, latest);
        commits.put(head, latest);
        commitSer();
    }

    /** Helps for deserialization of commit. */
    public void commitDeser() {
        commits = (Hashtable<String, Commit>) deserialize(".gitlet/commits");
        head = (String) deserialize(".gitlet/head");
        currentBranch = (String) deserialize(".gitlet/currentBranch");
        branchPointer = (HashMap<String, Commit>)
                deserialize(".gitlet/branchPointers");
    }

    /** Helps for serialization of commit. */
    public void commitSer() {
        serialize("branchPointers", branchPointer);
        serialize("currentBranch", currentBranch);
        serialize("commits", commits);
        serialize("head", head);
    }

    /** Helper method that tracks a FILE by moving it from stagingArea
     *  to trackedFiles. */
    public void trackFiles(String file) {
        File stagedFile = new File(".gitlet/stagingArea/" + file);
        File trackedFile = new File(".gitlet/trackedFiles/" + file);
        byte[] fileByteStream = Utils.readContents(stagedFile);
        Utils.writeContents(trackedFile, fileByteStream);
        stagedFile.delete();
    }

    /** Remove the file FILENAME from the working directory if it was tracked in
     *  the current commit. If the file had been staged, then unstage it,
     *  but don't remove it from the working directory unless it was tracked
     *  in the current commit.. */
    public void remove(String fileName) {
        commits = (Hashtable<String, Commit>) deserialize(".gitlet/commits");
        Commit latest = getHead();
        ArrayList<byte[]> trackedFiles = latest.getFileBytes();
        File targetFile;
        File removed;
        if (!Utils.plainFilenamesIn(".").contains(fileName)
                && latest.getFileData().containsKey(fileName)) {
            removed = new File(".gitlet/" + latest.getFileData().get(fileName));
            byte[] fileByteStream = Utils.readContents(removed);
            File destination = new File(".gitlet/removedFiles/" + fileName);
            Utils.writeContents(destination, fileByteStream);
        }
        if (Utils.plainFilenamesIn(".gitlet/trackedFiles").contains(fileName)) {
            targetFile = new File(fileName);
            try {
                removed = new File(".gitlet/removedFiles/" + fileName);
                byte[] fileByteStream = Utils.readContents(targetFile);
                Utils.writeContents(removed, fileByteStream);
            } catch (IllegalArgumentException e) {
                System.err.println("No reason to remove the file.");
            }
            trackedFiles.remove(fileName);
            targetFile.delete();
        } else if (Utils.plainFilenamesIn(".gitlet/stagingArea")
                .contains(fileName)) {
            targetFile = new File(".gitlet/stagingArea/" + fileName);
            targetFile.delete();
        } else {
            System.err.println("No reason to remove the file.");
        }
        commits.put(head, latest);
        serialize("commits", commits);
        serialize("head", head);
    }


    /** Prints a log of this branch. */
    public void log() {
        Commit c = getHead();
        while (c != null) {
            System.out.println("===");
            System.out.println("Commit " + c.getHash());
            System.out.println(c.getDateTime());
            System.out.println(c.getMessage());
            System.out.println();
            c = c.getParent();
        }
    }

    /** Prints a global log of all commits. **/
    public void gLog() {
        commits = (Hashtable<String, Commit>) deserialize(".gitlet/commits");
        Enumeration e = commits.elements();
        while (e.hasMoreElements()) {
            printCommit((Commit) e.nextElement());
        }
    }

    /** Prints a single commit C. **/
    private void printCommit(Commit c) {
        commits = (Hashtable<String, Commit>) deserialize(".gitlet/commits");
        System.out.println("===");
        System.out.println("Commit " + c.getHash());
        System.out.println(c.getDateTime());
        System.out.println(c.getMessage());
        System.out.println();
    }

    /** Prints out the ids of all commits with given commit
     *  MESSAGE. */
    public void find(String message) {
        commits = (Hashtable<String, Commit>) deserialize(".gitlet/commits");
        Enumeration e = commits.elements();
        int counter = 0;
        while (e.hasMoreElements()) {
            Commit c = (Commit) e.nextElement();
            if (c.getMessage().equals(message)) {
                System.out.println(c.getHash());
                counter += 1;
            }
        }
        if (counter == 0) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** Displays what branches currently exist, and marks the current
     *  branch with a *. Also displays what files have been staged or marked
     *  for untracking. */
    public void status() {
        branchPointer = (HashMap<String, Commit>)
                deserialize(".gitlet/branchPointers");
        printBranches(branchPointer);
        printStatus("Staged Files",
                Utils.plainFilenamesIn(".gitlet/stagingArea"));
        printStatus("Removed Files",
                Utils.plainFilenamesIn(".gitlet/removedFiles"));
        printStatus("Modifications Not Staged For Commit",
                new ArrayList<>());
        printStatus("Untracked Files", new ArrayList<>());
    }

    /** Helper function for status that prints out the TITLE and the files
     *  within LST. */
    public void printStatus(String title, List<String> lst) {
        System.out.println("=== " + title + " ===");
        Collections.sort(lst);
        for (String s : lst) {
            System.out.println(s);
        }
        System.out.println();
    }

    /** Helper function for status that prints out the branches from
     * the HashMap of BRANCHES that is passed in. */
    public void printBranches(HashMap branches) {
        currentBranch = (String) deserialize(".gitlet/currentBranch");
        System.out.println("=== Branches ===");
        Set<String> s = branches.keySet();
        ArrayList<String> answer = new ArrayList<>();
        answer.addAll(s);
        Collections.sort(answer);
        for (String key: answer) {
            if (key.equals(currentBranch)) {
                System.out.println("*" + key);
            } else {
                System.out.println(key);
            }
        }
        System.out.println();
    }

    /** Takes the version of the FILENAME as it exists in the head commit, the
     *  front of the current branch, and puts it in the working directory. */
    public void fileCheckout(String fileName) {
        commits = (Hashtable<String, Commit>) deserialize(".gitlet/commits");
        head = (String) deserialize(".gitlet/head");
        currentBranch = (String) deserialize(".gitlet/currentBranch");
        Commit c = commits.get(head);
        ArrayList<String> files = c.getFileNames();
        if (!files.contains(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        } else {
            File sourceFile = new File(".gitlet/trackedFiles/" + fileName);
            File targetFile = new File(fileName);
            byte[] sourceBytes = Utils.readContents(sourceFile);
            Utils.writeContents(targetFile, sourceBytes);
        }
    }

    /** Takes the version of the FILENAME as it exists in the commit with the
     *  given COMMITID, and puts it in the working directory, overwriting the
     *  version of the file that's already there if there is one. */
    public void checkout(String commitID, String fileName) {
        commits = (Hashtable<String, Commit>) deserialize(".gitlet/commits");
        Commit c;
        c = commits.get(commitID);
        if (c == null) {
            System.err.println("No commit with that id exists.");
            return;
        }
        ArrayList<byte[]> fileBytes = c.getFileBytes();
        ArrayList<String> files = c.getFileNames();
        int i = files.indexOf(fileName);
        if (i == -1) {
            System.err.println("File does not exist in that commit.");
            return;
        }
        byte[] newBytes = fileBytes.get(i);
        File targetFile = new File(fileName);
        Utils.writeContents(targetFile, newBytes);

    }

    /** Takes all files in the commit at the head of the given BRANCHNAME, and
     *  puts them in the working directory, overwriting the versions of the
     *  files that are already there if they exist. */
    public void branchCheckout(String branchName) {
        branchPointer = (HashMap<String, Commit>)
                deserialize(".gitlet/branchPointers");
        currentBranch = (String) deserialize(".gitlet/currentBranch");
        head = (String) deserialize(".gitlet/head");
        if (branchName.equals(currentBranch)) {
            System.out.println("No need to "
                    + "checkout the current branch.");
            return;
        }
        Commit newCommit;
        newCommit = branchPointer.get(branchName);
        if (newCommit == null) {
            System.err.println("No such branch exists.");
            return;
        }
        ArrayList<String> newFiles = new ArrayList<>();
        newFiles.addAll(newCommit.getFileNames());
        ArrayList<byte[]> newFileBytes = new ArrayList<>();
        newFileBytes.addAll(newCommit.getFileBytes());
        Commit currentCommit = branchPointer.get(currentBranch);
        ArrayList<String> currentFiles = new ArrayList<>();
        currentFiles.addAll(currentCommit.getFileNames());
        File tempFile;
        byte[] tempBytes;
        if (branchContainsUntracked(branchName)) {
            System.out.println("There is an untracked file in the way;"
                    + " delete it or add it first.");
            return;
        } else {
            File deleteFile;
            for (String s : currentFiles) {
                if (!newFiles.contains(s)) {
                    deleteFile = new File(s);
                    deleteFile.delete();
                }
            }
            for (int i = 0; i < newFiles.size(); i++) {
                tempFile = new File(newFiles.get(i));
                tempBytes = newFileBytes.get(i);
                Utils.writeContents(tempFile, tempBytes);
            }
            clearStagingArea();
        }
        currentBranch = branchName;
        head = newCommit.getHash();
        serialize("currentBranch", currentBranch);
        serialize("head", head);
    }

    /** Returns true if a working file is untracked in the current branch
     *  and would be overwritten by the checkout of BRANCHNAME. */
    public boolean branchContainsUntracked(String branchName) {
        currentBranch = (String) deserialize(".gitlet/currentBranch");
        Commit currComm = branchPointer.get(currentBranch);
        Commit newComm = branchPointer.get(branchName);

        ArrayList<String> currFiles = currComm.getFileNames();
        ArrayList<String> newFiles = newComm.getFileNames();
        for (String s : Utils.plainFilenamesIn(".")) {
            if (!currFiles.contains(s) && newFiles.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /** Clears the staging area of all files. */
    public void clearStagingArea() {
        File stage;
        for (String s : Utils.plainFilenamesIn(".gitlet/stagingArea")) {
            stage = new File(s);
            stage.delete();
        }
    }

    /** Return the head pointer. **/
    public Commit getHead() {
        commits = (Hashtable<String, Commit>)
                deserialize(".gitlet/commits");
        head = (String) deserialize(".gitlet/head");
        return commits.get(head);
    }

    /** Creates a new branch named BRANCHNAME, and points it at the
     *  current head node. */
    public void branch(String branchName) {
        branchPointer = (HashMap<String, Commit>)
                deserialize(".gitlet/branchPointers");
        if (branchPointer != null) {
            if (branchPointer.containsKey(branchName)) {
                System.out.println("A branch with that name already exists.");
                return;
            } else {
                branchPointer.put(branchName, getHead());
            }
        } else {
            branchPointer.put(branchName, getHead());
        }
        serialize("branchPointers", branchPointer);
    }

    /** Deletes the branch BR. */
    public void rmBranch(String br) {
        branchPointer = (HashMap<String, Commit>)
                deserialize(".gitlet/branchPointers");
        if (!branchPointer.containsKey(br)) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (br.equals(currentBranch)) {
            System.out.println("Cannot remove the current branch.");
            return;
        } else {
            branchPointer.remove(br);
        }
        serialize("branchPointers", branchPointer);
    }

    /**  Checks out all the files tracked by the given COMMITID. Removes tracked
     *  files that are not present in the given file. Also moves the current
     *  branch's head to that commit node. */
    public void reset(String commitID) {
        commits = (Hashtable<String, Commit>)
                deserialize(".gitlet/commits");
        head = (String) deserialize(".gitlet/head");
        Commit c;
        try {
            c = commits.get(commitID);
        } catch (NullPointerException e) {
            System.err.println("No commit with that id exists.");
            return;
        }
        if (resetContainsUntracked(commitID)) {
            System.out.println("There is an untracked file in the way;"
                    + " delete it or add it first.");
            return;
        }
        HashMap<String, String> files = c.getFileData();
        for (String name : files.keySet()) {
            checkout(commitID, name);
        }
        File deleteFile;
        for (String name : Utils.plainFilenamesIn(".gitlet/trackedFiles")) {
            if (!files.keySet().contains(name)) {
                deleteFile = new File(".gitlet/trackedFiles/" + name);
                deleteFile.delete();
            }
        }
        head = c.getHash();
        clearStagingArea();
    }

    /** Returns true if a working file is untracked in the current commit
     *  and would be overwritten by the reset of COMMITID. */
    public boolean resetContainsUntracked(String commitID) {
        Commit currComm = commits.get(head);
        Commit newComm = commits.get(commitID);
        ArrayList<String> currFiles = currComm.getFileNames();
        ArrayList<String> newFiles = newComm.getFileNames();
        for (String s : Utils.plainFilenamesIn(".")) {
            if (!currFiles.contains(s) && newFiles.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /** Return the current branch BR. */
    public Commit getCurrentBranch() {
        branchPointer = (HashMap<String, Commit>)
                deserialize(".gitlet/branchPointers");
        currentBranch = (String) deserialize(".gitlet/currentBranch");
        return branchPointer.get(currentBranch);
    }

    /** Set currentBranch as BRANCH. **/
    public void setCurrentBranch(String branch) {
        currentBranch = (String) deserialize(".gitlet/currentBranch");
        currentBranch = branch;
        serialize("currentBranch", branch);
    }

    /** Returns true iff merge errors for GB. */
    public boolean mergeErrors(String gb) {
        branchPointer = (HashMap<String, Commit>)
                deserialize(".gitlet/branchPointers");
        if (branchContainsUntracked(gb)) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it or add it first.");
            return false;
        } else if (!Utils.plainFilenamesIn(".gitlet/stagingArea").isEmpty()
                || !Utils.plainFilenamesIn(".gitlet/removedFiles").isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return false;
        } else if (!branchPointer.containsKey(gb)) {
            System.out.println("A branch with that name does not exist.");
            return false;
        }
        return true;
    }

    /** Merges current branch with GIVENBRANCH. */
    public void merge(String givenBranch) {
        if (!mergeErrors(givenBranch)) {
            return;
        }
        boolean co = false;
        Commit gb = branchPointer.get(givenBranch);
        Commit cb = getCurrentBranch();
        if (gb.isEquals(cb)) {
            System.out.println("Cannot merge a branch with itself.");
        }
        Commit sp = findSplitPoint(cb, gb);
        if (sp.isEquals(gb)) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            return;
        } else if (sp.isEquals(getCurrentBranch())) {
            setCurrentBranch(givenBranch);
            System.out.println("Current branch fast-forwarded."); return;
        } else {
            for (String f : sp.getFileData().keySet()) {
                if (cb.containsFile(f) && gb.containsFile(f)) {
                    if (sameFile(f, sp, cb)) {
                        if (!(sameFile(f, sp, gb))) {
                            checkout(gb.getHash(), f);
                            add(f);
                        }
                    } else if (!sameFile(f, sp, cb)) {
                        if (!sameFile(f, sp, gb) && !sameFile(f, cb, gb)) {
                            co = true;
                            conflictHelper(f, cb, gb);
                        }
                    }
                } else if (cb.containsFile(f)) {
                    if (!sameFile(f, sp, cb)) {
                        co = true;
                        conflictHelper(f, cb, gb);
                    } else {
                        remove(f);
                    }
                } else if (gb.containsFile(f) && !sameFile(f, sp, gb)) {
                    co = true; conflictHelper(f, sp, cb);
                }
            }
            HashSet<String> cbUniqueFileNames = new HashSet<>();
            cbUniqueFileNames.addAll(cb.getFileData().keySet());
            cbUniqueFileNames.removeAll(sp.getFileData().keySet());
            for (String fn : cbUniqueFileNames) {
                if (gb.containsFile(fn) && (!sameFile(fn, cb, gb))) {
                    co = true; conflictHelper(fn, cb, gb);
                }
            }
            HashSet<String> gbUniqueFileNames = new HashSet<>();
            gbUniqueFileNames.addAll(gb.getFileData().keySet());
            gbUniqueFileNames.removeAll(sp.getFileData().keySet());
            for (String fn : gbUniqueFileNames) {
                checkout(gb.getHash(), fn); add(fn);
            }
        }
        printCon(co, givenBranch);
    }

    /** Print statements for C, GB if conflicted. */
    public void printCon(boolean c, String gb) {
        if (c) {
            System.out.println("Encountered a merge conflict.");
            return;
        } else {
            commit("Merged " + currentBranch + " with " + gb + ".");
            return;
        }
    }

    /** Helper method, taking a FLNM,
     * CBRANCH, GIVENBRANCH. */
    void conflictHelper(String flNm, Commit cBranch, Commit givenBranch) {
        byte[] currentBytes = null;
        if (cBranch.containsFile(flNm)) {
            currentBytes = cBranch.getBytesByFileName(flNm);
        }
        byte[] givenBytes = null;
        if (givenBranch.containsFile(flNm)) {
            givenBytes = givenBranch.getBytesByFileName(flNm);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<<<<<<< HEAD\n");
        if (currentBytes != null) {
            sb.append(new String(currentBytes));
            sb.append("\n");
        } else {
            sb.append("\n");
        }
        sb.append("=======\n");
        if (givenBytes != null) {
            sb.append(new String(givenBytes));
        } else {
            sb.append("\n");
        }
        sb.append(">>>>>>>");
        String s = sb.toString();
        byte[] newBytes = s.getBytes();
        try {
            File f = new File(".gitlet/" + cBranch.getFileHash(flNm));
            Utils.writeContents(f, newBytes);
        } catch (IOError e) {
            System.out.println("Error with writing to files.");
        }
    }

    /** Returns true iff contents FILENAME are the same in A and B. */
    public boolean sameFile(String fileName, Commit a, Commit b) {
        return (a.getFileHash(fileName).equals(b.getFileHash(fileName)));
    }

    /** Returns splitpoint between commit A and B. */
    public static Commit findSplitPoint(Commit a, Commit b) {
        if (a.isEquals(b)) {
            return a;
        } else if (a.isInit()) {
            return a;
        } else if (b.isInit()) {
            return b;
        } else if (a.before(b)) {
            return findSplitPoint(a, b.getParent());
        }   else if (b.before(a)) {
            return findSplitPoint(a.getParent(), b);
        } else {
            System.out.println(a + "/n" + b);
            System.out.println("There is some big error.");
            throw new Error();
        }
    }

    /** Serialize all objects OBJ to a file with name FOS. */
    public void serialize(String fos, Object... obj) {
        try {
            FileOutputStream fileStream;
            fileStream = new FileOutputStream(".gitlet/" + fos);
            ObjectOutputStream os = new ObjectOutputStream(fileStream);
            for (Object o : obj) {
                os.writeObject(o);
            }
            os.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /** Return deserialize objects of the file FIS. */
    public Object deserialize(String fis) {
        try {
            FileInputStream fileStream = new FileInputStream(fis);
            ObjectInputStream os;
            os = new ObjectInputStream(fileStream);
            try {
                Object obj = os.readObject();
                os.close();
                return obj;
            } catch (ClassNotFoundException e) {
                System.err.println(e.getMessage());
                return null;
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    /** HashMap keeping track of our branhces. **/
    private  HashMap<String, Commit> branchPointer = new HashMap<>();
    /** Hashtable of commits. **/
    private  Hashtable<String, Commit> commits = new Hashtable<>();
    /** Identifies current branch by name. **/
    private  String currentBranch;
    /** String identifying head pointer. **/
    private  String head;
}
