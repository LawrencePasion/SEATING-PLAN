import java.util.Random;
import java.util.Scanner;

/**
 * Intelligent Classroom & Seating Plan Generator (console app)
 *
 * Only uses: Scanner, Random, loops, methods, arrays, if/else/switch,
 * Math.ceil/min/max, basic String methods and printf.
 * (No classes besides this one, no ArrayList, no try/catch, no file I/O.)
 *
 * Compile:  javac SeatingPlanGenerator3.java
 * Run:      java SeatingPlanGenerator
 *
 * HOW THE DRAWING IS SCALED
 *   1 character across = CM_PER_CHAR cm   (default 10 cm)
 *   1 line down        = CM_PER_LINE cm   (default 15 cm)
 * Terminal characters are about twice as tall as they are wide, so a
 * 10 cm : 15 cm ratio keeps the drawing looking roughly right.
 * Change the two numbers below if you want a different scale.
 * Every size is rounded UP to whole characters/lines (Math.ceil).
 */
public class SPGver1 {

    // ============================================================
    // SECTION 1: SETTINGS AND STORED DATA
    // ============================================================

    static final Scanner sc = new Scanner(System.in);
    static final Random random = new Random();

    // ----- limits -----
    static final int MAX_STUDENTS = 60;
    static final int MAX_COLUMNS = 8;       // the "default" column range is 1-8
    static final int MAX_NAME_LENGTH = 12;  // keeps the name roster tidy

    // ----- drawing scale -----
    static final int CM_PER_CHAR = 10;      // 1 character across = 10 cm
    static final int CM_PER_LINE = 15;      // 1 line down = 15 cm
    static final int LABEL_WIDTH = 5;       // left margin for row labels (R1, R2...)

    // ----- clearance the teacher needs (in cm) -----
    static final int FRONT_CLEAR_CM = 100;  // space between teacher desk and first row
    static final int BACK_CLEAR_CM = 80;    // space behind the last row of chairs

    // ----- room and furniture sizes (all in cm) -----
    static int roomWidthCm;
    static int roomLengthCm;
    static int deskWidthCm;
    static int deskDepthCm;
    static int chairCm;
    static int aisleCm;        // gap between columns
    static int rowGapCm;       // gap between rows
    static int teacherWidthCm;
    static int teacherDepthCm;

    // ----- the seating grid -----
    // seats[row][col] holds a student's name, or null if the seat is empty
    static String[][] seats = new String[0][0];
    static int rows = 0;
    static int cols = 0;
    static boolean classReady = false;   // becomes true after the first setup

    // counts how many lines the room drawing has printed (used to fill the room)
    static int linesDrawn = 0;

    // ============================================================
    // SECTION 2: MAIN MENU
    // ============================================================

    public static void main(String[] args) {
        int choice;

        do {
            System.out.println("\n==========================================");
            System.out.println("  INTELLIGENT CLASSROOM & SEATING PLANNER");
            System.out.println("==========================================");
            System.out.println(" 1. Set up a new class");
            System.out.println(" 2. Classroom & furniture details");
            System.out.println(" 0. Exit");
            choice = readIntRange("Choose an option: ", 0, 2);

            switch (choice) {
                case 1:
                    setupClass();
                    break;
                case 2:
                    if (classReady) {
                        printDetails();
                    } else {
                        System.out.println("No class yet. Use option 1 first.");
                    }
                    break;
                case 0:
                    System.out.println("Goodbye!");
                    break;
                default:
                    break;
            }
        } while (choice != 0);
    }

    // ============================================================
    // SECTION 3: SETUP (room size -> how many seats fit -> students -> columns)
    // ============================================================

