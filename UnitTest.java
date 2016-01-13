package gitlet;

import static org.junit.Assert.assertTrue;
import java.io.File;
import org.junit.Test;
import ucb.junit.textui;

/**
 * Class that provides JUnit tests for Gitlet, as well as a couple of utility
 * methods.
 * @author ZubinKoticha, Maaz Uddin
 *
 */
public class UnitTest {

    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    /** Tests init. */
    @Test
    public void testInit() {
        FileStructure fs = new FileStructure();
        fs.init();
        File f = new File(".gitlet/");
        assertTrue(f.exists());
    }
}