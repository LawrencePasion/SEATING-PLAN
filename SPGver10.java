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
 * DRAWING: the ASCII art is NOT to scale. Desks, chairs, aisles, etc. are drawn at
 * fixed sizes. The measurements only decide how many columns/rows fit in the room.
 *
 * ROOM WIDTH  = left-to-right (the whiteboard wall)  -> horizontal, the long side
 * ROOM LENGTH = front-to-back                        -> vertical
 */
public class SPGver10 {
    //(class level global variables)
    static final Scanner sc = new Scanner(System.in);
    static final Random random = new Random();
    // MGA INITIALIZATIONs 
    static final int MAX_STUDENTS = 60;
    static final int MAX_COLUMNS = 8;
    static final int MAX_NAME_LENGTH = 12;
    // (for measurements all of them are in cm)
    static final int CM_PER_CHAR = 10;
    static final int CM_PER_LINE = 20;
    static final int FRONT_CLEAR_CM = 100;   // teacher table -> first row
    static final int BACK_CLEAR_CM = 80;     // behind the last row
    // fixed sizes used ONLY for the ASCII drawing (not to scale)
    static final int DRAW_DESK_W = 10, DRAW_DESK_H = 4;   // 4 tall: border, 2 name lines, border
    static final int DRAW_CHAIR_W = 6, DRAW_CHAIR_H = 1;
    static final int DRAW_AISLE = 3, DRAW_ROW_GAP = 1, DRAW_FRONT_GAP = 1;
    static final int DRAW_TEACHER_W = 13, DRAW_TEACHER_H = 3;
    static int drawW;                        // width (in characters) of the drawn room
    // default dimensions for the room and furniture, not final to be mutated if the user chooses custom sizing.
    static int roomWidthCm, roomLengthCm;
    static int deskWidthCm, deskDepthCm, chairCm;
    static int aisleCm, rowGapCm;
    static int teacherWidthCm, teacherDepthCm;

    static String[] seats = new String[0];   // initializes placeholder to create arrays, seat i = row (i / cols), column (i % cols); null = empty
    static int rows, cols;
    static boolean classReady = false;
    static boolean armchair = false;        // false = separate desk + chair, true = armchair (one piece, same size as a chair); one type per plan

    // ============================================================
    // MAIN MENU
    // ============================================================

