package hr.riteh.medfileconversionjavafx.converters;

import ch.systemsx.cisd.hdf5.hdf5lib.H5D;
import hr.riteh.medfileconversionjavafx.exceptions.DirectoryNotFoundException;
import hr.riteh.medfileconversionjavafx.helper.Triplet;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.*;
import loci.formats.in.OMETiffReader;
import loci.formats.in.TiffReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MicroscopyHSIConverter {
    private String basePath;
    private String hdfDirectoryPath;
    private int write_file_id = -1;
    private Map<String, String> darkMetadataMap, greenMetadataMap, whiteMetadataMap;
    List<Triplet<String, String, String>> manifestFiles;
    private Class pixelType;

    public String getBasePath() {
        return basePath;
    }

    public String getHdfDirectoryPath() {
        return hdfDirectoryPath;
    }

    public Class getPixelType() {
        return pixelType;
    }

    public int getNumDarkImages() {
        return darkMetadataMap.get("Image Count") != null ? Integer.parseInt(darkMetadataMap.get("Image Count")) : 0;
    }

    public Map<String, String> getDarkMetadataMap() {
        return darkMetadataMap;
    }

    public int getNumGreenImages() {
        return greenMetadataMap.get("Image Count") != null ? Integer.parseInt(greenMetadataMap.get("Image Count")) : 0;
    }

    public Map<String, String> getGreenMetadataMap() {
        return greenMetadataMap;
    }

    public int getNumWhiteImages() {
        return whiteMetadataMap.get("Image Count") != null ? Integer.parseInt(whiteMetadataMap.get("Image Count")) : 0;
    }

    public Map<String, String> getWhiteMetadataMap() {
        return whiteMetadataMap;
    }

    public MicroscopyHSIConverter(String basePath, String hdfDirectoryPath) throws DirectoryNotFoundException {
        if (!Files.exists(Path.of(basePath)))
            throw new DirectoryNotFoundException("Directory with the given path doesn't exist");

        this.basePath = basePath;
        this.hdfDirectoryPath = hdfDirectoryPath;
        System.out.println("HDF Directory Path in converter: " + hdfDirectoryPath);
        darkMetadataMap = new HashMap<>();
        greenMetadataMap = new HashMap<>();
        whiteMetadataMap = new HashMap<>();
        manifestFiles = new ArrayList<Triplet<String, String, String>>();
    }
    
    public void run() throws IOException, ServiceException, DependencyException, FormatException, ParserConfigurationException, TransformerException {
        findAllFiles();
        checkHdfDirectory();
        createHdfFile();

        System.out.println("Reading dark file");
        readOmeTifFile(basePath + "\\dark\\MMstack_Pos0.ome.tif", darkMetadataMap, "dark");
        storeMetadataXml(darkMetadataMap, "darkMetadata");

        System.out.println("Reading white file");
        readOmeTifFile(basePath + "\\white\\MMstack_Pos0.ome.tif", whiteMetadataMap, "white");
        storeMetadataXml(whiteMetadataMap, "whiteMetadata");

        System.out.println("Reading green file");
        readOmeTifFile(basePath + "\\green\\MMstack_Pos0.ome.tif", greenMetadataMap, "green");
        storeMetadataXml(greenMetadataMap, "greenMetadata");

        storeManifestXml();
        closeHdfFile();
    }

    private void checkHdfDirectory() throws IOException {
        if (!isDirEmpty(Path.of(hdfDirectoryPath))) throw new DirectoryNotEmptyException("Directory with the given path is not empty");
    }

    private static boolean isDirEmpty(final Path directory) throws IOException {
        try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }

    /*public void readOmeTifFile(String filePath) throws IOException, FormatException, ServiceException, DependencyException {
        System.out.println("Reading OME-TIFF file: " + filePath);

        ClassList classList = new ClassList(OMETiffReader.class);

        // Initialize the Bio-Formats reader with the modified class list
        ImageReader reader = new ImageReader(classList);

        System.out.println("Reader used: " + reader.getReader(filePath).getClass().getName());

        // Initialize the metadata store
        ServiceFactory factory = new ServiceFactory();
        OMEXMLService service = factory.getInstance(OMEXMLService.class);
        IMetadata metadata = service.createOMEXMLMetadata();
        reader.setMetadataStore(metadata);

        // Open the OME-TIFF file
        reader.setId(filePath);

        // Read image dimensions
        int seriesCount = reader.getSeriesCount();
        System.out.println("Number of series: " + seriesCount);

        for (int series = 0; series < seriesCount; series++) {
            reader.setSeries(series);
            int width = reader.getSizeX();
            int height = reader.getSizeY();
            System.out.println("Series " + series + ": " + width + "x" + height);

            // Read the image as a BufferedImage
            // BufferedImage image = reader.openImage(0);
            System.out.println("Image read successfully for series " + series);
        }

        // Close the reader
        reader.close();
    }*/

    /*public void readOmeTifFile(String filePath) throws IOException, FormatException, ServiceException, DependencyException {
        System.out.println("Reading OME-TIFF file: " + filePath);

        // Explicitly include relevant readers in the ClassList
        ClassList<IFormatReader> classList = new ClassList<>(IFormatReader.class);
        classList.addClass(OMETiffReader.class);
        classList.addClass(TiffReader.class);

        // Initialize the Bio-Formats reader with the specified class list
        ImageReader reader = new ImageReader(classList);

        try {
            System.out.println("Reader used: " + reader.getReader(filePath).getClass().getName());

            // Initialize the metadata store
            ServiceFactory factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            IMetadata metadata = service.createOMEXMLMetadata();
            reader.setMetadataStore(metadata);

            // Open the file
            reader.setId(filePath);

            // Read image dimensions
            int seriesCount = reader.getSeriesCount();
            System.out.println("Number of series: " + seriesCount);

            for (int series = 0; series < seriesCount; series++) {
                reader.setSeries(series);
                int width = reader.getSizeX();
                int height = reader.getSizeY();
                int depth = reader.getSizeZ();
                System.out.println("Series " + series + ": " + width + "x" + height + "x" + depth);

                // Read the image as a BufferedImage
                // BufferedImage image = reader.openImage(0);
                System.out.println("Image read successfully for series " + series);
            }
        } catch (FormatException e) {
            System.err.println("File format not recognized: " + e.getMessage());
            throw e;
        } finally {
            // Close the reader
            reader.close();
        }
    }*/

    public void readOmeTifFile(String filePath, Map<String, String> metadataMap, String name) throws IOException, FormatException, DependencyException, ServiceException, ParserConfigurationException, TransformerException {
        ServiceFactory factory = new ServiceFactory();
        OMEXMLService service = factory.getInstance(OMEXMLService.class);
        IMetadata metadata = service.createOMEXMLMetadata();

        ClassList<IFormatReader> classList = new ClassList<>(IFormatReader.class);
        classList.addClass(OMETiffReader.class);
        classList.addClass(TiffReader.class);

        IFormatReader reader = new ImageReader(classList);
        reader.setMetadataStore(metadata);
        reader.setId(filePath); // Set the file path

        // Get image dimensions
        int width = reader.getSizeX(); // Get the image width
        int height = reader.getSizeY(); // Get the image height
        //System.out.println("Image size: " + width + " x " + height);

        // Optionally, you can fetch other dimensions if available (e.g., depth)
        int depth = reader.getSizeZ(); // Get the image depth (if it's a 3D image)
        //System.out.println("Image depth: " + depth);

        int numImages = metadata.getImageCount();
        //System.out.println("Number of images: " + numImages);

        //System.out.println("pixel type: " + metadata.getPixelsType(0));
        // Extract image names and channel information
        for (int i = 0; i < numImages; i++) {
            String imageName = metadata.getImageName(i);
            //System.out.println("Image " + i + ": " + imageName);

            int numChannels = metadata.getChannelCount(i);
            //System.out.println("Number of channels: " + numChannels);
            for (int j = 0; j < numChannels; j++) {
                String channelName = metadata.getChannelName(i, j);
                //System.out.println("Channel " + j + ": " + channelName);
            }
        }

        // Optionally, handle timepoints
        int numTimepoints = metadata.getPlaneCount(0); // If timepoints exist
        //System.out.println("Number of timepoints: " + numTimepoints);

        storeMetadata(metadataMap, metadata, reader);
        //storeMetadataXml(darkMetadataMap, "darkMetadata");

        switch (metadata.getPixelsType(0)) {
            case UINT8:
                System.out.println("Pixel type: UINT8");
                pixelType = byte.class;
                break;
            case UINT16:
                System.out.println("Pixel type: UINT16");
                pixelType = short.class;
                break;
            case FLOAT:
                System.out.println("Pixel type: FLOAT");
                pixelType = float.class;
                break;
            default:
                System.out.println("Unknown pixel type");
        }

        System.out.println("is little endian: " + reader.isLittleEndian());
        extractImageData(reader, name);

        // Optionally, close the reader after use
        reader.close();
    }

    private void storeMetadataXml(Map<String, String> metadataMap, String datasetName) throws ParserConfigurationException, TransformerException, IOException {
        // Create a DocumentBuilder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Create a new Document
        Document document = builder.newDocument();

        // Create root element
        Element root = document.createElement("metadata");
        document.appendChild(root);

        for (String name : metadataMap.keySet())
        {
            String value = metadataMap.get(name);

            Element sub = document.createElement("key");
            sub.appendChild(document.createTextNode(value));
            sub.setAttribute("field", name);

            root.appendChild(sub);
        }

        // Write to XML file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
//        DOMSource source = new DOMSource(document);

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        String xmlString = writer.getBuffer().toString();

        storeStringAsAscii(datasetName, xmlString);

        // just to check
        //stringToDom(xmlString, hdfDirectoryPath + "\\" + datasetName + ".xml");

//        manifestFiles.add(new Triplet<>(hdfDirectoryPath + "\\metadata.xml", "metadata", "xml"));
        manifestFiles.add(new Triplet<>(datasetName, datasetName, "xml"));
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
        //stringToDom(xmlString, hdfDirectoryPath + "\\manifest.xml");
    }

    private void storeMetadata(Map<String, String> metadataMap, IMetadata metadata, IFormatReader reader) {
        metadataMap.put("Width", String.valueOf(reader.getSizeX()));
        metadataMap.put("Height", String.valueOf(reader.getSizeY()));
        metadataMap.put("Depth", String.valueOf(reader.getSizeZ()));
        metadataMap.put("Pixel Type", String.valueOf(metadata.getPixelsType(0)));
        metadataMap.put("Number of Images", String.valueOf(metadata.getImageCount()));
        metadataMap.put("Number of Channels", String.valueOf(metadata.getChannelCount(0)));
        metadataMap.put("Number of Timepoints", String.valueOf(metadata.getPlaneCount(0)));
        metadataMap.put("Image Name", metadata.getImageName(0));
        metadataMap.put("Image Count", String.valueOf(reader.getImageCount()));

        System.out.println("Metadata stored: " + metadataMap);
    }

    private void extractImageData(IFormatReader reader, String name) throws IOException, FormatException {
        int tileWidth = 512;
        int tileHeight = 512;
        int imageWidth = reader.getSizeX();
        int imageHeight = reader.getSizeY();

        int imageCount = reader.getImageCount();
        //int imageCount = 1;

        //byte[] fullImage = new byte[imageWidth * imageHeight * imageCount]; // For 8-bit grayscale, adjust if needed
        System.out.println("pixel type: " + darkMetadataMap.get("Pixel Type"));

        /*for (int z = 0; z < imageCount; z++) {
            System.out.println("z = " + z);
            byte[] image = new byte[imageWidth * imageHeight];

            byte[] data = reader.openBytes(z, 0, 0, imageWidth, imageHeight);

            for (int ty = 0; ty < imageHeight; ty++) {
                System.arraycopy(data, ty * imageWidth, image, ty * imageWidth, imageWidth);
            }

        }*/

        long[] dims3D = {imageCount, imageHeight, imageWidth};

        int dataspace_id = H5.H5Screate_simple(3, dims3D, null);
        int dataset_id = H5.H5Dcreate(write_file_id, name, HDF5Constants.H5T_NATIVE_SHORT, dataspace_id,
                HDF5Constants.H5P_DEFAULT);
        long stride[] = null;
        long count[] = {1, 1, 1};
        long block[] = {1, imageHeight, imageWidth};

        boolean isLittleEndian = reader.isLittleEndian();

        long[] memoryDims = {1, imageHeight, imageWidth};
        int memspace_id = H5.H5Screate_simple(memoryDims.length, memoryDims, null);

        System.out.println("image count: " + imageCount);
        for (int z = 0; z < imageCount; z++) {
            System.out.println("z = " + z);
            long[] start = {z, 0, 0};

            // Allocate the appropriate array based on the type
            /*Object image;
            if (type == byte.class) {
                image = new byte[imageWidth * imageHeight];
            } else if (type == short.class) {
                image = new short[imageWidth * imageHeight];
            } else if (type == float.class) {
                image = new float[imageWidth * imageHeight];
            } else {
                throw new UnsupportedOperationException("Unsupported pixel type: " + type.getSimpleName());
            }*/
            short[] image = new short[imageWidth * imageHeight];

            // Read raw byte data for the entire image
            byte[] data = reader.openBytes(z, 0, 0, imageWidth, imageHeight);

            // Convert the byte array to the appropriate type
            /*if (type == byte.class) {
                System.arraycopy(data, 0, image, 0, data.length);
            } else if (type == short.class) {
                short[] shortImage = (short[]) image;
                for (int i = 0; i < shortImage.length; i++) {
                    shortImage[i] = (short) (((data[i * 2 + 1] & 0xFF) << 8) | (data[i * 2] & 0xFF));
                }

                H5.H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, stride, count, block);
                H5.H5Dwrite(dataset_id, HDF5Constants.H5T_NATIVE_SHORT, memspace_id, dataspace_id, HDF5Constants.H5P_DEFAULT, shortImage);
            } else if (type == float.class) {
                float[] floatImage = (float[]) image;
                for (int i = 0; i < floatImage.length; i++) {
                    int intBits = ((data[i * 4] & 0xFF) << 24) | ((data[i * 4 + 1] & 0xFF) << 16)
                            | ((data[i * 4 + 2] & 0xFF) << 8) | (data[i * 4 + 3] & 0xFF);
                    floatImage[i] = Float.intBitsToFloat(intBits);
                }
            }*/

            for (int i = 0; i < image.length; i++) {
                //image[i] = (short) (((data[i * 2 + 1] & 0xFF) << 8) | (data[i * 2] & 0xFF));

                if (isLittleEndian) {
                    // Little-endian: LSB comes first
                    image[i] = (short) (((data[i * 2] & 0xFF)) | ((data[i * 2 + 1] & 0xFF) << 8));
                } else {
                    // Big-endian: MSB comes first
                    image[i] = (short) (((data[i * 2 + 1] & 0xFF) << 8) | (data[i * 2] & 0xFF));
                }
            }

            H5.H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, stride, count, block);
            H5.H5Dwrite(dataset_id, HDF5Constants.H5T_NATIVE_SHORT, memspace_id, dataspace_id, HDF5Constants.H5P_DEFAULT, image);

            System.out.println("Finished processing image for z = " + z);
        }

        H5D.H5Dclose(dataset_id);
        H5.H5Sclose(dataspace_id);
        System.out.println("Finished reading image");

    }

    private int dataTypeToHdfType (Class<?> dataType) {
        if (dataType == byte.class) {
            return HDF5Constants.H5T_NATIVE_B8;
        } else if (dataType == short.class) {
            return HDF5Constants.H5T_NATIVE_SHORT;
        } else if (dataType == float.class) {
            return HDF5Constants.H5T_NATIVE_FLOAT;
        } else {
            throw new UnsupportedOperationException("Unsupported pixel type: " + dataType.getSimpleName());
        }
    }

    public void createHdfFile() throws IOException, HDF5LibraryException {
        if (!Files.exists(Path.of(hdfDirectoryPath))) throw new DirectoryNotFoundException("Directory with the given path doesn't exist");

        String hdfPath = hdfDirectoryPath + "\\microscopy_hsi.h5";

        Files.deleteIfExists(Paths.get(hdfPath));

        write_file_id = H5.H5Fcreate(hdfPath, HDF5Constants.H5F_ACC_TRUNC,
                HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);

        long[] dims = {"Microscopy-HSI".length()}; // Scalar attribute
        int dataspace_id = H5.H5Screate_simple(1, dims, null);

        // Create the attribute
        int attribute_id = H5.H5Acreate(write_file_id, "format",
                HDF5Constants.H5T_NATIVE_CHAR, dataspace_id,
                HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);

        // Write the attribute value
        H5.H5Awrite(attribute_id, HDF5Constants.H5T_NATIVE_CHAR, "Microscopy-HSI".getBytes(StandardCharsets.UTF_8));

        // Close the attribute and dataspace
        H5.H5Aclose(attribute_id);
        H5.H5Sclose(dataspace_id);

    }

    private void findAllFiles() throws IOException {
        Set<String> files;

        try (Stream<Path> paths = Files.walk(Path.of(basePath))) {
            files = paths
                    .filter(Files::isRegularFile) // Only include regular files
                    .filter(path -> !path.getFileName().toString().startsWith("._")) // Exclude files starting with "._"
                    .map(path -> Path.of(basePath).relativize(path).toString()) // Get file names
                    .collect(Collectors.toSet());
        }

        boolean containsWhite = false, containsDark = false, containsGreen = false;
        for (String s : files) {
            System.out.println("file: " + s);

            if (s.contains(".ome.tif")) {
                if (s.contains("white")) {
                    containsWhite = true;
                } else if (s.contains("dark")) {
                    containsDark = true;
                } else if (s.contains("green")) {
                    containsGreen = true;
                }
            }
        }

        if (!containsWhite) throw new FileNotFoundException("White file not found");
        if (!containsDark) throw new FileNotFoundException("Dark file not found");
        if (!containsGreen) throw new FileNotFoundException("Green file not found");
    }

    private void storeStringAsAscii(String datasetName, String str) throws HDF5Exception {
        long[] dimsStr = new long[]{str.length()};
        int str_dataspace_id = H5.H5Screate_simple(1, dimsStr, null);

        int str_dataset_id = H5. H5Dcreate(write_file_id, datasetName,
                HDF5Constants.H5T_NATIVE_CHAR, str_dataspace_id,
                HDF5Constants.H5P_DEFAULT);

        int memspace_id = H5.H5Screate_simple(1, dimsStr, null);
        H5.H5Dwrite(str_dataset_id, HDF5Constants.H5T_NATIVE_CHAR, memspace_id, str_dataspace_id, HDF5Constants.H5P_DEFAULT, str.getBytes(StandardCharsets.UTF_8));

        if (memspace_id >= 0) H5.H5Sclose(memspace_id);
        if (str_dataspace_id >= 0) H5.H5Sclose(str_dataspace_id);
        if (str_dataset_id >= 0) H5.H5Dclose(str_dataset_id);
    }

    private void stringToDom(String xmlSource, String filename)
            throws IOException {
        FileWriter fw = new FileWriter(filename);
        fw.write(xmlSource);
        fw.close();
    }

    private void closeHdfFile() throws HDF5LibraryException {
        H5.H5Fclose(write_file_id);
    }
}
