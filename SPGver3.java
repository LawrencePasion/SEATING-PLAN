import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;

/**
 * Classroom & Seating Plan Generator (console app)
 *
 * Compile: javac SPGver1.java
 * Run:     java SPGver1
 *
 * DRAWING SCALE (sizes are rounded UP to whole characters/lines)
 *   1 character across = 10 cm
 *   1 line down        = 20 cm   (terminal characters are ~2x taller than wide,
 *                                 so this keeps the drawing true to scale and landscape)
 *
 * ROOM WIDTH  = left-to-right (the whiteboard wall)  -> horizontal, the long side
 * ROOM LENGTH = front-to-back                        -> vertical
 */
public class SPGver3 {

    static final Scanner sc = new Scanner(System.in);
    static final Random random = new Random();

    static final int MAX_STUDENTS = 60;
    static final int MAX_COLUMNS = 8;
    static final int MAX_NAME_LENGTH = 12;

    static final int CM_PER_CHAR = 10;
    static final int CM_PER_LINE = 20;
    static final int FRONT_CLEAR_CM = 100;   // teacher table -> first row
    static final int BACK_CLEAR_CM = 80;     // behind the last row
    static final int PARTITION_CM = 100;     // extra gap added to the aisle at the partition

    static int roomWidthCm, roomLengthCm;
    static int deskWidthCm, deskDepthCm, chairCm;
    static int aisleCm, rowGapCm;
    static int teacherWidthCm, teacherDepthCm;
    static int partitions;                   // 0 = none, 1 = splits the seats into 2 sections

    static String[] seats = new String[0];   // seat i = row (i / cols), column (i % cols); null = empty
    static int rows, cols;
    static int partX = -1;                   // drawing column of the partition (-1 = none)
    static boolean classReady = false;

    // ============================================================
    // MAIN MENU
    // ============================================================

    public static void main(String[] args) {
        int choice;
        do {
            System.out.println("\n==========================================");
            System.out.println("  CLASSROOM & SEATING PLANNER");
            System.out.println("==========================================");
            System.out.println(" 1. Set up a new class");
            System.out.println(" 2. Classroom & furniture details");
            System.out.println(" 0. Exit");
            choice = readInt("Choose an option: ", 0, 2);

            if (choice == 1) {
                setupClass();
            } else if (choice == 2) {
                if (classReady) {
                    printDetails();
                } else {
                    System.out.println("No class yet. Use option 1 first.");
                }
            }
        } while (choice != 0);
        System.out.println("Goodbye!");
    }

    // ============================================================
    // SETUP
    // ============================================================

