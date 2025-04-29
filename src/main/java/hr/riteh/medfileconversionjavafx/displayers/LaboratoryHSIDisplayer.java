package hr.riteh.medfileconversionjavafx.displayers;

import hr.riteh.medfileconversionjavafx.helper.Dimension;
import javafx.scene.Scene;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
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
    private int selectedSlice;
    private float[] data;
    private int[] displayData;
    private int[][] displayData2d;
    private String hdfDirectoryPath;
    private String hdfFilePath;

    Map<Dimension, Integer> dimensionMap = Map.of(
            Dimension.WAVELENGTHS, 2048,
            Dimension.SAMPLES, 1224,
            Dimension.POSITIONS, 400
    );

    public LaboratoryHSIDisplayer(Map<String, String> metadataMap, double[] positions, double[] wavelengths, boolean isDirectoryProvided,String hdfPath, Scene scene) {
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
        if (isDirectoryProvided) {
            this.hdfDirectoryPath = hdfPath;
            findHdfFile();
        }
        else this.hdfFilePath = hdfPath;
        System.out.println("HDF5 file path: " + hdfFilePath);
    }

    private void findHdfFile() {
        System.out.println("Finding HDF5 file in directory: " + hdfDirectoryPath);
        File directory = new File(hdfDirectoryPath);
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".h5"));
        if (files != null && files.length > 0) {
            hdfFilePath = files[0].getAbsolutePath();
        } else {
            System.out.println("No HDF5 file found in the directory.");
        }
    }

    public void setData(float[] data) {
        this.data = data;
    }

    public Dimension getSelectedDimension() {
        return selectedDimension;
    }

    public Dimension getFirstDimension() {
        return firstDimension;
    }

    public Dimension getSecondDimension() {
        return secondDimension;
    }

    public void setSelectedDimension(Dimension selectedDimension) {
        this.selectedDimension = selectedDimension;
    }

    public void setFirstDimension(Dimension firstDimension) {
        this.firstDimension = firstDimension;
    }

    public void setSecondDimension(Dimension secondDimension) {
        this.secondDimension = secondDimension;
    }

    public void setNumSelectedDimension(int numSelectedDimension) {
        this.numSelectedDimension = numSelectedDimension;
    }

    public int getNumSelectedDimension() {
        return numSelectedDimension;
    }

    public void setNumFirstDimension(int numFirstDimension) {
        this.numFirstDimension = numFirstDimension;
    }

    public void setNumSecondDimension(int numSecondDimension) {
        this.numSecondDimension = numSecondDimension;
    }

    public void setSelectedSlice(int selectedSlice) {
        System.out.println(Math.max(Math.min(selectedSlice, numSelectedDimension - 1), 0));
        this.selectedSlice = Math.max(Math.min(selectedSlice, numSelectedDimension - 1), 0);
    }

    public void setMemoryDims(long[] memoryDims) {
        this.memoryDims = memoryDims;
    }

    public void setBlock(long[] block) {
        this.block = block;
    }

    public int getSelectedSlice() {
        return selectedSlice;
    }

    public Map<String, String> getMetadataMap() {
        return metadataMap;
    }

    public void printMetadata() {
        for (Map.Entry<String, String> entry : metadataMap.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    public double[] getWavelengths() {
        return wavelengths;
    }

    public void incrementSelectedSlice() {
        selectedSlice = Math.floorMod(selectedSlice+1, numSelectedDimension);
    }

    public void decrementSelectedSlice() {
        selectedSlice = Math.floorMod(selectedSlice-1, numSelectedDimension);
    }

    public void resetDisplayerOnSelectedDim(Dimension selectedDimension, Dimension firstDimension, Dimension secondDimension) {
        setSelectedDimension(selectedDimension);
        setFirstDimension(firstDimension);
        setSecondDimension(secondDimension);

        setNumSelectedDimension(dimensionMap.get(selectedDimension));
        setNumFirstDimension(dimensionMap.get(firstDimension));
        setNumSecondDimension(dimensionMap.get(secondDimension));

        switch (selectedDimension) {
            case WAVELENGTHS -> {
                setMemoryDims(new long[]{dimensionMap.get(Dimension.POSITIONS), 1, dimensionMap.get(Dimension.SAMPLES)});
                setBlock(new long[]{dimensionMap.get(Dimension.POSITIONS), 1, dimensionMap.get(Dimension.SAMPLES)});
            }

            case POSITIONS -> {
                setMemoryDims(new long[]{1, dimensionMap.get(Dimension.WAVELENGTHS), dimensionMap.get(Dimension.SAMPLES)});
                setBlock(new long[]{1, dimensionMap.get(Dimension.WAVELENGTHS), dimensionMap.get(Dimension.SAMPLES)});
            }

            case SAMPLES -> {
                setMemoryDims(new long[]{dimensionMap.get(Dimension.POSITIONS), dimensionMap.get(Dimension.WAVELENGTHS), 1});
                setBlock(new long[]{dimensionMap.get(Dimension.POSITIONS), dimensionMap.get(Dimension.WAVELENGTHS), 1});
            }
        }

        setSelectedSlice(0);
    }

    private int[] getData() throws HDF5Exception {
//        int hdf_file_id = -1;
//        int dataset_id = -1;
//        int dataspace_id = -1;
//        int memspace_id = -1;
//        try {
//            hdf_file_id = H5.H5Fopen("D:\\Faks\\4. godina\\Izborni projekt\\medFile-Conversion\\laboratory_hsi.h5", HDF5Constants.H5F_ACC_RDONLY,
//                    HDF5Constants.H5P_DEFAULT);
//
////            System.out.println("path: " + hdfDirectoryPath + "\\laboratory_hsi.h5");
////            System.out.println(new File(hdfDirectoryPath + "\\laboratory_hsi.h5").exists());
//
//            hdf_file_id = H5.H5Fopen(hdfDirectoryPath + "\\laboratory_hsi.h5", HDF5Constants.H5F_ACC_RDONLY,
//                    HDF5Constants.H5P_DEFAULT);
//
//            dataset_id = H5.H5Dopen(hdf_file_id, "reflectance");
//            dataspace_id = H5.H5Dget_space(dataset_id);
//
//            if (selectedDimension == Dimension.POSITIONS) start = new long[]{selectedSlice, 0, 0};
//            else if (selectedDimension == Dimension.WAVELENGTHS) start = new long[]{0, selectedSlice, 0};
//            else start = new long[]{0, 0, selectedSlice};
//
//            memspace_id = H5.H5Screate_simple(memoryDims.length, memoryDims, null);
//            // Allocate space for the data
//            data = new float[(int) (memoryDims[0] * memoryDims[1] * memoryDims[2])];
//
//            H5.H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, stride, count, block);
//            H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, memspace_id, dataspace_id, HDF5Constants.H5P_DEFAULT, data);
//
//            float max = 0;
//            for (int i=0; i<data.length; i++) {
//                if (data[i] > max) {
//                    max = data[i];
//                }
//            }
//
//            int[] displayData = new int[(int) (memoryDims[0] * memoryDims[1] * memoryDims[2])];
//            for (int i=0; i<data.length; i++) {
//                displayData[i] = (int)((data[i]/max)*255);
//            }
//
//            return displayData;
//        } finally {
//            if (memspace_id >= 0) H5.H5Sclose(memspace_id);
//            if (dataspace_id >= 0) H5.H5Sclose(dataspace_id);
//            if (dataset_id >= 0) H5.H5Dclose(dataset_id);
//            if (hdf_file_id >= 0) H5.H5Fclose(hdf_file_id);
//        }

        int hdf_file_id = -1;
        int dataset_id = -1;
        int dataspace_id = -1;
        int memspace_id = -1;

//            hdf_file_id = H5.H5Fopen("D:\\Faks\\4. godina\\Izborni projekt\\medFile-Conversion\\laboratory_hsi.h5", HDF5Constants.H5F_ACC_RDONLY,
//                    HDF5Constants.H5P_DEFAULT);

//            System.out.println("path: " + hdfDirectoryPath + "\\laboratory_hsi.h5");
//            System.out.println(new File(hdfDirectoryPath + "\\laboratory_hsi.h5").exists());

            //hdf_file_id = H5.H5Fopen(hdfDirectoryPath + "\\laboratory_hsi.h5", HDF5Constants.H5F_ACC_RDONLY,
            //        HDF5Constants.H5P_DEFAULT);

            hdf_file_id = H5.H5Fopen(hdfFilePath, HDF5Constants.H5F_ACC_RDONLY,
                HDF5Constants.H5P_DEFAULT);

            dataset_id = H5.H5Dopen(hdf_file_id, "reflectance");
            dataspace_id = H5.H5Dget_space(dataset_id);

        // Get the dimensions of the dataspace
        long[] dims = new long[3]; // Adjust the size of the array based on the expected number of dimensions
        H5.H5Sget_simple_extent_dims(dataspace_id, dims, null);

        // Print the dimensions
        System.out.println("Dimensions: " + Arrays.toString(dims));


        if (selectedDimension == Dimension.POSITIONS) start = new long[]{selectedSlice, 0, 0};
            else if (selectedDimension == Dimension.WAVELENGTHS) start = new long[]{0, selectedSlice, 0};
            else start = new long[]{0, 0, selectedSlice};

            memspace_id = H5.H5Screate_simple(memoryDims.length, memoryDims, null);
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

    public BufferedImage getImg() {
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