    static void setupClass() {
        System.out.println("\n=== CLASS SETUP ===");

        // start from the default sizes every time
        resetDefaults();

        // ---- step 1: room + furniture sizes ----
        boolean useDefaults = readYesNo("Use default sizes? (8 x 10 m room, 60 x 45 cm desks) (y/n): ");

        if (!useDefaults) {
            // furniture first, because the room fit depends on it
            deskWidthCm = readIntRange("Student desk width in cm (40-200): ", 40, 200);
            deskDepthCm = readIntRange("Student desk depth in cm (30-120): ", 30, 120);
            chairCm = readIntRange("Chair size in cm (30-80): ", 30, 80);
            aisleCm = readIntRange("Spacing between columns in cm (30-200): ", 30, 200);
            rowGapCm = readIntRange("Spacing between rows in cm (20-200): ", 20, 200);
            teacherWidthCm = readIntRange("Teacher's table width in cm (80-250): ", 80, 250);
            teacherDepthCm = readIntRange("Teacher's table depth in cm (40-120): ", 40, 120);

            // keep asking for the room until at least ONE row of desks fits
            while (true) {
                // + 0.5 rounds to the nearest cm (avoids 729.99999 problems)
                roomWidthCm = (int) (readDoubleRange("Room width in meters (3-12): ", 3, 12) * 100 + 0.5);
                roomLengthCm = (int) (readDoubleRange("Room length in meters (3-20): ", 3, 20) * 100 + 0.5);

                if (maxRowsThatFit() >= 1) {
                    break;
                }
                System.out.println("That room is too short for this furniture (not even 1 row fits). Try again.");
            }
        }

        // ---- step 2: work out how many columns/rows THIS room can hold ----
        int maxCols = maxColumnsThatFit();
        int maxRows = maxRowsThatFit();
        int capacity = maxCols * maxRows;
        int maxStudents = Math.min(MAX_STUDENTS, capacity);

        System.out.println();
        System.out.printf("This room fits at most %d columns x %d rows = %d seats.%n", maxCols, maxRows, capacity);
        if (maxCols < MAX_COLUMNS) {
            System.out.println("Column range adjusted: the default is 1-" + MAX_COLUMNS
                    + ", but only 1-" + maxCols + " fit across a "
                    + (roomWidthCm / 100.0) + " m wide room (so the drawing stays true to scale).");
        }

        // ---- step 3: number of students (limited by the room) ----
        int studentCount = readIntRange("Number of students (1-" + maxStudents + "): ", 1, maxStudents);

        // ---- step 4: choose columns ----
        // fewest columns allowed = students / rows that fit (rounded up)
        int minCols = (int) Math.ceil((double) studentCount / maxRows);
        // you can't have more columns than students, or more than the room fits
        int maxColsAllowed = Math.min(maxCols, studentCount);

        // suggested columns from class-size tiers (else-if chain)
        int suggested;
        if (studentCount <= 10) {
            suggested = 3;
        } else if (studentCount <= 20) {
            suggested = 4;
        } else if (studentCount <= 30) {
            suggested = 5;
        } else if (studentCount <= 40) {
            suggested = 6;
        } else {
            suggested = 8;
        }
        // keep the suggestion inside the allowed range
        suggested = Math.max(minCols, Math.min(suggested, maxColsAllowed));

        if (minCols > 1) {
            System.out.println("Note: only " + maxRows + " rows fit lengthwise, so you need at least "
                    + minCols + " columns for " + studentCount + " students.");
        }
        cols = readIntRange("How many columns? (" + minCols + "-" + maxColsAllowed
                + ", suggested " + suggested + "): ", minCols, maxColsAllowed);
        rows = (int) Math.ceil((double) studentCount / cols);

        // ---- step 5: seat everyone with placeholder names S1, S2, ... then shuffle ----
        seats = new String[rows][cols];
        for (int i = 0; i < studentCount; i++) {
            seats[i / cols][i % cols] = "S" + (i + 1);   // i / cols = row, i % cols = column
        }
        shuffleSeats();
        classReady = true;

        System.out.println("\nClass created! Students have placeholder names (S1, S2, ...).");
        System.out.println("Use the menu below to rename them and adjust the plan.");
        showPlan("SEATING PLAN");
        editMenu();
    }

