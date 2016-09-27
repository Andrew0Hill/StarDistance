/**
 * Created by Andrew_2 on 9/26/2016.
 */
import static org.jocl.CL.*;
import org.jocl.*;
public class StarGraphicsCompute {
    /**
     * The source code of the OpenCL program to execute
     */
 /*   private static String programSource =
            "__kernel void "+
                    "calculateStars(__global const double *x,"+
                    "             __global const double *y,"+
                    "             __global const double *z,"+
                    "             __global double *out)"+
                    "{"+
                    "    int gid = get_global_id(0);"+
                    "    out[gid] = x[gid] + y[gid] + z[gid];"+
                    "}";*/
    private static String programSource =
            "__kernel void " +
                    "calculateStars(__global const double *x," +
                    "               __global const double *y," +
                    "               __global const double *z," +
                    "               __global double *out," +
                    "               __global const int *size)" +
                    "{" +
                    "   int width = size[0];"+
                    "   int i = get_global_id(0);" +
                    "   int j = get_global_id(1);" +
                    "   int index = (i*width)+j;"+
                    "   out[index] = sqrt((x[i]-x[j])*(x[i]-x[j]) + (y[i]-y[j])*(y[i]-y[j]) + (z[i]-z[j])*(z[i]-z[j]));}";



    /**
     * The entry point of this sample
     *
     * @param args Not used
     */
    static double x_coords[];
    static double y_coords[];
    static double z_coords[];
    static double output[];
    static double smallestDistance[];
    static int array_width[];
    static int numValid;
    StarGraphicsCompute(double x[], double y[], double z[], int size){
        x_coords = x;
        y_coords = y;
        z_coords = z;
        numValid = size;
        array_width = new int[1];
        array_width[0] = size;
        output = new double[size*size];
        for(int i = 0; i < size; ++i){
            output[i] = 0;
        }
        smallestDistance = new double[size];
    }
    public static void go()
    {
        // Create input- and output data


        Pointer x_ptr = Pointer.to(x_coords);
        Pointer y_ptr = Pointer.to(y_coords);
        Pointer z_ptr = Pointer.to(z_coords);
        Pointer out_ptr = Pointer.to(output);
        Pointer size_ptr = Pointer.to(array_width);


        // The platform, device type and device number
        // that will be used
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Create a context for the selected device
        cl_context context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        // Create a command-queue for the selected device
        cl_command_queue commandQueue =
                clCreateCommandQueue(context, device, 0, null);

        // Allocate the memory objects for the input- and output data
        cl_mem memObjects[] = new cl_mem[5];
        memObjects[0] = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_double * numValid, x_ptr, null);
        memObjects[1] = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_double * numValid, y_ptr, null);
        memObjects[2] = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_double * numValid, z_ptr, null);
        memObjects[3] = clCreateBuffer(context,
                CL_MEM_READ_WRITE,
                Sizeof.cl_double * numValid * numValid, null, null);
        memObjects[4] = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_int, size_ptr, null);

        // Create the program from the source code
        cl_program program = clCreateProgramWithSource(context,
                1, new String[]{ programSource }, null, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);

        // Create the kernel
        cl_kernel kernel = clCreateKernel(program, "calculateStars", null);

        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memObjects[2]));
        clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(memObjects[3]));
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(memObjects[4]));

        // Set the work-item dimensions
        long global_work_size[] = new long[]{numValid,numValid};
        long local_work_size[] = new long[]{16,16};

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                global_work_size, local_work_size, 0, null, null);

        // Read the output data
        clEnqueueReadBuffer(commandQueue, memObjects[3], CL_TRUE, 0,
                numValid *numValid * Sizeof.cl_double, out_ptr, 0, null, null);

        // Release kernel, program, and memory objects
        clReleaseMemObject(memObjects[0]);
        clReleaseMemObject(memObjects[1]);
        clReleaseMemObject(memObjects[2]);
        clReleaseMemObject(memObjects[3]);
        clReleaseMemObject(memObjects[4]);
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);

       for(int i = 0; i < numValid; ++i){
           double min = output[(i*numValid)+0];
           smallestDistance[i] = min;
           for(int j = 0; j < numValid; ++j){
               if(i!=j) {
                   if (output[(i * numValid) + j] < smallestDistance[i]) {
                       smallestDistance[i] = output[(i * numValid) + j];
                   }
               }
           }
       }
        System.out.println("Test Complete");
    }
}
