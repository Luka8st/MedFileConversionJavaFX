package hr.riteh.medfileconversionjavafx.displayers;

import hr.riteh.medfileconversionjavafx.helper.Dimension;
import javafx.scene.Scene;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;

import java.awt.image.BufferedImage;
import java.util.Map;


public class LaboratoryHSIDisplayer {

    private static final int NUM_WAVELENGTHS = 2048; // 2048
    private static final int NUM_SAMPLES = 1224; // 1224
    private static final int NUM_POS_HSI = 400; // 400
    private Dimension selectedDimension, firstDimension, secondDimension;
    private int numSelectedDimension, numFirstDimension, numSecondDimension;
    private long[] stride = null;  // Null means default to 1 (contiguous)
    private long[] count = {1, 1, 1};  // Single block
    private long[] block;  // Size of the block
    private long[] start;
    private long[] memoryDims;
    private Map<String, String> metadataMap;
    private double[] positions;
    private double[] wavelengths;
    private int hdf_file_id = -1;
    private int selectedSlice;
    private float[] data;
    private int[] displayData;
    private int[][] displayData2d;

    public LaboratoryHSIDisplayer(Map<String, String> metadataMap, double[] positions, double[] wavelengths, Scene scene) {
        selectedDimension = Dimension.WAVELENGTHS;
        firstDimension = Dimension.POSITIONS;
        secondDimension = Dimension.SAMPLES;

        numSelectedDimension = NUM_WAVELENGTHS;
        numFirstDimension = NUM_POS_HSI;
        numSecondDimension = NUM_SAMPLES;

        block = new long[]{numFirstDimension, 1, numSecondDimension};  // Size of the block

        memoryDims = new long[]{numFirstDimension, 1, numSecondDimension};

        this.metadataMap = metadataMap;
        this.positions = positions;
        this.wavelengths = wavelengths;
    }

    private int[] getData() throws HDF5Exception {
        hdf_file_id = H5.H5Fopen("D:\\Faks\\4. godina\\Izborni projekt\\medFile-Conversion\\laboratory_hsi.h5", HDF5Constants.H5F_ACC_RDONLY,
                HDF5Constants.H5P_DEFAULT);

        int dataset_id = H5.H5Dopen(hdf_file_id, "reflectance");
        int dataspace_id = H5.H5Dget_space(dataset_id);

        if (selectedDimension == Dimension.POSITIONS) start = new long[]{selectedSlice, 0, 0};
        else if (selectedDimension == Dimension.WAVELENGTHS) start = new long[]{0, selectedSlice, 0};
        else start = new long[]{0, 0, selectedSlice};

        int memspace_id = H5.H5Screate_simple(memoryDims.length, memoryDims, null);
        // Allocate space for the data
        data = new float[(int) (memoryDims[0] * memoryDims[1] * memoryDims[2])];

        H5.H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, stride, count, block);
        H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, memspace_id, dataspace_id, HDF5Constants.H5P_DEFAULT, data);

        float max = 0;
        for (int i=0; i<data.length; i++) {
            if (data[i] > max) {
                max = data[i];
            }
        }

        int[] displayData = new int[(int) (memoryDims[0] * memoryDims[1] * memoryDims[2])];
        for (int i=0; i<data.length; i++) {
            displayData[i] = (int)((data[i]/max)*255);
        }

        return displayData;
    }

    public BufferedImage paint() {
        displayData = getData();
        displayData2d = new int[numFirstDimension][numSecondDimension];
        for ( int i = 0; i < numFirstDimension; i++ )
            System.arraycopy(displayData, (i*numSecondDimension), displayData2d[i], 0, numSecondDimension);

        BufferedImage img = new BufferedImage(numFirstDimension, numSecondDimension, BufferedImage.TYPE_BYTE_GRAY);

        for (int x = 0; x < numFirstDimension; x++) {
            for (int y = 0; y < numSecondDimension; y++) {
                // Ensure the value is within 0-255
                int value = (int) Math.min(Math.max(displayData2d[x][y], 0), 255);
                int rgb = value << 16 | value << 8 | value; // Convert to grayscale (R=G=B)
                img.setRGB(x, y, rgb);

            }
        }

        return img;
    }
}
