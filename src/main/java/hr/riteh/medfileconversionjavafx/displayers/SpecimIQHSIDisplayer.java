package hr.riteh.medfileconversionjavafx.displayers;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Map;

public class SpecimIQHSIDisplayer {

    private String currentDataset;
    private String hdfDirectoryPath;
    private String imageName;
    private long[] memoryDims = new long[3];
    private int selectedSlice = 0;
    private int lines;
    private int samples;
    private int bands;
    private Map<String, Object> resultMetadataMap;
    private Map<String, Object> captureMetadataMap;

    public SpecimIQHSIDisplayer(String hdfDirectoryPath, Map<String, Object> captureMetadataMap, Map<String, Object> resultMetadataMap) {
        this.hdfDirectoryPath = hdfDirectoryPath;
//        this.imageName = imageName;
//        this.currentDataset = "rawData_" + imageName; // Default dataset
        this.currentDataset = "rawData"; // Default dataset
        this.captureMetadataMap = captureMetadataMap;
        this.resultMetadataMap = resultMetadataMap;
    }

    public Map<String, Object> getResultMetadataMap() {
        return resultMetadataMap;
    }

    public Map<String, Object> getCaptureMetadataMap() {
        return captureMetadataMap;
    }

    public String[] getWavelengths() {
        return (String[]) captureMetadataMap.get("wavelength");
    }

    public int getLines() {
        return lines;
    }

    public void setCurrentDataset(String datasetName) {
        this.currentDataset = datasetName;
    }

    public void setDimensions(int[] dimensions) {
        this.lines = dimensions[0];
        this.samples = dimensions[1];
        this.bands = dimensions[2];
    }

    public void incrementSelectedSlice() {
        selectedSlice = Math.floorMod(selectedSlice+1, lines);
    }

    public void decrementSelectedSlice() {
        selectedSlice = Math.floorMod(selectedSlice-1, lines);
    }

    public void setSelectedSlice(int slice) {
        selectedSlice = Math.floorMod(slice, lines);
    }

    public int getSelectedSlice() {
        return selectedSlice;
    }

    /*public BufferedImage getImg() {
        System.out.println("Getting image");
        short[] data = getData();
        int[][] data2D = new int[(int) memoryDims[1]][(int) memoryDims[2]];

        for ( int i = 0; i < memoryDims[1]; i++ ) {
            System.out.println("i: " + i);
            System.arraycopy(data, (i * (int) memoryDims[2]), data2D[i], 0, (int) memoryDims[2]);
        }

        BufferedImage img = new BufferedImage((int)memoryDims[1], (int)memoryDims[2], BufferedImage.TYPE_BYTE_GRAY);


        for (int x = 0; x < memoryDims[1]; x++) {
            System.out.println("x: " + x);
            for (int y = 0; y < memoryDims[2]; y++) {
                // Ensure the value is within 0-255
                int value = (int) Math.min(Math.max(data2D[x][y], 0), 255);
                int rgb = value << 16 | value << 8 | value; // Convert to grayscale (R=G=B)
                img.setRGB(x, y, rgb);

            }
        }

        return img;
    }*/

    public BufferedImage getImg() {
        System.out.println("Getting image");
        short[] data = getData();

        if (data == null || memoryDims[1] <= 0 || memoryDims[2] <= 0) {
            throw new RuntimeException("Invalid data or memory dimensions.");
        }

        // Validate data size
        if (data.length != memoryDims[1] * memoryDims[2]) {
            throw new RuntimeException("Data size does not match expected dimensions.");
        }

        // Initialize 2D array
        short[][] data2D = new short[(int) memoryDims[1]][(int) memoryDims[2]];

        // Copy data into 2D array
        for (int i = 0; i < memoryDims[1]; i++) {
//            System.out.println("i: " + i);
            System.arraycopy(data, i * (int) memoryDims[2], data2D[i], 0, (int) memoryDims[2]);
        }


        System.out.println(memoryDims[1] + " " + memoryDims[2]);
        // Create BufferedImage
         BufferedImage img = new BufferedImage((int) memoryDims[1], (int) memoryDims[2], BufferedImage.TYPE_BYTE_GRAY);
//        BufferedImage img = new BufferedImage((int) memoryDims[2], (int) memoryDims[1], BufferedImage.TYPE_INT_RGB);


        // Populate image pixels
//        for (int y = 0; y < memoryDims[1]; y++) {
//            for (int x = 0; x < memoryDims[2]; x++) {
//                int value = Math.min(Math.max(data2D[y][x], 0), 255);
//                int rgb = value << 16 | value << 8 | value; // Convert to grayscale
//                img.setRGB(x, y, rgb);
//            }
//        }
        for (int sample = 0; sample < memoryDims[1]; sample++) {
            for (int band = 0; band < memoryDims[2]; band++) {
                int value = data2D[sample][band]; // Use the first line for simplicity
                int rgb = value << 16 | value << 8 | value; // Convert to grayscale (R=G=B)
                img.setRGB(sample, band, rgb); // Set the pixel value
            }
        }

        System.out.println("Finished creating image");
        return img;
    }