    static void setupClass() {
        System.out.println("\n=== CLASS SETUP ===");
        resetDefaults();

        if (!readYesNo("Use default sizes? (room " + roomText() + ", desks " + deskWidthCm + " x "
                + deskDepthCm + " cm) (y/n): ")) {
            deskWidthCm = readInt("Student desk WIDTH (side to side) in cm (40-200): ", 40, 200);
            deskDepthCm = readInt("Student desk DEPTH (front to back) in cm (30-120): ", 30, 120);
            chairCm = readInt("Chair size (square) in cm (30-80): ", 30, 80);
            aisleCm = readInt("Spacing between columns in cm (30-200): ", 30, 200);
            rowGapCm = readInt("Spacing between rows in cm (20-200): ", 20, 200);
            teacherWidthCm = readInt("Teacher's table WIDTH in cm (80-250): ", 80, 250);
            teacherDepthCm = readInt("Teacher's table DEPTH in cm (40-120): ", 40, 120);

            while (true) {
                roomWidthCm = (int) (readDouble("Room WIDTH in meters (left to right, along the whiteboard) (3-20): ", 3, 20) * 100 + 0.5);
                roomLengthCm = (int) (readDouble("Room LENGTH in meters (front to back) (3-20): ", 3, 20) * 100 + 0.5);
                if (maxRows() >= 1) {
                    break;
                }
                System.out.println("The room LENGTH is too short for this furniture (not even 1 row fits). Try again.");
            }
        }

        partitions = readInt("Partition? (0 = none, 1 = one partition splitting the seats into 2 sections): ", 0, 1);

        int maxCols = maxCols();
        int maxRows = maxRows();
        int capacity = maxCols * maxRows;
        int maxStudents = Math.min(MAX_STUDENTS, capacity);

        System.out.printf("%nThis room fits at most %d columns x %d rows = %d seats.%n", maxCols, maxRows, capacity);
        if (maxCols < MAX_COLUMNS) {
            System.out.println("Only " + maxCols + " of the usual " + MAX_COLUMNS + " columns fit across the room WIDTH.");
        }

        int students = readInt("Number of students (1-" + maxStudents + "): ", 1, maxStudents);

        int minCols = (students + maxRows - 1) / maxRows;      // ceil(students / maxRows)
        int maxColsAllowed = Math.min(maxCols, students);
        if (minCols > 1) {
            System.out.println("Note: only " + maxRows + " rows fit, so you need at least " + minCols + " columns.");
        }
        cols = readInt("How many columns? (" + minCols + "-" + maxColsAllowed + ", suggested "
                + maxColsAllowed + "): ", minCols, maxColsAllowed);
        rows = (students + cols - 1) / cols;

        seats = new String[rows * cols];
        for (int i = 0; i < students; i++) {
            seats[i] = "S" + (i + 1);
        }
        shuffleSeats();
        classReady = true;

        System.out.println("\nClass created! Students have placeholder names (S1, S2, ...).");
        showPlan("SEATING PLAN");
        editMenu();
    }

    static void resetDefaults() {
        roomWidthCm = 1100;     // 11 m wide  (left-right)
        roomLengthCm = 900;     // 9 m long   (front-back)  -> landscape
        deskWidthCm = 60;
        deskDepthCm = 45;
        chairCm = 40;
        aisleCm = 60;
        rowGapCm = 50;
        teacherWidthCm = 150;
        teacherDepthCm = 70;
        partitions = 0;
    }

    // Capacity is measured in DRAWN characters/lines, so the drawing always fits the room.
    static int maxCols() {
        int n = 0;
        while (n < MAX_COLUMNS && gridChars(n + 1) <= toChars(roomWidthCm)) {
            n++;
        }
        return n;
    }

    static int maxRows() {
        int n = 0;
        while (linesNeeded(n + 1) <= toLines(roomLengthCm)) {
            n++;
        }
        return n;
    }

    // drawn width of n columns (plus the partition gap if it is on)
    static int gridChars(int n) {
        int extra = (partitions == 1 && n >= 2) ? toChars(PARTITION_CM) : 0;
        return n * deskChars() + (n - 1) * toChars(aisleCm) + extra;
    }

    // drawn height of: whiteboard + teacher + front gap + n rows + back gap
    static int linesNeeded(int n) {
        return 1 + teacherLines() + toLines(FRONT_CLEAR_CM) + n * (deskLines() + chairLines())
                + (n - 1) * toLines(rowGapCm) + toLines(BACK_CLEAR_CM);
    }

    static String roomText() {
        return "WIDTH " + roomWidthCm / 100.0 + " m (left-right) x LENGTH " + roomLengthCm / 100.0 + " m (front-back)";
    }

    // ============================================================
    // EDIT MENU
    // ============================================================

    static void editMenu() {
        int choice;
        do {
            System.out.println("\n--- WHAT NEXT? ---");
            System.out.println(" 1. Re-shuffle seating");
            System.out.println(" 2. Assign a name to a specific seat (e.g. R1 C1)");
            System.out.println(" 3. Add more students (fills the next empty seats)");
            System.out.println(" 4. Finalize plan (print the final version)");
            System.out.println(" 0. Back to main menu");
            choice = readInt("Choose an option: ", 0, 4);

            switch (choice) {
                case 1:
                    shuffleSeats();
                    showPlan("SEATING PLAN (re-shuffled)");
                    break;
                case 2:
                    assignName();
                    break;
                case 3:
                    addStudents();
                    break;
                case 4:
                    showPlan("FINAL SEATING PLAN");
                    System.out.println("Plan finalized. Copy or screenshot it. Going back to the main menu.");
                    choice = 0;
                    break;
            }
        } while (choice != 0);
    }

