package gitlet;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Maaz Uddin, Zubin Koticha
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */

    public static void main(String... args) {
        FileStructure gitlet = new FileStructure();
        if (args.length == 0) {
            System.err.println("Please enter a command."); return;
        }
        switch (args[0].toLowerCase()) {
        case "init":
            gitlet.init(); return;
        case "add":
            gitlet.add(args[1]); return;
        case "commit":
            try {
                gitlet.commit(args[1]);
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Please enter a commit message.");
            }
            return;
        case "rm":
            gitlet.remove(args[1]); return;
        case "global-log":
            gitlet.gLog(); return;
        case "log":
            gitlet.log(); return;
        case "find":
            try {
                gitlet.find(args[1]);
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Please enter a message to find.");
            }
            return;
        case "status":
            gitlet.status(); return;
        case "checkout":
            if (args.length == 3) {
                gitlet.fileCheckout(args[2]);
            } else if (args.length == 4) {
                if (args[2].equals("--")) {
                    gitlet.checkout(args[1], args[3]);
                } else {
                    System.out.println("Incorrect operands."); return;
                }
            } else if (args.length == 2) {
                gitlet.branchCheckout(args[1]);
            }
            return;
        case "branch":
            gitlet.branch(args[1]); return;
        case "rm-branch":
            gitlet.rmBranch(args[1]); return;
        case "reset":
            gitlet.reset(args[1]); return;
        case "merge":
            gitlet.merge(args[1]); return;
        default:
            System.err.println("No command with that name exists."); return;
        }
    }
}
