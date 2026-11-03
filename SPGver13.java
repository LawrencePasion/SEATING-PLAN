import java.util.Arrays;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.Random;
import java.util.Scanner;

// Classroom & Seating Plan Generator (console app)
// ROOM WIDTH  = left-to-right (the whiteboard wall)  -> horizontal, the long side
// ROOM LENGTH = front-to-back                        -> vertical
public class SPGver13 {
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
    static final int DESK_CHAIR_GAP_CM = 20; // space between a desk and its chair (separate desk + chair only)
    // fixed sizes used ONLY for the ASCII drawing (not to scale)
    static final int DRAW_DESK_W = 10;
    static final int DRAW_DESK_H = 4;   // 4 tall: border, 2 name lines, border
    static final int DRAW_CHAIR_W = 6;
    static final int DRAW_CHAIR_H = 1;
    static final int DRAW_AISLE = 3;
    static final int DRAW_ROW_GAP = 1;
    static final int DRAW_FRONT_GAP = 1;
    static final int DRAW_DESK_CHAIR_GAP = 1;             // blank line between a desk and its chair
    static final int DRAW_TEACHER_W = 13;
    static final int DRAW_TEACHER_H = 3;
    static int drawW;                        // width (in characters) of the drawn room
    // default dimensions for the room and furniture, not final to be mutated if the user chooses custom sizing.
    static int roomWidthCm;
    static int roomLengthCm;
    static int deskWidthCm;
    static int deskDepthCm;
    static int chairCm;
    static int aisleCm;
    static int rowGapCm;
    static int teacherWidthCm;
    static int teacherDepthCm;