    // shuffles every seat, empty ones included
    static void shuffleSeats() {
        Collections.shuffle(Arrays.asList(seats), random);
    }

    static void assignName() {
        int r = readInt("Row number (1-" + rows + "): ", 1, rows) - 1;
        int c = readInt("Column number (1-" + cols + "): ", 1, cols) - 1;
        int i = r * cols + c;

        if (seats[i] != null) {
            System.out.println(tag(i) + " currently holds " + seats[i] + " (will be replaced).");
        }
        String name = readText("New name: ");
        String problem = checkName(name, i);

        if (!problem.isEmpty()) {
            System.out.println(problem);
        } else {
            seats[i] = name;
            System.out.println(name + " is now at " + tag(i) + ".");
            showPlan("SEATING PLAN (updated)");
        }
    }

    static void addStudents() {
        if (firstEmpty() == -1) {
            System.out.println("Every seat is taken. Re-run setup for a bigger grid.");
            return;
        }
        System.out.println("Type a name to add a student, or 'done' to finish.");

        int i;
        while ((i = firstEmpty()) != -1) {
            String name = readText("Name: ");
            if (name.equalsIgnoreCase("done")) {
                break;
            }
            String problem = checkName(name, i);
            if (!problem.isEmpty()) {
                System.out.println(problem);
                continue;
            }
            seats[i] = name;
            System.out.println("Added " + name + " at " + tag(i) + ".");
        }

        if (firstEmpty() == -1) {
            System.out.println("All seats are now taken.");
        }
        showPlan("SEATING PLAN (updated)");
    }

    // "" if the name is fine, otherwise what is wrong (seat skip may keep its own name)
    static String checkName(String name, int skip) {
        if (name.isEmpty()) {
            return "Name can't be blank.";
        } else if (name.length() > MAX_NAME_LENGTH) {
            return "Name is too long (max " + MAX_NAME_LENGTH + " characters).";
        } else if (name.equalsIgnoreCase("done")) {
            return "'done' is reserved, pick another name.";
        }
        for (int i = 0; i < seats.length; i++) {
            if (i != skip && seats[i] != null && seats[i].equalsIgnoreCase(name)) {
                return "That name is already used, add a surname or initial.";
            }
        }
        return "";
    }

    static int countStudents() {
        int count = 0;
        for (String s : seats) {
            if (s != null) {
                count++;
            }
        }
        return count;
    }

    static int firstEmpty() {
        for (int i = 0; i < seats.length; i++) {
            if (seats[i] == null) {
                return i;
            }
        }
        return -1;
    }

    static String tag(int i) {
        return "R" + (i / cols + 1) + "C" + (i % cols + 1);
    }

    // ============================================================
    // DRAWING
    // ============================================================

    static void showPlan(String title) {
        System.out.println("\n########## " + title + " ##########");
        drawRoom();
        printLegend();
        printRoster();
    }

    static int toChars(int cm) {
        return (cm + CM_PER_CHAR - 1) / CM_PER_CHAR;     // rounded up
    }

    static int toLines(int cm) {
        return (cm + CM_PER_LINE - 1) / CM_PER_LINE;     // rounded up
    }

    // minimums: desk 6 wide (4 inside for a name) x 3 tall (border, name, border);
    // teacher table 9 wide so "TEACHER" fits
    static int deskChars()    { return Math.max(6, toChars(deskWidthCm)); }
    static int deskLines()    { return Math.max(3, toLines(deskDepthCm)); }
    static int chairChars()   { return Math.min(deskChars(), toChars(chairCm)); }
    static int chairLines()   { return toLines(chairCm); }
    static int teacherChars() { return Math.max(9, toChars(teacherWidthCm)); }
    static int teacherLines() { return Math.max(3, toLines(teacherDepthCm)); }

