package hr.riteh.medfileconversionjavafx.displayers;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;

public class MicroscopyHSIDisplayer {
    private int selectedSlice;
    private int numDarkImages;
    private Map<String, String> darkMetadataMap;
    private int numGreenImages;
    private Map<String, String> greenMetadataMap;
    private int numWhiteImages;
    private Map<String, String> whiteMetadataMap;
    private String selectedDataset;
    private String hdfDirectoryPath;
    private String hdfFilePath;

    public MicroscopyHSIDisplayer(Map<String, String> darkMetadataMap, int numDarkImages,
                                  Map<String, String> greenMetadataMap, int numGreenImages,
                                  Map<String, String> whiteMetadataMap, int numWhiteImages,
                                  boolean isDirectoryProvided, String hdfPath) {
        this.darkMetadataMap = darkMetadataMap;
        this.numDarkImages = numDarkImages;
        this.greenMetadataMap = greenMetadataMap;
        this.numGreenImages = numGreenImages;
        this.whiteMetadataMap = whiteMetadataMap;
        this.numWhiteImages = numWhiteImages;
        if (isDirectoryProvided) {
            this.hdfDirectoryPath = hdfPath;
            findHdfFile();
        }
        else this.hdfFilePath = hdfPath;
        System.out.println("HDF Directory Path: " + hdfDirectoryPath);
        selectedDataset = "dark";
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

    public void setSelectedSlice(int selectedSlice) {
        this.selectedSlice = selectedSlice;
    }

    public void setSelectedDataset(String selectedDataset) {
        this.selectedDataset = selectedDataset;
    }

    public int getSelectedSlice() {
        return selectedSlice;
    }

    public int getNumDarkImages() {
        return numDarkImages;
    }

    public Map<String, String> getDarkMetadataMap() {
        return darkMetadataMap;
    }

    public int getNumGreenImages() {
        return numGreenImages;
    }

    public Map<String, String> getGreenMetadataMap() {
        return greenMetadataMap;
    }

    public int getNumWhiteImages() {
        return numWhiteImages;
    }

    public Map<String, String> getWhiteMetadataMap() {
        return whiteMetadataMap;
    }

    public BufferedImage getImg() {
        short[] displayData = getData();
        short[][] displayData2d = new short[2560][2160];
        for ( int i = 0; i < 2560; i++ )
            System.arraycopy(displayData, (i*2160), displayData2d[i], 0, 2160);

        BufferedImage img = new BufferedImage(2560, 2160, BufferedImage.TYPE_USHORT_GRAY);

        for (int x = 0; x < 2560; x++) {
            for (int y = 0; y < 2160; y++) {
                // Ensure the value is within 0-255
                int value = (int) Math.min(Math.max(displayData2d[x][y]/257, 0), 255);  //257 = 65535/255
                int rgb = value << 16 | value << 8 | value; // Convert to grayscale (R=G=B)
                img.setRGB(x, y, rgb);

            }
        }

        return img;
    }

    private short[] getData() {
        int hdf_file_id = -1;
        int dataset_id = -1;
        int dataspace_id = -1;
        int memspace_id = -1;

        //System.out.println("Reading Laboratory HSI file: " + hdfDirectoryPath + "\\microscopy_hsi.h5");
        hdf_file_id = H5.H5Fopen(hdfFilePath, HDF5Constants.H5F_ACC_RDONLY,
                HDF5Constants.H5P_DEFAULT);

        dataset_id = H5.H5Dopen(hdf_file_id, selectedDataset);
        dataspace_id = H5.H5Dget_space(dataset_id);

        long[] dims = new long[3]; // Adjust the size of the array based on the expected number of dimensions
        H5.H5Sget_simple_extent_dims(dataspace_id, dims, null);

        System.out.println("Dimensions: " + dims[0] + ", " + dims[1] + ", " + dims[2]);

        long[] start = {selectedSlice, 0, 0};
        long[] stride = null;
        long[] count = {1, 1, 1};
        long[] block = {1, dims[1], dims[2]};

        long[] memoryDims = new long[]{1, dims[1], dims[2]};
        short[] data = new short[(int) (memoryDims[0] * memoryDims[1] * memoryDims[2])];

        memspace_id = H5.H5Screate_simple(memoryDims.length, memoryDims, null);

        H5.H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, stride, count, block);
        H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_USHORT, memspace_id, dataspace_id, HDF5Constants.H5P_DEFAULT, data);

        return data;
    }

}