    static String[] seats = new String[0];   // initializes placeholder to create arrays, seat i = row (i / cols), column (i % cols); null = empty
    static int rows;
    static int cols;
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
            } else if (choice == 2) {   //else if to accomodate option 0
                printInfoSheet();   // ashow general info
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
        int seatType = readInt("Choose the seat type: ", 1, 2);
        if (seatType == 2) {
            armchair = true;
        } else {
            armchair = false;
        }

        String furniture;
        if (armchair) {
            furniture = "armchairs " + chairCm + " x " + chairCm + " cm";   
        } else {
            furniture = "desks " + deskWidthCm + " x " + deskDepthCm + " cm";   
        }
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

            boolean roomFits = false;   //create variable roomFits(switch/flag)
            while (!roomFits) {  //nested while loop in the if: if the user inputs n to default size, keep asking until the sizes are valid
                roomWidthCm = (int) (readDouble("Room WIDTH in meters (left to right, along the whiteboard) (3-20): ", 3, 20) * 100 + 0.5); //equation of m to cm
                roomLengthCm = (int) (readDouble("Room LENGTH in meters (front to back) (3-20): ", 3, 20) * 100 + 0.5); //(int) truncates decimal vals for final result
                if (maxRows() >= 1) {
                    roomFits = true;
                } else {
                    System.out.println("The room LENGTH is too short for this furniture (not even 1 row fits). Try again.");
                }
            }
        }

        int maxCols = maxCols(); //calls helper method
        int maxRows = maxRows();
        int capacity = maxCols * maxRows; //calculates for the capacity of the class
        int maxStudents = Math.min(MAX_STUDENTS, capacity); //compares which is less for it to actually fit

        System.out.printf("%nThis room fits at most %d columns x %d rows = %d seats.%n", maxCols, maxRows, capacity); // show physical limits of room
                            //%n for new line, %d placeholder for ints
        if (maxCols < MAX_COLUMNS) {    
            System.out.println("Only " + maxCols + " of the usual " + MAX_COLUMNS + " columns fit across the room WIDTH."); 
        }   //notifies user if columns are below the usual

        int students = readInt("Number of students (1-" + maxStudents + "): ", 1, maxStudents); //asks user for # of students

        // ask for every student's name (seats is temporarily sized to the student count so checkName can catch duplicates)
        seats = new String[students];   //creates an array named seats
        int entered = 0;    // how many names have been accepted so far
        while (entered < students) {
            String name = readText("Name of student " + (entered + 1) + " of " + students + ": ");
            String problem = checkName(name, entered);
            if (!problem.isEmpty()) {   //isEmpty is built in
                System.out.println(problem);    // entered doesn't go up, so this student is asked again, and it shows what went wrong
            } else {
                seats[entered] = name;
                entered++;
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
        int maxColsAllowed = Math.min(maxCols, totalSeats);      // no need for more columns than there are to accomodate seats
        cols = readInt("How many columns? (" + minCols + "-" + maxColsAllowed + "): ", minCols, maxColsAllowed);

        // rows are worked out automatically; the last row may be only partly filled (seats fill left to right)
        rows = (totalSeats + cols - 1) / cols;  //formula used for rounding up, final row added for remaining students after filling up the latest row
        int lastRowSeats = totalSeats - (rows - 1) * cols;  //find out how many seats in the last row
        if (lastRowSeats < cols) {  //checks if last row is partially filled
            System.out.println("The last row (R" + rows + ") will only have " + lastRowSeats + " seat(s); the remaining spots stay empty (no desk or chair).");
        }
        //match the total physical seats in the room (which could be more than the number of students, meaning there will be empty desks or chairs)
        // grow the seat list to the real seat count, keeping the names already entered. like if there are more seats user inputted than the # of students
        String[] biggerSeats = new String[totalSeats];   // the new slots start as null (empty). 
        for (int i = 0; i < seats.length; i++) {
            biggerSeats[i] = seats[i];
        }
        seats = biggerSeats;
        shuffleSeats();

        System.out.println("\nClass created!");
        showPlan("SEATING PLAN");
        editMenu();
    }
//=======================================END OF PART 1======================================================================================
//=======================================END OF PART 1======================================================================================
//=======================================END OF PART 1======================================================================================
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
    }
    //METHODS USED EARLIER
    // Capacity is worked out from the real measurements (the drawing itself is not to scale).
    static int maxCols() {  //calculates for max rows based of width(horizontal)
        int n = 0;
        while (n < MAX_COLUMNS && gridChars(n + 1) <= toChars(roomWidthCm)) {   //starts at 0 increasing to find max 0 based indexing, and if the columns would fit, 2nd part isnt 0 based indexing thats why its <=
            n++;
        }
        return n;
    }

    static int maxRows() {  //calculates for the max rows based off the length(height/vertical)
        int n = 0;
        while (linesNeeded(n + 1) <= toLines(roomLengthCm)) { //checks if lines exceed the Length, if it does or is equal to it then it stops 
            n++;
        }
        return n;
    }

    // Calculates the total horizontal character width needed for n columns (desks + aisles)
    static int gridChars(int n) {   //how many horizontal text will take up screen
        return n * deskChars() + (n - 1) * toChars(aisleCm);
    }

    // drawn height of: whiteboard + teacher + front gap + n rows (desk + desk/chair gap + chair) + row gaps + back gap
    static int linesNeeded(int n) { //hm vertical lines(front 2 back)
        return 1 + teacherLines() + toLines(FRONT_CLEAR_CM) + n * (deskLines() + gapLines() + chairLines()) //1 is the top line of wall btw
                + (n - 1) * toLines(rowGapCm) + toLines(BACK_CLEAR_CM);
    }

    static String roomText() {  //converts measurements of cm into meters
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
    static String checkName(String name, int skip) {    //skip parameter tells it which index position in the array to ignore during duplicate checks
        if (name.isEmpty()) {
            return "Name can't be blank.";
        } else if (name.length() > MAX_NAME_LENGTH) {
            return "Name is too long (max " + MAX_NAME_LENGTH + " characters).";
        }
        for (int i = 0; i < seats.length; i++) {
            if (i != skip) {                        // don't compare a seat with itself
                if (seats[i] != null) {             // check for null FIRST: an empty seat has no name to compare
                    if (seats[i].equalsIgnoreCase(name)) {  //checks 4 names that r basically the same but with diff capitalizations
                        return "That name is already used, add a surname or initial.";
                    }
                }
            }
        }
        return ""; // leaves empty string if name is completely valid
    }

    static int countStudents() {
        int count = 0;
        for (int i = 0; i < seats.length; i++) {
            if (seats[i] != null) {
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
        return (cm + CM_PER_CHAR - 1) / CM_PER_CHAR;     //Converts cm into how many character spaces it takes horizontally (rounded up)
    }

    static int toLines(int cm) {
        return (cm + CM_PER_LINE - 1) / CM_PER_LINE;     //Converts cm into how many text lines it takes vertically (rounded up)
    }

    // used only for the capacity check (how many columns/rows fit the room)
    // an armchair is a single square piece the size of a chair, so it replaces both the desk and the chair
    static int deskChars() {
        if (armchair) {
            return Math.max(6, toChars(chairCm));
        } else {
            return Math.max(6, toChars(deskWidthCm));
        }
    }

    static int deskLines() {
        if (armchair) {
            return Math.max(DRAW_DESK_H, toLines(chairCm));
        } else {
            return Math.max(3, toLines(deskDepthCm));
        }
    }

    static int chairLines() {
        if (armchair) {
            return 0;       // an armchair has no separate chair
        } else {
            return toLines(chairCm);
        }
    }

    static int gapLines() {     // desk-to-chair gap (none for armchairs)
        if (armchair) {
            return 0;
        } else {
            return toLines(DESK_CHAIR_GAP_CM);
        }
    }

    static int teacherLines() { return Math.max(3, toLines(teacherDepthCm)); }
//==============================END OF PART 2========================================================================================
//==============================END OF PART 2========================================================================================
//==============================END OF PART 2========================================================================================
    // centers text in a field of the given width (cuts it if too long)
    static String center(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int left = (width - text.length()) / 2;
        return repeatText(" ", left) + text + repeatText(" ", width - text.length() - left);
    }

    // text for a desk's name lines (line 1 = first, line 2 = second); a name too long for one line
    // is split in two with hyphens, e.g. "Christopher" -> "Chris-" / "-topher"
    static String nameLine(String name, int line, int width) {
        if (name == null || line > 2) {
            return "";
        }
        if (name.length() <= width) {
            if (line == 1) {
                return name;
            } else {
                return "";
            }
        }
        int cut = name.length() / 2;
        if (line == 1) {
            return name.substring(0, cut) + "-";
        } else {
            return "-" + name.substring(cut);
        }
    }

    // repeats a piece of text several times, e.g. repeatText("-", 5) gives "-----"
    static String repeatText(String text, int times) {
        String result = "";
        for (int i = 0; i < times; i++) {
            result += text;
        }
        return result;
    }

    // one empty room line
    static char[] blank() {
        char[] b = new char[drawW];
        for (int i = 0; i < b.length; i++) {
            b[i] = ' ';
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
        int left = (drawW - text.length()) / 2;
        String f = String.valueOf(fill);
        return "+" + repeatText(f, left) + text + repeatText(f, drawW - left - text.length()) + "+";
    }

    static void drawRoom() {
        int dc = DRAW_DESK_W;
        int dl = DRAW_DESK_H;
        int cc = DRAW_CHAIR_W;
        int aisle = DRAW_AISLE;
        int cl;                 // lines used by the chair (none for an armchair)
        int deskChairGap;       // blank lines between a desk and its chair
        if (armchair) {
            cl = 0;
            deskChairGap = 0;
        } else {
            cl = DRAW_CHAIR_H;
            deskChairGap = DRAW_DESK_CHAIR_GAP;
        }

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
        int tc = DRAW_TEACHER_W;
        int tl = DRAW_TEACHER_H;
        for (int i = 0; i < tl; i++) {
            b = blank();
            String tableRow;
            if (i == 0 || i == tl - 1) {
                tableRow = "+" + repeatText("-", tc - 2) + "+";      // top / bottom border
            } else if (i == 1) {
                tableRow = "|" + center("TEACHER", tc - 2) + "|";
            } else {
                tableRow = "|" + center("", tc - 2) + "|";
            }
            put(b, (drawW - tc) / 2, tableRow);
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
                    String deskRow;
                    if (line == 0 || line == dl - 1) {
                        deskRow = "+" + repeatText("-", dc - 2) + "+";      // top / bottom border
                    } else {
                        deskRow = "|" + center(nameLine(name, line, dc - 2), dc - 2) + "|";
                    }
                    put(b, x[c], deskRow);
                }
                if (line == 1) {
                    emit("R" + (r + 1), b);     // row label goes on the first name line
                } else {
                    emit("", b);
                }
            }
            // gap between the desk and its chair (separate desk + chair only)
            for (int i = 0; i < deskChairGap; i++) {
                emit("", blank());
            }
            for (int line = 0; line < cl; line++) {
                b = blank();
                for (int c = 0; c < cols; c++) {
                    if (r * cols + c >= seats.length) {
                        break;      // no chair here either
                    }
                    put(b, x[c] + (dc - cc) / 2, repeatText("#", cc));
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
//=======================================END OF PART 3======================================================================================
//=======================================END OF PART 3======================================================================================
//=======================================END OF PART 3======================================================================================
    static void printLegend() {
        int seatsTotal = seats.length;
        System.out.println("\n--- LEGEND (drawing is NOT to scale) ---");
        if (armchair) {
            System.out.println(" +--------+   Armchair (seat and desk in one, no separate chair): " + chairCm + " x " + chairCm + " cm (EMPTY = no student)");
        } else {
            System.out.println(" +--------+   Student desk: width " + deskWidthCm + " x depth " + deskDepthCm + " cm (EMPTY = no student)");
            System.out.println(" ######       Chair: " + chairCm + " x " + chairCm + " cm (" + DESK_CHAIR_GAP_CM + " cm gap between desk and chair)");
        }
        System.out.println(" TEACHER      Teacher's table: width " + teacherWidthCm + " x depth " + teacherDepthCm + " cm");
        System.out.println(" gap          Between columns: " + aisleCm + " cm | between rows: " + rowGapCm + " cm");
        System.out.println(" room         WIDTH " + roomWidthCm / 100.0 + " m | LENGTH " + roomLengthCm / 100.0 + " m");
        System.out.println(" R1 / C1      Row and column numbers (use them to find a seat)");
        System.out.println("Students: " + countStudents() + " | Seats: " + seatsTotal
                + " | Empty: " + (seatsTotal - countStudents()));
    }

    // shown from the main menu (option 2): limits, default sizes and what fits by default
    static void printInfoSheet() {
        resetDefaults();    // load the default values (setupClass resets them again anyway)
        armchair = false;   // capacity below is worked out for the default separate desk + chair

        System.out.println("\n--- CLASSROOM & FURNITURE INFO SHEET ---");

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
        System.out.println(" Desk gap : " + DESK_CHAIR_GAP_CM + " cm between each desk and its chair (separate desk and chair only)");

        System.out.println("\nNOTES");
        System.out.println(" - The room size decides how many columns and rows fit.");
        System.out.println(" - The ASCII drawing is NOT to scale.");
        System.out.println(" - The last row may be partly filled; empty spots have no desk or chair.");
    }

    // ============================================================
    // INPUT HELPERS
    // ============================================================

    static int readInt(String prompt, int min, int max) {   //robust input valdiator
        int value = 0;
        boolean valid = false;
        while (!valid) {
            System.out.print(prompt);
            try {
                value = sc.nextInt();
                sc.nextLine();      // nextInt() leaves the Enter key behind, so clear it
                if (value >= min && value <= max) {
                    valid = true;
                }
            } catch (InputMismatchException e) {    // nextInt() throws this when the input isn't a whole number
                sc.nextLine();      // throw away the bad input so the loop doesn't repeat forever
            }
            if (!valid) {
                System.out.println("Enter a whole number from " + min + " to " + max + ".");
            }
        }
        return value;
    }

    static double readDouble(String prompt, double min, double max) {   //same as readInt but this time with decimal vals
        double value = 0;
        boolean valid = false;
        while (!valid) {
            System.out.print(prompt);
            try {
                value = sc.nextDouble();
                sc.nextLine();      // clear the Enter key left behind
                if (value >= min && value <= max) {
                    valid = true;
                }
            } catch (InputMismatchException e) {    // nextDouble() throws this when the input isn't a number
                sc.nextLine();      // throw away the bad input
            }
            if (!valid) {
                System.out.println("Enter a number from " + min + " to " + max + ".");
            }
        }
        return value;
    }

    static String readText(String prompt) {
        System.out.print(prompt);   //print instead of println so cursor remains on the question
        return sc.nextLine().trim();    //.trim cuts any spaces, return sends those 2 at the same line with their respective sequence
    }

    static boolean readYesNo(String prompt) {   //designed to ask the user a y/n question 
        boolean answered = false;   //loops as long as its false
        boolean result = false;     //stores outcome: true for yes, false for no. default is false
        while (!answered) { //keeps loop running until valid answer, default false turns into true due to !
            String a = readText(prompt);    //variable a is used to call readText method
            if (a.equalsIgnoreCase("y") || a.equalsIgnoreCase("yes")) { //.equalsIgnorCase accepts uppercase or lowercase
                result = true;
                answered = true;
            } else if (a.equalsIgnoreCase("n") || a.equalsIgnoreCase("no")) {   
                result = false;
                answered = true;
            } else {
                System.out.println("Please type y or n.");
            }
        }
        return result;
    }
}