    // centers text in a field of the given width (cuts it if too long)
    static String center(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int left = (width - text.length()) / 2;
        return " ".repeat(left) + text + " ".repeat(width - text.length() - left);
    }

    // one empty room line; optionally with the partition drawn in it
    static char[] blank(boolean withPartition) {
        char[] b = new char[toChars(roomWidthCm)];
        Arrays.fill(b, ' ');
        if (withPartition && partX >= 0) {
            b[partX] = '|';
            b[partX + 1] = '|';
        }
        return b;
    }

    static void put(char[] line, int x, String text) {
        text.getChars(0, text.length(), line, x);
    }

    static void emit(String label, char[] line) {
        System.out.printf("%-5s|%s|%n", label, new String(line));
    }

    // wall line with a label in the middle
    static String wall(char fill, String text) {
        int left = (toChars(roomWidthCm) - text.length()) / 2;
        String f = String.valueOf(fill);
        return "+" + f.repeat(left) + text + f.repeat(toChars(roomWidthCm) - left - text.length()) + "+";
    }

    static void drawRoom() {
        int w = toChars(roomWidthCm);
        int dc = deskChars(), dl = deskLines(), cc = chairChars(), cl = chairLines();
        int aisle = toChars(aisleCm), extra = toChars(PARTITION_CM);
        boolean part = partitions == 1 && cols >= 2;
        int split = (cols + 1) / 2;                       // columns in the left section

        // left edge of every column (the partition gap comes after column `split`)
        int[] x = new int[cols];
        x[0] = (w - gridChars(cols)) / 2;
        for (int c = 1; c < cols; c++) {
            x[c] = x[c - 1] + dc + aisle + ((part && c == split) ? extra : 0);
        }
        partX = part ? x[split - 1] + dc + (aisle + extra - 2) / 2 : -1;

        // column headers
        char[] h = blank(false);
        for (int c = 0; c < cols; c++) {
            String t = "C" + (c + 1);
            put(h, x[c] + (dc - t.length()) / 2, t);
        }
        System.out.println("\n      " + new String(h));

        // front wall + whiteboard
        System.out.println("     " + wall('=', " FRONT - WIDTH " + roomWidthCm / 100.0 + " m "));
        char[] b = blank(false);
        put(b, 0, center("WHITEBOARD", w));
        emit("", b);

        // teacher's table
        int tc = teacherChars(), tl = teacherLines();
        for (int i = 0; i < tl; i++) {
            b = blank(false);
            put(b, (w - tc) / 2, (i == 0 || i == tl - 1)
                    ? "+" + "-".repeat(tc - 2) + "+"
                    : "|" + center(i == 1 ? "TEACHER" : "", tc - 2) + "|");
            emit("", b);
        }

        // space between teacher and first row
        for (int i = 0; i < toLines(FRONT_CLEAR_CM); i++) {
            emit("", blank(false));
        }

        // student rows: desks (+---+ taken, +...+ empty), then chairs (# taken, : empty)
        for (int r = 0; r < rows; r++) {
            for (int line = 0; line < dl; line++) {
                b = blank(true);
                for (int c = 0; c < cols; c++) {
                    String name = seats[r * cols + c];
                    String fill = (name != null) ? "-" : ".";
                    put(b, x[c], (line == 0 || line == dl - 1)
                            ? "+" + fill.repeat(dc - 2) + "+"
                            : "|" + center((line == 1 && name != null) ? name : "", dc - 2) + "|");
                }
                emit(line == 1 ? "R" + (r + 1) : "", b);
            }
            for (int line = 0; line < cl; line++) {
                b = blank(true);
                for (int c = 0; c < cols; c++) {
                    put(b, x[c] + (dc - cc) / 2, (seats[r * cols + c] != null ? "#" : ":").repeat(cc));
                }
                emit("", b);
            }
            if (r < rows - 1) {
                for (int i = 0; i < toLines(rowGapCm); i++) {
                    emit("", blank(true));
                }
            }
        }

        // leftover floor (includes the back clearance) so the room keeps its true length
        int spare = toLines(roomLengthCm) - linesNeeded(rows) + toLines(BACK_CLEAR_CM);
        for (int i = 0; i < spare; i++) {
            emit("", blank(false));
        }

        System.out.println("     " + wall('-', " BACK ") + "  LENGTH (front to back) = " + roomLengthCm / 100.0 + " m");
    }

