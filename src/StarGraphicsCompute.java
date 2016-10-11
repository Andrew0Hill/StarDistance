/**
 * Created by Andrew_2 on 9/26/2016.
 */

import static org.jocl.CL.*;

import org.jocl.*;

public class StarGraphicsCompute {

    private static String programSource =
            "__kernel void " +
                    "calculateStars(__global const double *x," +
                    "               __global const double *y," +
                    "               __global const double *z," +
                    "               __global double *out," +
                    "               __global const int *size)" +
                    "{" +
                    "   int w = size[0];" +
                    "   int i = get_global_id(0);" +
                    "   out[i] = 10000000;" +
                    "   for(int j = 0; j < w; ++j) {" +
                    "   if(i!=j){" +
                    "       double value = sqrt((x[i]-x[j])*(x[i]-x[j]) + (y[i]-y[j])*(y[i]-y[j]) + (z[i]-z[j])*(z[i]-z[j]));" +
                    "       if(value < out[i]){" +
                    "           out[i] = value;" +
                    "                 }" +
                    "        }" +
                    "    }" +
                    "}";


    static double x_coords[];
    static double y_coords[];
    static double z_coords[];
    static double output[];
    static double smallestDistance[];
    static int sizeOfInput[];
    static int numValid;

    StarGraphicsCompute(double x[], double y[], double z[], int size) {
        x_coords = x;
        y_coords = y;
        z_coords = z;
        numValid = size;
        sizeOfInput = new int[1];
        sizeOfInput[0] = size;
        output = new double[size];
        for (int i = 0; i < size; ++i) {
            output[i] = 0;
        }
        smallestDistance = new double[size];
    }

    public static double[] go() {

        Pointer x_ptr = Pointer.to(x_coords);
        Pointer y_ptr = Pointer.to(y_coords);
        Pointer z_ptr = Pointer.to(z_coords);
        Pointer out_ptr = Pointer.to(output);
        Pointer size_ptr = Pointer.to(sizeOfInput);

        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        CL.setExceptionsEnabled(true);

        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        cl_context context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        cl_command_queue commandQueue =
                clCreateCommandQueue(context, device, 0, null);

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
                Sizeof.cl_double * numValid, null, null);
        memObjects[4] = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_int, size_ptr, null);

        cl_program program = clCreateProgramWithSource(context,
                1, new String[]{programSource}, null, null);

        clBuildProgram(program, 0, null, null, null, null);

        cl_kernel kernel = clCreateKernel(program, "calculateStars", null);

        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memObjects[2]));
        clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(memObjects[3]));
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(memObjects[4]));


        long global_work_size[] = new long[]{numValid};
        long local_work_size[] = new long[]{1};


        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                global_work_size, local_work_size, 0, null, null);
        long currentTime = System.nanoTime();

        clEnqueueReadBuffer(commandQueue, memObjects[3], CL_TRUE, 0,
                numValid * Sizeof.cl_double, out_ptr, 0, null, null);


        clReleaseMemObject(memObjects[0]);
        clReleaseMemObject(memObjects[1]);
        clReleaseMemObject(memObjects[2]);
        clReleaseMemObject(memObjects[3]);
        clReleaseMemObject(memObjects[4]);
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);

        long elapsed = (System.nanoTime() - currentTime) / 1000000000;
        System.out.println("Process complete. Elapsed time: " + elapsed + " seconds.");
        return output;
    }
}
