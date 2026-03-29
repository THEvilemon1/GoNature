import java.util.Scanner;
import java.util.ArrayList;

/**
 * This class tests a multi-threaded solution that checks an array.
 * It creates two threads that operate on shared data.
 * Finally, it prints the results.
 */
public class TestThreadCheckArray {

    /**
     * Main function:
     * - Reads input from user
     * - Initializes shared data
     * - Runs two threads
     * - Waits for them to finish
     * - Prints results
     */
    public static void main(String[] args) {

        // Scanner for user input (auto-closed using try-with-resources)
        try (Scanner input = new Scanner(System.in)) {

            Thread thread1, thread2;

            // Ask user for array size
            System.out.println("Enter array size");
            int num  = input.nextInt();

            // Create dynamic array (ArrayList)
            ArrayList<Integer> array = new ArrayList<>();

            // Read array values from user
            System.out.println("Enter numbers for array");
            for (int index = 0; index < num; index++) 
                array.add(input.nextInt());

            // Read number to check (b)
            System.out.println("Enter number");
            num = input.nextInt();

            // Create shared data object for threads
            SharedData sd = new SharedData(array, num);

            // Create two threads that work on the same shared data
            thread1 = new Thread(new ThreadCheckArray(sd), "thread1");
            thread2 = new Thread(new ThreadCheckArray(sd), "thread2");

            // Start both threads
            thread1.start();
            thread2.start();

            // Wait for both threads to finish execution
            try {
                thread1.join();
                thread2.join();
            } catch (InterruptedException e) {
                // Print error if thread is interrupted
                e.printStackTrace();
            }

            // If no solution found (flag is false)
            if (!sd.getFlag()) {
                System.out.println("Sorry");
                return;
            }

            // Print result values
            System.out.println("Solution for b : " + sd.getB() + ",n = " + sd.getArray().size());

            // Print indices of array
            System.out.print("I:    ");
            for(int index = 0; index < sd.getArray().size(); index++)
                System.out.print(index + "    ");

            System.out.println();

            // Print array values aligned nicely
            System.out.print("A:    ");
            for (int value : sd.getArray())
            {
                System.out.print(value);

                // Calculate number of digits in value
                int temp = value;
                int counter = 5;

                while (true)
                {
                    temp = temp / 10;
                    counter--;
                    if (temp == 0)
                        break;
                }

                // Add spaces to align columns
                for (int i = 0; i < counter; i++)
                    System.out.print(" ");
            }

            System.out.println();

            // Print boolean result array (converted to 1/0)
            System.out.print("C:    ");
            for (boolean val : sd.getWinArray())
            {
                if (val)
                    System.out.print("1    ");
                else
                    System.out.print("0    ");	
            }
        }
    }
}