package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/** The commit class.
 * @author Maaz Uddin, Zubin Koticha
 */
public class Commit implements Serializable {

    /** A new Commit in the current system that contains the file MESSAGE,
     *  the PARENT commit of this commit and a list of all the BLOBS that are
     *  pointed to by this commit. */
    Commit(String message, Commit parent) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        dt = date;
        dateReference = dateFormat.format(date);
        commitMessage = message;
        parentPointer = parent;
        fileData = new HashMap<>();
        filesStore = new ArrayList<>();
        fileNames = new ArrayList<>();
    }

    /** Add the file NAME and BYTES to their respective ArrayLists. */
    public void addNameByte(String name, byte[] bytes) {
        fileNames.addAll(parentPointer.fileNames);
        filesStore.addAll(parentPointer.filesStore);
        fileNames.add(name);
        filesStore.add(bytes);
    }

    /** Map the file NAME to its corresponding HASHVAL. */
    public void addFileData(String name, String hashVal) {
        fileData.put(name, hashVal);
    }

    /** Return the HashMap of filenames to hashvalues. */
    public HashMap<String, String> getFileData() {
        return fileData;
    }

    /** Returns a String value of this commit using the date, message,
     * parent, and list of filenames. */
    public String toString() {
        String answer = commitMessage + ": " + dateReference + " ";
        for (String s : fileData.keySet()) {
            answer += s + " ";
        }
        return answer;
    }

    /** Set HashCode for this particular commit. */
    public void setHash() {
        hash = Utils.sha1(toString());
    }

    /** Returns the HashCode for this particular commit. */
    public String getHash() {
        return hash;
    }

    /** Returns the Date and Time for this particular commit. */
    public String getDateTime() {
        return dateReference;
    }

    /** Returns the byte[] from the file named SHAVAL. */
    public byte[] getBytes(String shaVal) {
        File f = new File(".gitlet/" + shaVal);
        return Utils.readContents(f);
    }

    /** Returns the byte[] from the FILENAME. */
    public byte[] getBytesByFileName(String fileName) {
        String shaVal = getFileHash(fileName);
        File f = new File(".gitlet/" + shaVal);
        return Utils.readContents(f);
    }

    /** Returns an ArrayList of all the files as byte[] tracked in
     *  this particular commit. */
    public ArrayList<byte[]> getFileBytes() {
        arrayListHelper();
        return (ArrayList) filesStore;
    }

    /** Returns an ArrayList of all the filenames as strings tracked in
     *  this particular commit. */
    public ArrayList<String> getFileNames() {
        arrayListHelper();
        return (ArrayList) fileNames;
    }

    /** Updates filenames and filebytes. */
    public void arrayListHelper() {
        ArrayList<String> filenames = new ArrayList<>();
        ArrayList<byte[]> filebytes = new ArrayList<>();
        File tempFile;
        for (String name : fileData.keySet()) {
            tempFile = new File(".gitlet/" + fileData.get(name));
            filebytes.add(Utils.readContents(tempFile));
            filenames.add(name);
        }
        fileNames = filenames;
        filesStore = filebytes;
    }

    /** Return the message for this particular commit. */
    public String getMessage() {
        return commitMessage;
    }

    /** Return the parent of this particular commit. */
    public Commit getParent() {
        return parentPointer;
    }

    /** Returns a commit equality by comparing this commit to B. */
    public boolean isEquals(Commit b) {
        return (b.getDate().equals(dt));
    }

    /** Returns true iff this commit is before B. */
    public boolean before(Commit b) {
        return (dt.before(b.getDate()));
    }

    /** Returns the date of this commit. */
    public Date getDate() {
        return dt;
    }

    /** Returns true if this is the initial commit. */
    public boolean isInit() {
        return (parentPointer == null);
    }

    /** Returns a STRING hash given a FILENAME. */
    public String getFileHash(String fileName) {
        return fileData.get(fileName);
    }

    /** Returns true iff this contains FILENAME. */
    public boolean containsFile(String fileName) {
        return fileData.containsKey(fileName);
    }

    /** Date of this commit's creation. */
    private Date dt;
    /** Date as a string in the correct format. */
    private String dateReference;
    /** The commit message of this commit. */
    private String commitMessage;
    /** The parent commit of this commit. */
    private Commit parentPointer;
    /** A list of the byte[] of files tracked by this commit. */
    private List<byte[]> filesStore;
    /** A list of the filenames tracked by this commit. */
    private List<String> fileNames;
    /** A HashMap that maps a filename to its sha1 value. */
    private HashMap<String, String> fileData;
    /** The hash value of the commit using sha1. */
    private String hash;
}