    public BufferedImage getPngImg(String pngName) {
        System.out.println("Getting image");
        float[][][] data = getPngData(pngName);

        System.out.println("data: " + data.length + " " + data[0].length + " " + data[0][0].length);
        System.out.println(data[18][2][0]);
        System.out.println(data[2][18][0]);

        BufferedImage img = new BufferedImage(data[0].length, data.length, BufferedImage.TYPE_INT_ARGB);
        System.out.println("img: " + img.getWidth() + " " + img.getHeight());

        for (int line = 0; line < data.length; line++) {
            for (int sample = 0; sample < data[0].length; sample++) {
//                System.out.println("line: " + line + " sample: " + sample);
                int r = (int) data[line][sample][0];
                int g = (int) data[line][sample][1];
                int b = (int) data[line][sample][2];
                int a = (int) data[line][sample][3];
                int rgba = (a << 24) | (r << 16) | (g << 8) | b;
//                img.setRGB(sample, line, rgba);
                img.setRGB(sample, line, rgba);
            }
        }

        return img;
    }


    /*
    private short[] getData() {
        System.out.println("Getting data");
        int hdf_file_id = -1;
        int dataset_id = -1;
        int dataspace_id = -1;
        int memspace_id = -1;

        hdf_file_id = H5.H5Fopen(hdfDirectoryPath + "\\specimiq_hsi.h5", HDF5Constants.H5F_ACC_RDONLY,
                HDF5Constants.H5P_DEFAULT);
        dataset_id = H5.H5Dopen(hdf_file_id, "rawData 2139");
        dataspace_id = H5.H5Dget_space(dataset_id);

        long[] dims = new long[3]; // Adjust the size of the array based on the expected number of dimensions
        H5.H5Sget_simple_extent_dims(dataspace_id, dims, null);
        memoryDims = dims;

        // Print the dimensions
        System.out.println("Dimensions: " + Arrays.toString(dims));

        // Allocate space for the data
        short[] data = new short[(int) (memoryDims[0] * memoryDims[1] * memoryDims[2])];
        memspace_id = H5.H5Screate_simple(memoryDims.length, memoryDims, null);


        start = new long[]{0, 0, 0};  // Start of hyperslab
        block = new long[]{1, memoryDims[1], memoryDims[2]};  // Block size
        H5.H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, stride, count, block);
        H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_SHORT, memspace_id, dataspace_id, HDF5Constants.H5P_DEFAULT, data);

        System.out.println("End of getData");
        return data;
    }*/

    private short[] getData() {
        System.out.println("Getting slice " + selectedSlice);
        int hdf_file_id = -1;
        int dataset_id = -1;
        int dataspace_id = -1;
        int memspace_id = -1;

        try {
            // Open HDF5 file and dataset
            hdf_file_id = H5.H5Fopen(hdfDirectoryPath + "\\specimiq_hsi.h5", HDF5Constants.H5F_ACC_RDONLY,
                    HDF5Constants.H5P_DEFAULT);
            if (hdf_file_id < 0) {
                throw new RuntimeException("Failed to open HDF5 file.");
            }

            System.out.println("Current dataset: " + currentDataset);
//            dataset_id = H5.H5Dopen(hdf_file_id, "rawData 2139");
            dataset_id = H5.H5Dopen(hdf_file_id, currentDataset, HDF5Constants.H5P_DEFAULT);
            if (dataset_id < 0) {
                throw new RuntimeException("Failed to open dataset.");
            }

            dataspace_id = H5.H5Dget_space(dataset_id);
            if (dataspace_id < 0) {
                throw new RuntimeException("Failed to get dataspace.");
            }

            // Get dataset dimensions
            long[] dims = new long[3];
            H5.H5Sget_simple_extent_dims(dataspace_id, dims, null);
            memoryDims = dims;
            System.out.println("Dimensions: " + Arrays.toString(dims));

            // Allocate space for one slice
            short[] data = new short[(int) (memoryDims[1] * memoryDims[2])];

            // Create memory space for the slice
            memspace_id = H5.H5Screate_simple(2, new long[]{memoryDims[1], memoryDims[2]}, null);
            if (memspace_id < 0) {
                throw new RuntimeException("Failed to create memory space.");
            }

            long[] start = new long[]{selectedSlice, 0, 0}; // Start at the i-th slice
            long[] stride = new long[]{1, 1, 1}; // Contiguous access
            long[] count = new long[]{1, 1, 1};  // Read one slice
            long[] block = new long[]{1, memoryDims[1], memoryDims[2]}; // Block size is one slice

            H5.H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, stride, count, block);

            // Read data into the allocated array
            H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_SHORT, memspace_id, dataspace_id, HDF5Constants.H5P_DEFAULT, data);