    static void printLegend() {
        int seatsTotal = rows * cols;
        System.out.println("\n--- LEGEND (1 char across = " + CM_PER_CHAR + " cm, 1 line down = " + CM_PER_LINE
                + " cm, sizes rounded up) ---");
        System.out.println(" +----+   Student desk (taken): width " + deskWidthCm + " x depth " + deskDepthCm
                + " cm -> " + deskChars() + " chars x " + deskLines() + " lines");
        System.out.println(" +....+   Student desk (empty)");
        System.out.println(" ####     Chair (taken): " + chairCm + " x " + chairCm + " cm -> " + chairChars()
                + " chars x " + chairLines() + " lines");
        System.out.println(" ::::     Chair (empty)");
        System.out.println(" TEACHER  Teacher's table: width " + teacherWidthCm + " x depth " + teacherDepthCm
                + " cm -> " + teacherChars() + " chars x " + teacherLines() + " lines");
        System.out.println(" ||       Partition: " + (partitions == 1
                ? "ON, adds " + PARTITION_CM + " cm to the aisle and splits the seats into 2 sections" : "OFF"));
        System.out.println(" gap      Between columns: " + aisleCm + " cm -> " + toChars(aisleCm)
                + " chars | between rows: " + rowGapCm + " cm -> " + toLines(rowGapCm) + " lines");
        System.out.println(" room     WIDTH " + roomWidthCm / 100.0 + " m -> " + toChars(roomWidthCm)
                + " chars | LENGTH " + roomLengthCm / 100.0 + " m -> " + toLines(roomLengthCm) + " lines");
        System.out.println(" R1 / C1  Row and column numbers (use them to find or assign a seat)");
        System.out.println("Students: " + countStudents() + " | Seats: " + seatsTotal
                + " | Empty: " + (seatsTotal - countStudents()));
    }

    // full names, 3 per line
    static void printRoster() {
        System.out.println("\n--- WHO SITS WHERE ---");
        int n = 0;
        for (int i = 0; i < seats.length; i++) {
            if (seats[i] != null) {
                System.out.printf("%-24s", tag(i) + " = " + seats[i]);
                if (++n % 3 == 0) {
                    System.out.println();
                }
            }
        }
        if (n % 3 != 0) {
            System.out.println();
        }
    }

    static void printDetails() {
        System.out.println("\n--- CLASSROOM DETAILS ---");
        System.out.println("Room size     : " + roomText());
        System.out.println("Grid          : " + rows + " rows x " + cols + " columns");
        System.out.println("Room can hold : up to " + maxCols() + " columns x " + maxRows() + " rows");
        printLegend();
    }

    // ============================================================
    // INPUT HELPERS
    // ============================================================

    static int readInt(String prompt, int min, int max) {
        while (true) {
            try {
                int value = Integer.parseInt(readText(prompt));
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // fall through to the message below
            }
            System.out.println("Enter a whole number from " + min + " to " + max + ".");
        }
    }

    static double readDouble(String prompt, double min, double max) {
        while (true) {
            try {
                double value = Double.parseDouble(readText(prompt));
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // fall through to the message below
            }
            System.out.println("Enter a number from " + min + " to " + max + ".");
        }
    }

    static String readText(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    static boolean readYesNo(String prompt) {
        while (true) {
            String a = readText(prompt);
            if (a.equalsIgnoreCase("y") || a.equalsIgnoreCase("yes")) {
                return true;
            } else if (a.equalsIgnoreCase("n") || a.equalsIgnoreCase("no")) {
                return false;
            }
            System.out.println("Please type y or n.");
        }
    }
}