    public static void main(String[] args) {
        int choice;
        do { //do while
            System.out.println("\n==========================================");
            System.out.println("  CLASSROOM & SEATING PLANNER");
            System.out.println("==========================================");
            System.out.println(" 1. Set up a new class");
            System.out.println(" 2. Classroom & furniture details");
            System.out.println(" 0. Exit");
            choice = readInt("Choose an option: ", 0, 2);

            if (choice == 1) { //nested if in do while
                setupClass();   
            } else if (choice == 2) {   
                if (classReady) {   //nested if
                    printDetails();
                } else {
                    printInfoSheet();   // no class yet: show general info instead
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
        // desk type comes first, one type for the whole plan (no mixing)
        System.out.println(" 1. Separate desk and chair");
        System.out.println(" 2. Armchair (one piece, same size as a chair)");
        armchair = readInt("Choose the seat type: ", 1, 2) == 2;

        //     V -Dynamic Prompt Concatenation (IF USER INPUTS Y IT FLIPS INTO F THEN NO NEED TO INPUT CUSTOM SIZES)
        String furniture = armchair ? "armchairs " + chairCm + " x " + chairCm + " cm"
                                    : "desks " + deskWidthCm + " x " + deskDepthCm + " cm";
        if (!readYesNo("Use default sizes? (room " + roomText() + ", " + furniture + ") (y/n): ")) {
            if (armchair) {
                chairCm = readInt("Armchair size (square) in cm (30-80): ", 30, 80);
            } else {
                deskWidthCm = readInt("Student desk WIDTH (side to side) in cm (40-200): ", 40, 200);
                deskDepthCm = readInt("Student desk DEPTH (front to back) in cm (30-120): ", 30, 120);
                chairCm = readInt("Chair size (square) in cm (30-80): ", 30, 80);
            }
            aisleCm = readInt("Spacing between columns in cm (30-200): ", 30, 200);
            rowGapCm = readInt("Spacing between rows in cm (20-200): ", 20, 200);
            teacherWidthCm = readInt("Teacher's table WIDTH in cm (80-250): ", 80, 250);
            teacherDepthCm = readInt("Teacher's table DEPTH in cm (40-120): ", 40, 120);

            while (true) {  //nested while loop in the if loop, here if user inputs n to default size, it makes sure sizes are valid
                roomWidthCm = (int) (readDouble("Room WIDTH in meters (left to right, along the whiteboard) (3-20): ", 3, 20) * 100 + 0.5);
                roomLengthCm = (int) (readDouble("Room LENGTH in meters (front to back) (3-20): ", 3, 20) * 100 + 0.5);
                if (maxRows() >= 1) {
                    break;
                }
                System.out.println("The room LENGTH is too short for this furniture (not even 1 row fits). Try again.");
            }
        }

        int maxCols = maxCols(); //calls helper method
        int maxRows = maxRows();
        int capacity = maxCols * maxRows; //calculates for the capacity of the class
        int maxStudents = Math.min(MAX_STUDENTS, capacity); //pretty proud of this witty part, it compares which is less for it to aqctually fit

        System.out.printf("%nThis room fits at most %d columns x %d rows = %d seats.%n", maxCols, maxRows, capacity); // show physical limits of room
        if (maxCols < MAX_COLUMNS) {    //
            System.out.println("Only " + maxCols + " of the usual " + MAX_COLUMNS + " columns fit across the room WIDTH."); 
        }   //notifies user if columns are below the usual

        int students = readInt("Number of students (1-" + maxStudents + "): ", 1, maxStudents);

        // ask for every student's name (seats is temporarily sized to the student count so checkName can catch duplicates)
        seats = new String[students];
        for (int i = 0; i < students; i++) {
            String name = readText("Name of student " + (i + 1) + " of " + students + ": ");
            String problem = checkName(name, i);
            if (!problem.isEmpty()) {
                System.out.println(problem);
                i--;    // ask for this student again
            } else {
                seats[i] = name;
            }
        }

        // how many seats (desks + chairs) to set up: at least one per student, at most what the room fits
        int totalSeats;
        if (students == capacity) {
            totalSeats = students;
            System.out.println("Every seat in the room is needed, so the class will have " + totalSeats + " seats.");
        } else {
            totalSeats = readInt("How many seats? (" + students + "-" + capacity + "): ", students, capacity);
        }

        int minCols = (totalSeats + maxRows - 1) / maxRows;      // ceil(totalSeats / maxRows): fewest columns that keep rows within the room
        int maxColsAllowed = Math.min(maxCols, totalSeats);      // no need for more columns than seats
        cols = readInt("How many columns? (" + minCols + "-" + maxColsAllowed + "): ", minCols, maxColsAllowed);

        // rows are worked out automatically; the last row may be only partly filled (seats fill left to right)
        rows = (totalSeats + cols - 1) / cols;
        int lastRowSeats = totalSeats - (rows - 1) * cols;
        if (lastRowSeats < cols) {
            System.out.println("The last row (R" + rows + ") will only have " + lastRowSeats + " seat(s); the remaining spots stay empty (no desk or chair).");
        }

        // grow the seat list to the real seat count, keeping the names already entered
        String[] named = seats;
        seats = new String[totalSeats];
        for (int i = 0; i < students; i++) {
            seats[i] = named[i];
        }
        shuffleSeats();
        classReady = true;

        System.out.println("\nClass created!");
        showPlan("SEATING PLAN");
        editMenu();
    }

    static void resetDefaults() {   //whenevr a new calss is made these are wiped clean off
        roomWidthCm = 1100;     // 11 m wide  (left-right)
        roomLengthCm = 900;     // 9 m long   (front-back)  -> landscape
        deskWidthCm = 60;
        deskDepthCm = 45;
        chairCm = 40;
        aisleCm = 60;
        rowGapCm = 50;
        teacherWidthCm = 150;
        teacherDepthCm = 70;
    }
    //METHODS USED EARLIER
    // Capacity is worked out from the real measurements (the drawing itself is not to scale).
    static int maxCols() {
        int n = 0;
        while (n < MAX_COLUMNS && gridChars(n + 1) <= toChars(roomWidthCm)) {   //starts at 0 increasing to find max 0 based indexing, and if the columns would fit, 2nd part isnt 0 based indexing thats why its <=
            n++;
        }
        return n;
    }

    static int maxRows() {  //calculates for the max rows based off the max length(height)
        int n = 0;
        while (linesNeeded(n + 1) <= toLines(roomLengthCm)) {   
            n++;
        }
        return n;
    }

    // drawn width of n columns
    static int gridChars(int n) {   //how many horizontal text will take up screen
        return n * deskChars() + (n - 1) * toChars(aisleCm);
    }

    // drawn height of: whiteboard + teacher + front gap + n rows + back gap
    static int linesNeeded(int n) { //hm vertical lines
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
            System.out.println(" 0. Back to main menu");
            choice = readInt("Choose an option: ", 0, 1);

            switch (choice) {
                case 1:
                    shuffleSeats();
                    showPlan("SEATING PLAN (re-shuffled)");
                    break;
            }
        } while (choice != 0);
    }

    // shuffles every seat, empty ones included
    static void shuffleSeats() {
        Collections.shuffle(Arrays.asList(seats), random);
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

    // ============================================================
    // DRAWING
    // ============================================================

    static void showPlan(String title) {
        System.out.println("\n########## " + title + " ##########");
        drawRoom();
        printLegend();
    }

    static int toChars(int cm) {
        return (cm + CM_PER_CHAR - 1) / CM_PER_CHAR;     // rounded up
    }

    static int toLines(int cm) {
        return (cm + CM_PER_LINE - 1) / CM_PER_LINE;     // rounded up
    }

    // used only for the capacity check (how many columns/rows fit the room)
    // an armchair is a single square piece the size of a chair, so it replaces both the desk and the chair
    static int deskChars()    { return Math.max(6, toChars(armchair ? chairCm : deskWidthCm)); }
    static int deskLines()    { return armchair ? Math.max(DRAW_DESK_H, toLines(chairCm)) : Math.max(3, toLines(deskDepthCm)); }
    static int chairLines()   { return armchair ? 0 : toLines(chairCm); }
    static int teacherLines() { return Math.max(3, toLines(teacherDepthCm)); }

    // centers text in a field of the given width (cuts it if too long)
    static String center(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int left = (width - text.length()) / 2;
        return " ".repeat(left) + text + " ".repeat(width - text.length() - left);
    }

    // text for a desk's name lines (line 1 = first, line 2 = second); a name too long for one line
    // is split in two with hyphens, e.g. "Christopher" -> "Chris-" / "-topher"
    static String nameLine(String name, int line, int width) {
        if (name == null || line > 2) {
            return "";
        }
        if (name.length() <= width) {
            return line == 1 ? name : "";
        }
        int cut = name.length() / 2;
        return line == 1 ? name.substring(0, cut) + "-" : "-" + name.substring(cut);
    }

    // one empty room line
    static char[] blank() {
        char[] b = new char[drawW];
        Arrays.fill(b, ' ');
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
        int left = (drawW - text.length()) / 2;
        String f = String.valueOf(fill);
        return "+" + f.repeat(left) + text + f.repeat(drawW - left - text.length()) + "+";
    }

    static void drawRoom() {
        int dc = DRAW_DESK_W, dl = DRAW_DESK_H, cc = DRAW_CHAIR_W, cl = armchair ? 0 : DRAW_CHAIR_H;
        int aisle = DRAW_AISLE;

        // width of the drawn grid, and of the drawn room (wide enough for the grid and the wall labels)
        int grid = cols * dc + (cols - 1) * aisle;
        drawW = Math.max(grid + 4, 30);

        // left edge of every column
        int[] x = new int[cols];
        x[0] = (drawW - grid) / 2;
        for (int c = 1; c < cols; c++) {
            x[c] = x[c - 1] + dc + aisle;
        }

        // column headers
        char[] h = blank();
        for (int c = 0; c < cols; c++) {
            String t = "C" + (c + 1);
            put(h, x[c] + (dc - t.length()) / 2, t);
        }
        System.out.println("\n      " + new String(h));

        // front wall + whiteboard
        System.out.println("     " + wall('=', " FRONT - WIDTH " + roomWidthCm / 100.0 + " m "));
        char[] b = blank();
        put(b, 0, center("WHITEBOARD", drawW));
        emit("", b);

        // teacher's table
        int tc = DRAW_TEACHER_W, tl = DRAW_TEACHER_H;
        for (int i = 0; i < tl; i++) {
            b = blank();
            put(b, (drawW - tc) / 2, (i == 0 || i == tl - 1)
                    ? "+" + "-".repeat(tc - 2) + "+"
                    : "|" + center(i == 1 ? "TEACHER" : "", tc - 2) + "|");
            emit("", b);
        }

        // space between teacher and first row
        for (int i = 0; i < DRAW_FRONT_GAP; i++) {
            emit("", blank());
        }

        // student rows: desks (+---+), then chairs (#); an empty seat looks the same but says EMPTY
        for (int r = 0; r < rows; r++) {
            for (int line = 0; line < dl; line++) {
                b = blank();
                for (int c = 0; c < cols; c++) {
                    if (r * cols + c >= seats.length) {
                        break;      // no desk here (partly filled last row)
                    }
                    String name = seats[r * cols + c];
                    if (name == null) {
                        name = "EMPTY";
                    }
                    // the armchair is drawn like a desk (+---+), it just has no chair row under it
                    put(b, x[c], (line == 0 || line == dl - 1)
                            ? "+" + "-".repeat(dc - 2) + "+"
                            : "|" + center(nameLine(name, line, dc - 2), dc - 2) + "|");
                }
                emit(line == 1 ? "R" + (r + 1) : "", b);
            }
            for (int line = 0; line < cl; line++) {
                b = blank();
                for (int c = 0; c < cols; c++) {
                    if (r * cols + c >= seats.length) {
                        break;      // no chair here either
                    }
                    put(b, x[c] + (dc - cc) / 2, "#".repeat(cc));
                }
                emit("", b);
            }
            if (r < rows - 1) {
                for (int i = 0; i < DRAW_ROW_GAP; i++) {
                    emit("", blank());
                }
            }
        }

        // a little floor behind the last row
        emit("", blank());

        System.out.println("     " + wall('-', " BACK ") + "  LENGTH (front to back) = " + roomLengthCm / 100.0 + " m");
    }

    static void printLegend() {
        int seatsTotal = seats.length;
        System.out.println("\n--- LEGEND (drawing is NOT to scale) ---");
        if (armchair) {
            System.out.println(" +--------+   Armchair (seat and desk in one, no separate chair): " + chairCm + " x " + chairCm + " cm (EMPTY = no student)");
        } else {
            System.out.println(" +--------+   Student desk: width " + deskWidthCm + " x depth " + deskDepthCm + " cm (EMPTY = no student)");
            System.out.println(" ######       Chair: " + chairCm + " x " + chairCm + " cm");
        }
        System.out.println(" TEACHER      Teacher's table: width " + teacherWidthCm + " x depth " + teacherDepthCm + " cm");
        System.out.println(" gap          Between columns: " + aisleCm + " cm | between rows: " + rowGapCm + " cm");
        System.out.println(" room         WIDTH " + roomWidthCm / 100.0 + " m | LENGTH " + roomLengthCm / 100.0 + " m");
        System.out.println(" R1 / C1      Row and column numbers (use them to find a seat)");
        System.out.println("Students: " + countStudents() + " | Seats: " + seatsTotal
                + " | Empty: " + (seatsTotal - countStudents()));
    }

    // shown from the main menu when no class exists yet: limits, default sizes and what fits by default
    static void printInfoSheet() {
        resetDefaults();    // load the default values (setupClass resets them again anyway)
        armchair = false;   // capacity below is worked out for the default separate desk + chair

        System.out.println("\n--- CLASSROOM & FURNITURE INFO SHEET ---");
        System.out.println("(No class has been set up yet. Use option 1 to create one.)");

        System.out.println("\nGENERAL LIMITS");
        System.out.println(" Max students per class : " + MAX_STUDENTS);
        System.out.println(" Max columns            : " + MAX_COLUMNS);
        System.out.println(" Max name length        : " + MAX_NAME_LENGTH + " characters");

        System.out.println("\nSEAT TYPES (one type for the whole plan)");
        System.out.println(" 1. Separate desk and chair");
        System.out.println(" 2. Armchair (one piece, same size as a chair)");

        System.out.println("\nALLOWED SIZES (custom sizing)");
        System.out.println(" Item                       Min     Max     Default");
        System.out.println(" Student desk width (cm)    40      200     " + deskWidthCm);
        System.out.println(" Student desk depth (cm)    30      120     " + deskDepthCm);
        System.out.println(" Chair / armchair (cm)      30      80      " + chairCm + " (square)");
        System.out.println(" Space between columns (cm) 30      200     " + aisleCm);
        System.out.println(" Space between rows (cm)    20      200     " + rowGapCm);
        System.out.println(" Teacher's table width (cm) 80      250     " + teacherWidthCm);
        System.out.println(" Teacher's table depth (cm) 40      120     " + teacherDepthCm);
        System.out.println(" Room width (m)             3       20      " + roomWidthCm / 100.0 + " (left to right, along the whiteboard)");
        System.out.println(" Room length (m)            3       20      " + roomLengthCm / 100.0 + " (front to back)");

        System.out.println("\nDEFAULT ROOM (if you choose default sizes)");
        System.out.println(" Room     : " + roomText());
        System.out.println(" Fits     : up to " + maxCols() + " columns x " + maxRows() + " rows = "
                + (maxCols() * maxRows()) + " seats (separate desk and chair)");
        System.out.println(" Clearance: " + FRONT_CLEAR_CM + " cm between teacher's table and first row, "
                + BACK_CLEAR_CM + " cm behind the last row");

        System.out.println("\nNOTES");
        System.out.println(" - The room size decides how many columns and rows fit.");
        System.out.println(" - The ASCII drawing is NOT to scale.");
        System.out.println(" - The last row may be partly filled; empty spots have no desk or chair.");
    }

    static void printDetails() {
        System.out.println("\n--- CLASSROOM DETAILS ---");
        System.out.println("Seat type     : " + (armchair ? "Armchair (one piece)" : "Separate desk and chair"));
        System.out.println("Room size     : " + roomText());
        System.out.println("Grid          : " + rows + " rows x " + cols + " columns (" + seats.length + " seats)");
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