            System.out.println("Finished reading slice " + selectedSlice);
            return data;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while reading HDF5 data.", e);
        } finally {
            // Close HDF5 resources
            try {
                if (memspace_id >= 0) H5.H5Sclose(memspace_id);
                if (dataspace_id >= 0) H5.H5Sclose(dataspace_id);
                if (dataset_id >= 0) H5.H5Dclose(dataset_id);
                if (hdf_file_id >= 0) H5.H5Fclose(hdf_file_id);
            } catch (Exception e) {
                System.err.println("Failed to close HDF5 resources: " + e.getMessage());
            }
        }
    }

    private float[][][] getPngData(String pngName) {
        int hdf_file_id = -1;
        int dataset_id = -1;
        int dataspace_id = -1;
        int memspace_id = -1;

        try {
            // Open HDF5 file and dataset
            hdf_file_id = H5.H5Fopen(hdfDirectoryPath + "\\specimiq_hsi.h5", HDF5Constants.H5F_ACC_RDONLY,
                    HDF5Constants.H5P_DEFAULT);
            if (hdf_file_id < 0) {
                throw new RuntimeException("Failed to open HDF5 file.");
            }

            dataset_id = H5.H5Dopen(hdf_file_id, pngName, HDF5Constants.H5P_DEFAULT);
            if (dataset_id < 0) {
                throw new RuntimeException("Failed to open dataset.");
            }

            dataspace_id = H5.H5Dget_space(dataset_id);
            if (dataspace_id < 0) {
                throw new RuntimeException("Failed to get dataspace.");
            }

            long[] dims = new long[3];
            H5.H5Sget_simple_extent_dims(dataspace_id, dims, null);
            System.out.println("png Dimensions: " + Arrays.toString(dims));

            float[] data = new float[(int) (dims[0] * dims[1] * dims[2])];
            memspace_id = H5.H5Screate_simple(3, dims, null);

            if (memspace_id < 0) {
                throw new RuntimeException("Failed to create memory space.");
            }

            long[] start = new long[]{0, 0, 0}; // Start at the i-th slice
            long[] stride = new long[]{1, 1, 1}; // Contiguous access
            long[] count = new long[]{1, 1, 1};  // Read one slice
            long[] block = dims; // Block size is one slice

            H5.H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, stride, count, block);

            H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, memspace_id, dataspace_id, HDF5Constants.H5P_DEFAULT, data);

            float[][][] data3D = new float[(int) dims[0]][(int) dims[1]][(int) dims[2]];

            for (int i = 0; i < dims[0]; i++) {
                for (int j = 0; j < dims[1]; j++) {
                    System.arraycopy(data, (i * (int) dims[1] + j) * (int) dims[2], data3D[i][j], 0, (int) dims[2]);
                }
            }

            return data3D;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while reading HDF5 data.", e);
        } finally {
            // Close HDF5 resources
            try {
                if (memspace_id >= 0) H5.H5Sclose(memspace_id);
                if (dataspace_id >= 0) H5.H5Sclose(dataspace_id);
                if (dataset_id >= 0) H5.H5Dclose(dataset_id);
                if (hdf_file_id >= 0) H5.H5Fclose(hdf_file_id);
            } catch (Exception e) {
                System.err.println("Failed to close HDF5 resources: " + e.getMessage());
            }
        }
    }

    public boolean hasMultipleLines() {
        System.out.println("lines: " + lines);
        return lines > 1;
    }
}