    // sets every size back to the default values
    static void resetDefaults() {
        roomWidthCm = 800;
        roomLengthCm = 1000;
        deskWidthCm = 60;
        deskDepthCm = 45;
        chairCm = 40;
        aisleCm = 60;
        rowGapCm = 50;
        teacherWidthCm = 150;
        teacherDepthCm = 70;
    }

    // how many columns fit across the room width
    // formula: cols*desk + (cols-1)*aisle <= width   ->   cols <= (width+aisle) / (desk+aisle)
    static int maxColumnsThatFit() {
        int fit = (roomWidthCm + aisleCm) / (deskWidthCm + aisleCm);
        return Math.min(fit, MAX_COLUMNS);
    }

    // how many rows fit along the room length
    // room = teacher desk + front clearance + rows*(desk+chair) + (rows-1)*gap + back clearance
    static int maxRowsThatFit() {
        int free = roomLengthCm - teacherDepthCm - FRONT_CLEAR_CM - BACK_CLEAR_CM + rowGapCm;
        if (free <= 0) {
            return 0;
        }
        return free / (deskDepthCm + chairCm + rowGapCm);
    }

    // ============================================================
    // SECTION 4: EDIT MENU (appears after the drawing + legend)
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
            choice = readIntRange("Choose an option: ", 0, 4);

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
                    choice = 0;   // leave this menu
                    break;
                default:
                    break;
            }
        } while (choice != 0);
    }

    // Fisher-Yates shuffle over EVERY seat (empty seats get shuffled too)
    static void shuffleSeats() {
        int total = rows * cols;
        for (int i = total - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);       // random position from 0 to i

            // swap seat i with seat j (turn a single number into row/col with / and %)
            String temp = seats[i / cols][i % cols];
            seats[i / cols][i % cols] = seats[j / cols][j % cols];
            seats[j / cols][j % cols] = temp;
        }
    }

    // put a chosen name into a chosen seat
    static void assignName() {
        int r = readIntRange("Row number (1-" + rows + "): ", 1, rows) - 1;   // -1 = array index
        int c = readIntRange("Column number (1-" + cols + "): ", 1, cols) - 1;

        if (seats[r][c] != null) {
            System.out.println("R" + (r + 1) + "C" + (c + 1) + " currently holds " + seats[r][c] + " (will be replaced).");
        }
        String name = readText("New name: ");
        String problem = checkName(name, r, c);

        if (problem.length() > 0) {
            System.out.println(problem);
        } else {
            seats[r][c] = name;
            System.out.println(name + " is now at R" + (r + 1) + "C" + (c + 1) + ".");
            showPlan("SEATING PLAN (updated)");
        }
    }

    // keep adding students into the first empty seat until "done" or the room is full
    static void addStudents() {
        if (countStudents() == rows * cols) {
            System.out.println("Every seat is taken. Re-run setup for a bigger grid.");
            return;
        }

        System.out.println("Type a name to add a student, or 'done' to finish.");
        while (countStudents() < rows * cols) {
            String name = readText("Name: ");

            if (name.equalsIgnoreCase("done")) {
                break;
            }

            // find the first empty seat (row by row)
            int er = -1;
            int ec = -1;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (seats[r][c] == null && er == -1) {
                        er = r;
                        ec = c;
                    }
                }
            }

            String problem = checkName(name, er, ec);
            if (problem.length() > 0) {
                System.out.println(problem);
                continue;   // ask again
            }
            seats[er][ec] = name;
            System.out.println("Added " + name + " at R" + (er + 1) + "C" + (ec + 1) + ".");
        }

        if (countStudents() == rows * cols) {
            System.out.println("All seats are now taken.");
        }
        showPlan("SEATING PLAN (updated)");
    }

    // returns "" if the name is fine, otherwise a message saying what is wrong
    // (r, c) is the seat being filled, so a seat can keep its own name
    static String checkName(String name, int r, int c) {
        if (name.length() == 0) {
            return "Name can't be blank.";
        } else if (name.length() > MAX_NAME_LENGTH) {
            return "Name is too long (max " + MAX_NAME_LENGTH + " characters).";
        } else if (name.equalsIgnoreCase("done")) {
            return "'done' is reserved, pick another name.";
        } else if (isNameUsed(name, r, c)) {
            return "That name is already used, add a surname or initial.";
        }
        return "";
    }

    static boolean isNameUsed(String name, int skipR, int skipC) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                boolean sameSeat = (r == skipR && c == skipC);
                if (seats[r][c] != null && !sameSeat && seats[r][c].equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    static int countStudents() {
        int count = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (seats[r][c] != null) {
                    count++;
                }
            }
        }
        return count;
    }

    // ============================================================
    // SECTION 5: DRAWING (everything printed to scale)
    // ============================================================

    // title + drawing + legend + name roster, always together
    static void showPlan(String title) {
        System.out.println("\n########## " + title + " ##########");
        drawRoom();
        printLegend();
        printRoster();
    }

    // ----- unit conversions: centimeters -> characters / lines (rounded UP) -----
    static int toChars(int cm) {
        return (int) Math.ceil(cm / (double) CM_PER_CHAR);
    }

    static int toLines(int cm) {
        return (int) Math.ceil(cm / (double) CM_PER_LINE);
    }

    // ----- how big each piece is when drawn -----
    // desk needs at least 6 chars wide (4 inside for a name) and 3 lines tall (border, name, border)
    static int deskChars() {
        return Math.max(6, toChars(deskWidthCm));
    }

    static int deskLines() {
        return Math.max(3, toLines(deskDepthCm));
    }

    // chair is never drawn wider than its desk
    static int chairChars() {
        return Math.min(deskChars(), Math.max(2, toChars(chairCm)));
    }

    static int chairLines() {
        return Math.max(1, toLines(chairCm));
    }

    // ----- text helpers -----
    static String spaces(int n) {
        return " ".repeat(Math.max(0, n));
    }

    static String center(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        int left = (width - text.length()) / 2;
        return spaces(left) + text + spaces(width - text.length() - left);
    }

    static String truncate(String text, int max) {
        if (text.length() > max) {
            return text.substring(0, max);
        }
        return text;
    }

    // prints ONE line of the room: label (R1...) + left wall + content + right wall
    static void printRoomLine(String label, String inner) {
        int roomChars = toChars(roomWidthCm);
        System.out.println(label + spaces(LABEL_WIDTH - label.length())
                + "|" + inner + spaces(roomChars - inner.length()) + "|");
        linesDrawn++;
    }

    static void drawRoom() {
        int roomChars = toChars(roomWidthCm);
        int roomLines = toLines(roomLengthCm);
        int dChars = deskChars();
        int dLines = deskLines();
        int cChars = chairChars();
        int cLines = chairLines();
        int aisle = toChars(aisleCm);

        // total width of the desk grid, and the margin that centers it in the room
        int gridChars = cols * dChars + (cols - 1) * aisle;
        int margin = Math.max(0, (roomChars - gridChars) / 2);

        linesDrawn = 0;
        System.out.println();

        // ----- column headers (C1, C2, ...) -----
        String header = spaces(LABEL_WIDTH + 1 + margin);   // +1 skips the left wall
        for (int c = 0; c < cols; c++) {
            header = header + center("C" + (c + 1), dChars);
            if (c < cols - 1) {
                header = header + spaces(aisle);
            }
        }
        System.out.println(header);

        // ----- front wall + whiteboard -----
        System.out.println(spaces(LABEL_WIDTH) + "+" + "=".repeat(roomChars) + "+");
        printRoomLine("", center("FRONT OF CLASS - WHITEBOARD", roomChars));

        // ----- teacher's desk (centered) -----
        int tChars = Math.max(9, toChars(teacherWidthCm));   // 9 so the word TEACHER fits
        int tLines = Math.max(3, toLines(teacherDepthCm));
        int tPad = Math.max(0, (roomChars - tChars) / 2);
        for (int line = 0; line < tLines; line++) {
            String inner;
            if (line == 0 || line == tLines - 1) {
                inner = spaces(tPad) + "+" + "-".repeat(tChars - 2) + "+";
            } else if (line == 1) {
                inner = spaces(tPad) + "|" + center("TEACHER", tChars - 2) + "|";
            } else {
                inner = spaces(tPad) + "|" + spaces(tChars - 2) + "|";
            }
            printRoomLine("", inner);
        }

        // ----- empty space between teacher and the first row -----
        for (int i = 0; i < toLines(FRONT_CLEAR_CM); i++) {
            printRoomLine("", "");
        }

        // ----- the student rows -----
        for (int r = 0; r < rows; r++) {

            // desk lines: top border, name line(s), bottom border
            for (int line = 0; line < dLines; line++) {
                String inner = spaces(margin);

                for (int c = 0; c < cols; c++) {
                    boolean taken = (seats[r][c] != null);

                    if (line == 0 || line == dLines - 1) {
                        // borders: ---- for a taken desk, .... for an empty one
                        String fill = taken ? "-" : ".";
                        inner = inner + "+" + fill.repeat(dChars - 2) + "+";
                    } else if (line == 1) {
                        // name line (cut short if it does not fit; the roster has full names)
                        String shown = "";
                        if (taken) {
                            shown = truncate(seats[r][c], dChars - 2);
                        }
                        inner = inner + "|" + center(shown, dChars - 2) + "|";
                    } else {
                        // extra middle lines for deeper desks
                        inner = inner + "|" + spaces(dChars - 2) + "|";
                    }

                    if (c < cols - 1) {
                        inner = inner + spaces(aisle);
                    }
                }
                // the row label (R1, R2...) goes on the desk's name line
                String label = "";
                if (line == 1) {
                    label = "R" + (r + 1);
                }
                printRoomLine(label, inner);
            }

            // chair lines: # = taken seat, : = empty seat
            for (int line = 0; line < cLines; line++) {
                String inner = spaces(margin);
                int chairLeft = (dChars - cChars) / 2;   // centers the chair under the desk

                for (int c = 0; c < cols; c++) {
                    String fill = (seats[r][c] != null) ? "#" : ":";
                    inner = inner + spaces(chairLeft) + fill.repeat(cChars)
                            + spaces(dChars - cChars - chairLeft);
                    if (c < cols - 1) {
                        inner = inner + spaces(aisle);
                    }
                }
                printRoomLine("", inner);
            }

            // gap before the next row (not after the last one)
            if (r < rows - 1) {
                for (int i = 0; i < toLines(rowGapCm); i++) {
                    printRoomLine("", "");
                }
            }
        }

        // ----- leftover floor space so the room keeps its true length -----
        while (linesDrawn < roomLines) {
            printRoomLine("", "");
        }

        // ----- back wall -----
        System.out.println(spaces(LABEL_WIDTH) + "+" + "-".repeat(roomChars) + "+  <- back of class");
    }

    // legend: each drawing element with its real-life size and drawn size
    static void printLegend() {
        System.out.println("\n--- LEGEND (drawn to scale) ---");
        System.out.println("Scale: 1 character across = " + CM_PER_CHAR + " cm | 1 line down = " + CM_PER_LINE
                + " cm (sizes are rounded up to whole characters/lines)");
        System.out.println(" +----+   Student desk (taken) : " + deskWidthCm + " x " + deskDepthCm
                + " cm  -> " + deskChars() + " chars x " + deskLines() + " lines");
        System.out.println(" +....+   Student desk (empty) : same size, dotted borders");
        System.out.println(" ####     Chair (taken)        : " + chairCm + " x " + chairCm
                + " cm  -> " + chairChars() + " chars x " + chairLines() + " lines");
        System.out.println(" ::::     Chair (empty)        : same size");
        System.out.println(" TEACHER  Teacher's table      : " + teacherWidthCm + " x " + teacherDepthCm
                + " cm  -> " + Math.max(9, toChars(teacherWidthCm)) + " chars x "
                + Math.max(3, toLines(teacherDepthCm)) + " lines");
        System.out.println(" gap      Between columns      : " + aisleCm + " cm -> " + toChars(aisleCm)
                + " chars | between rows: " + rowGapCm + " cm -> " + toLines(rowGapCm) + " lines");
        System.out.printf(" room     %.1f m x %.1f m -> %d chars x %d lines%n",
                roomWidthCm / 100.0, roomLengthCm / 100.0, toChars(roomWidthCm), toLines(roomLengthCm));
        System.out.println(" R1 / C1  Row and column numbers (use them to find or assign a seat)");
        System.out.println("Students: " + countStudents() + " | Seats: " + (rows * cols)
                + " | Empty: " + (rows * cols - countStudents()));
    }

    // full names, 3 per line (the desks only have room for the first few letters)
    static void printRoster() {
        System.out.println("\n--- WHO SITS WHERE ---");
        int shownOnLine = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (seats[r][c] != null) {
                    System.out.printf("%-24s", "R" + (r + 1) + "C" + (c + 1) + " = " + seats[r][c]);
                    shownOnLine++;
                    if (shownOnLine % 3 == 0) {
                        System.out.println();
                    }
                }
            }
        }
        if (shownOnLine % 3 != 0) {
            System.out.println();
        }
    }

    // main menu option 2
    static void printDetails() {
        System.out.println("\n--- CLASSROOM DETAILS ---");
        System.out.printf("Room size      : %.1f m wide x %.1f m long%n", roomWidthCm / 100.0, roomLengthCm / 100.0);
        System.out.println("Grid           : " + rows + " rows x " + cols + " columns");
        System.out.println("Room can hold  : up to " + maxColumnsThatFit() + " columns x " + maxRowsThatFit() + " rows");
        System.out.printf("Space used     : %.2f m x %.2f m%n", requiredWidthCm() / 100.0, requiredDepthCm() / 100.0);
        printLegend();
    }

    static int requiredWidthCm() {
        return cols * deskWidthCm + (cols - 1) * aisleCm;
    }

    static int requiredDepthCm() {
        return teacherDepthCm + FRONT_CLEAR_CM + rows * (deskDepthCm + chairCm)
                + (rows - 1) * rowGapCm + BACK_CLEAR_CM;
    }

    // ============================================================
    // SECTION 6: INPUT HELPERS (hasNextInt / hasNextDouble instead of try/catch)
    // ============================================================

    // keeps asking until the user types a whole number between min and max
    static int readIntRange(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            if (sc.hasNextInt()) {
                int value = sc.nextInt();
                sc.nextLine();   // throw away the rest of the line (the Enter key)
                if (value >= min && value <= max) {
                    return value;
                }
            } else {
                sc.nextLine();   // throw away the bad text so we don't loop forever
            }
            System.out.println("Enter a whole number from " + min + " to " + max + ".");
        }
    }

    // same idea for decimal numbers
    static double readDoubleRange(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            if (sc.hasNextDouble()) {
                double value = sc.nextDouble();
                sc.nextLine();
                if (value >= min && value <= max) {
                    return value;
                }
            } else {
                sc.nextLine();
            }
            System.out.println("Enter a number from " + min + " to " + max + ".");
        }
    }

    // reads a whole line of text (names can have spaces)
    static String readText(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    static boolean readYesNo(String prompt) {
        while (true) {
            String answer = readText(prompt);
            if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) {
                return true;
            } else if (answer.equalsIgnoreCase("n") || answer.equalsIgnoreCase("no")) {
                return false;
            }
            System.out.println("Please type y or n.");
        }
    }
}
