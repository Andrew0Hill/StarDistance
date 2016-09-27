/**
 * Created by Andrew_2 on 9/26/2016.
 */

import static org.jocl.CL.*;

import org.jocl.*;
import org.apache.commons.csv.*;

import java.io.FileReader;
import java.math.*;
import java.io.StreamCorruptedException;

public class StarDistance {
    static final int NUM_OF_STARS = 119617;
    static final int NUM_OF_VALID_STARS = 115372;
    static final String DISTANCE_UNKNOWN = "10000000";
    static double x_coords[];
    static double y_coords[];
    static double z_coords[];
    public static void main(String args[]) {
        x_coords = new double[327];
        y_coords = new double[327];
        z_coords = new double[327];

        int index = 0;

        try {
            FileReader input = new FileReader(args[0]);
            Iterable<CSVRecord> stars = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(input);

            for (CSVRecord star : stars) {
                String distance = star.get(9);
                if(!distance.equals(DISTANCE_UNKNOWN) && Float.parseFloat(distance) <= 10) {
                    x_coords[index] = Float.parseFloat(star.get("X"));
                    y_coords[index] = Float.parseFloat(star.get("Y"));
                    z_coords[index] = Float.parseFloat(star.get("Z"));
                    ++index;
                }
            }
            System.out.print("Done!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        StarGraphicsCompute s = new StarGraphicsCompute(x_coords,y_coords,z_coords,327);
        s.go();
        //computeDistances(index);
    }
    public static void computeDistances(int index) {
        double output_distance[];
        int starsAnalyzed = 0;
        double min;
        double max;
        double mean;
        double sum = 0;
        output_distance = new double[index];

        for(int i = 0; i < index; ++i){
            output_distance[i] = distanceBetween(i,0);
            for(int j = 0; j < index; ++j){
                if(i!=j) {
                    double output = distanceBetween(i, j);
                    if (output < output_distance[i]) {
                        output_distance[i] = output;
                    }
                }
            }
            if((starsAnalyzed % 1000) == 0){
                System.out.println(starsAnalyzed + " stars analyzed.\n");
            }
            ++starsAnalyzed;
        }
        min = output_distance[0];
        max = output_distance[0];
        for(int i = 0; i < index; ++i) {
            if(output_distance[i] > max){
                max = output_distance[i];
            }
            else if(output_distance[i] < min){
                min = output_distance[i];
            }
            sum += output_distance[i];
        }
        mean = sum/index;
        System.out.println("Minimum: " + min);
        System.out.println("Maximum: " + max);
        System.out.println("Mean: " + mean);
        System.out.println("Done!");
    }
    public static double distanceBetween(int i, int j) {
        return Math.sqrt(Math.pow((x_coords[i] - x_coords[j]),2) + Math.pow((y_coords[i] - y_coords[j]),2) + Math.pow((z_coords[i] - z_coords[j]),2));
    }
}
