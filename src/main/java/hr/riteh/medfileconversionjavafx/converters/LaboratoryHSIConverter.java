package hr.riteh.medfileconversionjavafx.converters;

//import hdf.object.h5.H5File;

//import as.hdfql.*;
import hr.riteh.medfileconversionjavafx.exceptions.DirectoryNotFoundException;
import hr.riteh.medfileconversionjavafx.helper.Triplet;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;
//import ncsa.hdf.object.h5.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LaboratoryHSIConverter {
    private static final List<String> datasetKeys = Arrays.asList(
            "file type", "data type", "bands", "gain", "byte order", "samples", "header offset", "lines", "interleave",
            "random"
    );
    private static final List<String> deviceKeys = Arrays.asList(
            "illumination940", "illumination780", "illumination850", "wavelength", "illuminationW", "sensor type", "modType",
            "integrationTime"
    );

    private static final List<String> infoKeys = Arrays.asList(
            "InstitutionName", "patID", "studDate", "patient data", "physician", "patName", "studAccNb", "serNb", "patSex",
            "description", "patBD", "studID"
    );

    private double[] positions;
    private double[] wavelengths;

    private static int NUM_WAVELENGTHS; // 2048
    private static int NUM_SAMPLES; // 1224
    private static int NUM_POS_HSI; // 400
    private static final String HEADER_FILENAME_TEMPLATE = "_white_white.hdr";
    private static final String RAW_DATA_FILENAME_TEMPLATE = "_sample_raw_hsi.mat";
    private static final String WHITE_REFERENCE_FILENAME_TEMPLATE  = "_white_white.img";

    private int write_file_id = -1;
    private int write_group_id = -1;
    int read_file_id = -1;
    private String basePath;
    private String headerPath;
    private String rawDataPath;
    private String whiteReferencePath;
    private String hdfDirectoryPath;

    Map<String, String> dataMap;
    List<Triplet<String, String, String>> manifestFiles; // (path, type, extension)

    public Map<String, String> getDataMap() {
        return dataMap;
    }

    public double[] getPositions() {
        return positions;
    }

    public double[] getWavelengths() {
        return wavelengths;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public void setHdfDirectoryPath(String hdfDirectoryPath) {
        this.hdfDirectoryPath = hdfDirectoryPath;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getHdfDirectoryPath() {
        return hdfDirectoryPath;
    }

    public LaboratoryHSIConverter(String basePath, String hdfDirectoryPath) throws DirectoryNotFoundException {
        //    String basePath = System.getenv("BASE_DIR");

//        System.out.print("Enter the path to the directory that contains all the necessary files: ");
//        String basePath = (new Scanner(System.in)).nextLine();
        if (!Files.exists(Path.of(basePath))) throw new DirectoryNotFoundException("Directory with the given path doesn't exist");

        dataMap= new Hashtable<>();
        manifestFiles = new ArrayList<Triplet<String, String, String>>();
        this.basePath = basePath;
        this.hdfDirectoryPath = hdfDirectoryPath;

    }

    public void run() throws Exception {
        findAllFiles();
        createHdfFile();
        readHdrFile();
        storeMetadataXml();
        openRawDataFile();
        readPositions();
        readAcquiredAndStoreReflectance();
        storeManifestXml();
        closeHdfFile();
    }

    public void findAllFiles() throws FileNotFoundException {
        Set<String> files = Stream.of(new File(basePath).listFiles())
                .filter(file -> !file.isDirectory() && !file.getName().startsWith("._"))
                .map(File::getName)
                .collect(Collectors.toSet());

        boolean containsHeader = false, containsRaw = false, containsWhite = false;
        for (String s:files.stream().toArray(String[]::new)) {
            if (s.contains(HEADER_FILENAME_TEMPLATE)) {
                headerPath = String.valueOf(Path.of(basePath, s));
                containsHeader = true;
            }
            else if (s.contains(RAW_DATA_FILENAME_TEMPLATE)) {
                rawDataPath = String.valueOf(Path.of(basePath, s));
                containsRaw = true;
            }
            else if (s.contains(WHITE_REFERENCE_FILENAME_TEMPLATE)) {
                whiteReferencePath = String.valueOf(Path.of(basePath, s));
                containsWhite = true;
            }
        }
        if (!containsHeader) throw new FileNotFoundException("Header file is not found");
        if (!containsRaw) throw new FileNotFoundException("File that contains raw data is not found");
        if (!containsWhite) throw new FileNotFoundException("File that contains white reference data is not found");
    }

    public void openRawDataFile() {
        read_file_id = H5.H5Fopen(rawDataPath, HDF5Constants.H5F_ACC_RDONLY, HDF5Constants.H5P_DEFAULT);

        //-----------------------------------------------------------------------------------------

        String datasetName = "/acquired";

        int read_dataset_id = -1;
        int read_dataspace_id = -1;

        read_dataset_id = H5.H5Dopen(read_file_id, datasetName);
        read_dataspace_id = H5.H5Dget_space(read_dataset_id);

        // Get the dimensions of the dataspace
        long[] readdims = new long[3]; // Adjust the size of the array based on the expected number of dimensions
        H5.H5Sget_simple_extent_dims(read_dataspace_id, readdims, null);

        // Print the dimensions
        System.out.println("Read Dimensions: " + Arrays.toString(readdims));

        NUM_POS_HSI = (int) readdims[0];
        NUM_WAVELENGTHS = (int) readdims[1];
        NUM_SAMPLES = (int) readdims[2];
    }

    public void readHdrFile() throws FileNotFoundException {
        Scanner sc = new Scanner(new File(headerPath));

        while (sc.hasNextLine()) {
            String line = sc.nextLine();

            String[] splitLine = line.split(" = ");
            if (splitLine.length == 2){
                if (splitLine[0].trim().equals("wavelength")) {
                    String nextLine = sc.nextLine();
                    dataMap.put(splitLine[0], nextLine.replaceAll("}",  ""));
                    String[] stringValues = nextLine.replaceAll("}",  "").split(", ");
                    wavelengths = new double[stringValues.length];
                    for (int i=0; i<wavelengths.length; i++) wavelengths[i] = Double.parseDouble(stringValues[i]);
                }
                else {
                    dataMap.put(splitLine[0], splitLine[1]);
                }
            }
        }

        sc.close();
    }

    public void storeMetadataXml() throws ParserConfigurationException, TransformerException, IOException, HDF5Exception {
        // Create a DocumentBuilder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Create a new Document
        Document document = builder.newDocument();

        // Create root element
        Element root = document.createElement("metadata");
        document.appendChild(root);

        // Create book elements and add text content
        Element datasetBook = document.createElement("dataset");
        root.appendChild(datasetBook);

        Element deviceBook = document.createElement("device");
        root.appendChild(deviceBook);

        Element infoBook = document.createElement("info");
        root.appendChild(infoBook);

        for (String name : dataMap.keySet())
        {
            String value = dataMap.get(name);

            Element sub = document.createElement("key");
            sub.appendChild(document.createTextNode(value));
            sub.setAttribute("field", name);

            if (datasetKeys.contains(name)) {
                datasetBook.appendChild(sub);
            } else if (deviceKeys.contains(name)) {
                deviceBook.appendChild(sub);
            } else if (infoKeys.contains(name)) {
                infoBook.appendChild(sub);
            }
        }

        // Write to XML file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
//        DOMSource source = new DOMSource(document);

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        String xmlString = writer.getBuffer().toString();

        storeStringAsAscii("metadata", xmlString);

        // just to check
        stringToDom(xmlString, hdfDirectoryPath + "\\metadata_lab.xml");

        manifestFiles.add(new Triplet<>(hdfDirectoryPath + "\\metadata.xml", "metadata", "xml"));
    }

    public void storeManifestXml() throws ParserConfigurationException, TransformerException, IOException, HDF5Exception {
        // Create a DocumentBuilder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Create a new Document
        Document document = builder.newDocument();

        // Create root element
        Element root = document.createElement("manifest");
        document.appendChild(root);

        for (int i=0; i<manifestFiles.size(); i++) {
            Element sub = document.createElement("file");
            sub.appendChild(document.createTextNode(manifestFiles.get(i).getFirst()));
            sub.setAttribute("type", manifestFiles.get(i).getSecond());
            sub.setAttribute("extension", manifestFiles.get(i).getThird());

            root.appendChild(sub);
        }


        // Write to XML file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

//        DOMSource source = new DOMSource(document);
//        StreamResult result = new StreamResult("C:/Users/LukaBursic/Desktop/output.xml");
//        transformer.transform(source, result);

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        String xmlString = writer.getBuffer().toString();

        storeStringAsAscii("manifest", xmlString);

        // just to check
        stringToDom(xmlString, hdfDirectoryPath + "\\manifest_lab.xml");
    }

    public void createHdfFile() throws IOException, HDF5LibraryException {
//        System.out.print("Enter the path to the directory where you would like to store the HDF5 file: ");
//        hdfDirectoryPath = (new Scanner(System.in)).nextLine();
        if (!Files.exists(Path.of(hdfDirectoryPath))) throw new DirectoryNotFoundException("Directory with the given path doesn't exist");

        String hdfPath = hdfDirectoryPath + "\\laboratory_hsi.h5";

        Files.deleteIfExists(Paths.get(hdfPath));
        write_file_id = H5.H5Fcreate(hdfPath, HDF5Constants.H5F_ACC_TRUNC,
                HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
    }

    public void storeStringAsAscii(String datasetName, String str) throws HDF5Exception {
        long[] dimsStr = new long[]{str.length()};
        int str_dataspace_id = H5.H5Screate_simple(1, dimsStr, null);

        int str_dataset_id = H5.H5Dcreate(write_file_id, datasetName,
                HDF5Constants.H5T_NATIVE_CHAR, str_dataspace_id,
                HDF5Constants.H5P_DEFAULT);

        int memspace_id = H5.H5Screate_simple(1, dimsStr, null);
        H5.H5Dwrite(str_dataset_id, HDF5Constants.H5T_NATIVE_CHAR, memspace_id, str_dataspace_id, HDF5Constants.H5P_DEFAULT, str.getBytes(StandardCharsets.UTF_8));

        H5.H5Dclose(str_dataset_id);
    }

    public void stringToDom(String xmlSource, String filename)
            throws IOException {
        FileWriter fw = new FileWriter(filename);
        fw.write(xmlSource);
        fw.close();
    }

//    public void calculateAndStoreReflectance() throws IOException {
//        String rawPath = "D:/Faks/4. godina/Izborni projekt/example files/Laboratory-HSI/10-10-Aa_125823_sample_raw_hsi.mat";
//        String whitePath = "D:/Faks/4. godina/Izborni projekt/example files/Laboratory-HSI/10-10-Aa_125622_white_white.img";
//
//        long fileSize = Files.size(Paths.get(whitePath));
//        int numFloats = (int) (fileSize / Float.BYTES);
//
//        double[] whiteData;
//
//        byte[] fileContent = Files.readAllBytes(Paths.get(whitePath));
//        ByteBuffer buffer = ByteBuffer.wrap(fileContent);
//        buffer.order(ByteOrder.LITTLE_ENDIAN);  // Ensure the byte order matches the file's endianness
//
//        int numDoubles = fileContent.length / Double.BYTES;
//        whiteData = new double[numDoubles];
//        for (int i = 0; i < numDoubles; i++) {
//            whiteData[i] = buffer.getDouble();
//        }
//
//
//        double[] averagedWhiteData = averageData(whiteData);
//        HDFql.execute(String.format("CREATE DATASET reflectanceData AS FLOAT(%d, %d, %d)", NUM_POS_HSI, NUM_WAVELENGTHS, NUM_SAMPLES));
//        /*for (int i=0; i<NUM_SAMPLES; i++) {
//            System.out.println("i=" + i);
//            double[][] reflectanceChunk = new double[NUM_POS_HSI][NUM_WAVELENGTHS];
//
//            Mat5File matFile = Mat5.readFromFile(new File(String.format("D:\\Faks\\4. godina\\Izborni projekt\\example files\\Laboratory-HSI\\chunk_new\\acquired_chunk_%d.mat", (i+1))));
//
//            Matrix chunk = matFile.getMatrix("chunk");
//
//            if (chunk != null) {
//                System.out.println("Chunk dimensions: " + chunk.getNumRows() + "x" + chunk.getNumCols() + "x" + chunk.getNumElements() / (chunk.getNumRows() * chunk.getNumCols()));
//
//                StringBuilder query = new StringBuilder(String.format("INSERT INTO reflectanceData[0:1:%d:1,0:1:%d:1,%d:1:1:1] VALUES (", NUM_POS_HSI, NUM_WAVELENGTHS, i));
//
//                System.out.println("here1");
//                for (int j=0; j<NUM_POS_HSI; j++) {
//                    //System.out.println(j);
//                    //query.append("(");
//                    for (int k=0; k<NUM_WAVELENGTHS; k++) {
//                        // System.out.println(chunk.getInt(new int[]{0, k, j}));
//
//                        reflectanceChunk[j][k] = ((float)chunk.getInt(new int[]{0, k, j}))/averagedWhiteData[k];
//
//                        query.append(reflectanceChunk[j][k] + ", ");
//                        //query.append(j + ", ");
//                    }
//                    //if (j==NUM_POS_HSI-1) query.append(")");
//                    //else query.append("), ");
//                }
//                System.out.println("here2");
//
//                query.append(")");
//                //System.out.println(query);
//                HDFql.execute(query.toString());
//
//            } else {
//                System.out.println("Variable 'chunk' not found in the .mat file.");
//            }
//            //HDFql.execute("INSERT INTO reflectanceTest VALUES (1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0)");
//            //HDFql.execute("INSERT INTO reflectanceTest[0:1:3:1,0:1:2:1,2:1:1:1] VALUES (100.0, 100.0, 100.0, 100.0, 100.0, 100.0,)");
//        }
//         */
//
//        /////////////////////////////////////////////////////////////////////////////////////////////////
//
////        StringBuilder query = new StringBuilder(String.format("INSERT INTO reflectanceData[0:1:%d:1,0:1:%d:1,0:1:%d:1] VALUES (", NUM_POS_HSI, NUM_WAVELENGTHS, NUM_SAMPLES));
////        double[][][] testArray = new double[2][2][2];
////        testArray[0][0][0] = 1.0;
////        testArray[0][0][1] = 2.0;
////        testArray[0][1][0] = 3.0;
////        testArray[0][1][1] = 4.0;
////        testArray[1][0][0] = 5.0;
////        testArray[1][0][1] = 6.0;
////        testArray[1][1][0] = 7.0;
////        testArray[1][1][1] = 8.0;
////
////        for (int i=0; i<2; i++) {
////            for (int j=0; j<2; j++) {
////                for (int k=0; k<2; k++) {
////                    query.append(testArray[k][i][j] + ", ");
////                }
////            }
////        }
////        query.append(")");
////        System.out.println(query);
////
////        HDFql.execute(query.toString());
//
////        StringBuilder query = new StringBuilder(String.format("INSERT INTO reflectanceData[0:1:%d:1,0:1:%d:1,0:1:%d:1] VALUES (", NUM_POS_HSI, NUM_WAVELENGTHS, NUM_SAMPLES));
////
////        for (int sampIter=0; sampIter<NUM_SAMPLES; sampIter++) {
////            Mat5File matFile = Mat5.readFromFile(new File(String.format("D:\\Faks\\4. godina\\Izborni projekt\\example files\\Laboratory-HSI\\chunk_new\\acquired_chunk_%d.mat", (sampIter+1))));
////
////            Matrix chunk = matFile.getMatrix("chunk");
////            System.out.println("Chunk dimensions: " + chunk.getNumRows() + "x" + chunk.getNumCols() + "x" + chunk.getNumElements() / (chunk.getNumRows() * chunk.getNumCols()));
////
////            for (int posIter=0; posIter<NUM_POS_HSI; posIter++) {
////                for (int wavIter=0; wavIter<NUM_WAVELENGTHS; wavIter++) {
////
////                }
////            }
////        }
//    }

    private double[] averageData(double[] data) {
        double[][] reshapedWhiteData = new double[NUM_SAMPLES][NUM_WAVELENGTHS];
        for (int i = 0; i < NUM_SAMPLES; i++) {
            for (int j = 0; j < NUM_WAVELENGTHS; j++) {
                reshapedWhiteData[i][j] = data[i * NUM_WAVELENGTHS + j];
            }
        }

        double[] averagedWhiteData = new double[NUM_WAVELENGTHS];
        for (int j = 0; j < NUM_WAVELENGTHS; j++) {
            double sum = 0;
            for (int i = 0; i < NUM_SAMPLES; i++) {
                sum += reshapedWhiteData[i][j];
            }
            averagedWhiteData[j] = sum / NUM_SAMPLES;
        }

        return averagedWhiteData;
    }

    public void readAcquiredAndStoreReflectance() throws Exception {
        String datasetName = "/acquired";
        double[] averagedWhiteData = readAndAverageWhiteData();

        int read_dataset_id = -1;
        int read_dataspace_id = -1;

        read_dataset_id = H5.H5Dopen(read_file_id, datasetName);
        read_dataspace_id = H5.H5Dget_space(read_dataset_id);

        int reflectance_dataspace_id = -1;
        int reflectance_dataset_id = -1;

        int rank = H5.H5Sget_simple_extent_ndims(read_dataspace_id);
        long[] dims = new long[rank];
        H5.H5Sget_simple_extent_dims(read_dataspace_id, dims, null);

        long[] stride = null;  // Null means default to 1 (contiguous)
        long[] count = {1, 1, 1};  // Single block
        long[] block = {1, NUM_WAVELENGTHS, NUM_SAMPLES};  // Size of the block

        // Create a memory dataspace
        long[] memoryDims = {1, NUM_WAVELENGTHS, NUM_SAMPLES};
        int memspace_id = H5.H5Screate_simple(memoryDims.length, memoryDims, null);

        // Allocate space for the data
        short[] data = new short[(int) (memoryDims[0] * memoryDims[1] * memoryDims[2])];
        float[] refls = new float[(int) (memoryDims[0] * memoryDims[1] * memoryDims[2])];
        long[] dims3D = {NUM_POS_HSI, NUM_WAVELENGTHS, NUM_SAMPLES};

        write_group_id = H5.H5Gcreate(write_file_id, "g1",
                HDF5Constants.H5P_DEFAULT);
        reflectance_dataspace_id = H5.H5Screate_simple(3, dims3D, null);

        if ((write_group_id >= 0) && (reflectance_dataspace_id >= 0)) {
//            write_dataset_id = H5.H5Dcreate(write_group_id, "3D 64-bit double",
//                    HDF5Constants.H5T_NATIVE_DOUBLE, write_dataspace_id,
//                    HDF5Constants.H5P_DEFAULT);
            reflectance_dataset_id = H5.H5Dcreate(write_file_id, "reflectance",
                    HDF5Constants.H5T_NATIVE_FLOAT, reflectance_dataspace_id,
                    HDF5Constants.H5P_DEFAULT);
        }

        for (int i = 0; i < NUM_POS_HSI; i++) {
            long[] start = {i, 0, 0};  // Starting coordinates

            // reading
            H5.H5Sselect_hyperslab(read_dataspace_id, HDF5Constants.H5S_SELECT_SET, start, stride, count, block);
            H5.H5Dread(read_dataset_id, HDF5Constants.H5T_NATIVE_SHORT, memspace_id, read_dataspace_id, HDF5Constants.H5P_DEFAULT, data);

            int x = -1;
            int y = i;
            int z = -1;
            for (int j = 0; j < data.length; j++) {
                x = j / NUM_SAMPLES;
                z = j - x * NUM_SAMPLES;

                float reflectance = (float) (((double) data[j]) / averagedWhiteData[x]);
                refls[j] = reflectance;
            }

            // writing
            H5.H5Sselect_hyperslab(reflectance_dataspace_id, HDF5Constants.H5S_SELECT_SET, start, stride, count, block);
//            H5.H5Dwrite(write_dataset_id, HDF5Constants.H5T_NATIVE_DOUBLE, HDF5Constants.H5S_ALL,
//                    HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, refls);
            H5.H5Dwrite(reflectance_dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, memspace_id, reflectance_dataspace_id, HDF5Constants.H5P_DEFAULT, refls);
        }

        if (reflectance_dataset_id >= 0)
            H5.H5Dclose(reflectance_dataset_id);

        if (reflectance_dataspace_id >= 0)
            H5.H5Sclose(reflectance_dataspace_id);

        if (write_group_id >= 0)
            H5.H5Gclose(write_group_id);

        manifestFiles.add(new Triplet<>("reflectance", "reflectance", "?"));
    }

    public void readPositions(){

        String datasetName = "posHSI";
        int read_dataset_id = H5.H5Dopen(read_file_id, datasetName);
        int read_dataspace_id = H5.H5Dget_space(read_dataset_id);

        int rank = H5.H5Sget_simple_extent_ndims(read_dataspace_id);
        long[] dims = new long[rank];
        H5.H5Sget_simple_extent_dims(read_dataspace_id, dims, null);

//        System.out.println("DIMS");
//        for (long i:dims) {
//            System.out.print(i + " ");
//        }
//        System.out.println();

        int memspace_id = H5.H5Screate_simple(dims.length, dims, null);

        positions = new double[(int) (dims[0] * dims[1])];

        H5.H5Sselect_hyperslab(read_dataspace_id, HDF5Constants.H5S_SELECT_SET, new long[]{0, 0}, null, new long[]{1, 1}, dims);
        H5.H5Dread(read_dataset_id, HDF5Constants.H5T_NATIVE_DOUBLE, memspace_id, read_dataspace_id, HDF5Constants.H5P_DEFAULT, positions);
    }

    private double[] readAndAverageWhiteData() throws IOException {

        double[] whiteData;

        byte[] fileContent = Files.readAllBytes(Paths.get(whiteReferencePath));
        ByteBuffer buffer = ByteBuffer.wrap(fileContent);
        buffer.order(ByteOrder.LITTLE_ENDIAN);  // Ensure the byte order matches the file's endianness

        int numDoubles = fileContent.length / Double.BYTES;
        whiteData = new double[numDoubles];
        for (int i = 0; i < numDoubles; i++) {
            whiteData[i] = buffer.getDouble();
        }

        double[] averagedWhiteData = averageData(whiteData);
        return averagedWhiteData;
    }

    public void closeHdfFile() throws HDF5LibraryException {
        H5.H5Fclose(write_file_id);
    }
}

