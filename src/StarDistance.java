/**
 * Created by Andrew_2 on 9/26/2016.
 */

import static org.jocl.CL.*;

import org.jocl.*;
import org.apache.commons.csv.*;

import java.io.FileReader;
import java.io.InterruptedIOException;
import java.math.*;
import java.io.StreamCorruptedException;

public class StarDistance {
    static final int NUM_OF_STARS = 119617;
    static final int NUM_OF_VALID_STARS = 115372;
    static final int NUM_VALID_100 = 24638;
    static final int NUM_VALID_10 = 327;
    static final String DISTANCE_UNKNOWN = "10000000";
    static double x_coords[];
    static double y_coords[];
    static double z_coords[];
    static double output[];

    public static void main(String args[]) {
        int size = 0;
        int bound;
        // The bound on star distance is passed in as a program argument. If none
        // is specified, we default to the maximum.
        if (args.length >= 2) {
            System.out.println("Alternate distance provided.");
            System.out.println("Calculating distances within " + args[1] + " parsecs of Sol");
            bound = Integer.parseInt(args[1]);
        } else {
            System.out.println("No bound specified. Calculating all star distances.");
            bound = Integer.parseInt(DISTANCE_UNKNOWN);
        }
        x_coords = new double[NUM_OF_VALID_STARS];
        y_coords = new double[NUM_OF_VALID_STARS];
        z_coords = new double[NUM_OF_VALID_STARS];
        try {
            // Apache Commons CSV lets us open the CSV file and iterate over the contents.
            FileReader input = new FileReader(args[0]);
            Iterable<CSVRecord> stars = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(input);
            for (CSVRecord star : stars) {
                String distance = star.get(9);
                if (!distance.equals(DISTANCE_UNKNOWN) && (Double.parseDouble(star.get(9)) <= bound)) {
                    // Put each coordinate into its own array.
                    x_coords[size] = Double.parseDouble(star.get("X"));
                    y_coords[size] = Double.parseDouble(star.get("Y"));
                    z_coords[size] = Double.parseDouble(star.get("Z"));
                    ++size;
                }
            }
            System.out.println("File read complete. " + size + " valid stars found.\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Compute distances on GPU.
        System.out.println("Beginning calculation using GPU.");
        //StarGraphicsCompute s = new StarGraphicsCompute(x_coords, y_coords, z_coords, size);
        //output = s.go();
        //printResults(output, size, bound);
        // Compute distances on CPU.
        System.out.println("Beginning calculation using CPU.");
        output = computeDistances(size);
        printResults(output, size, bound);

    }

    public static double[] computeDistances(int size) {
        long currentTime = System.nanoTime();
        double output_distance[];
        double output;
        // New array to hold the smallest distance for each star.
        output_distance = new double[size];
        for (int i = 0; i < size; ++i) {
            // Set the inital value to the maximum.
            output_distance[i] = 10000000;
            for (int j = 0; j < size; ++j) {
                if (i != j) {
                    // Get the distance between star i and star j.
                    output = distanceBetween(i, j);
                    // Update the min distance for this star if necessary.
                    if (output < output_distance[i]) {
                        output_distance[i] = output;
                    }
                }
            }
            if(i % 1000 == 0){
                System.out.println(i + " stars analyzed.");
            }
        }
        long elapsed = (System.nanoTime() - currentTime) / 1000000000;
        System.out.println("Process complete. Elapsed time: " + elapsed + " seconds.");
        return output_distance;
    }

    public static double distanceBetween(int i, int j) {
        return Math.sqrt(Math.pow((x_coords[i] - x_coords[j]), 2) + Math.pow((y_coords[i] - y_coords[j]), 2) + Math.pow((z_coords[i] - z_coords[j]), 2));
    }

    public static void printResults(double output_distance[], int size, int distance) {
        /*
        Technically, storing the smallest distance for each star is unnecessary since
        we can just keep a running min and max. I did it this way so I can reuse the code
        for the GPU computation, because the GPU code returns an array of values as opposed to just
        the min, max, and mean.
         */
        double min = output_distance[0];
        double max = output_distance[0];
        double mean;
        double sum = 0;
        // Find the min and max.
        for (int i = 0; i < size; ++i) {
            if (output_distance[i] > max) {
                max = output_distance[i];
            } else if (output_distance[i] < min) {
                min = output_distance[i];
            }
            sum += output_distance[i];
        }
        // Print the resutls.
        mean = sum / size;
        System.out.println("Distance bound: " + distance + " parsecs.");
        System.out.println("Minimum: " + min);
        System.out.println("Maximum: " + max);
        System.out.println("Mean: " + mean);
        System.out.println("Done!\n");
    }
}
