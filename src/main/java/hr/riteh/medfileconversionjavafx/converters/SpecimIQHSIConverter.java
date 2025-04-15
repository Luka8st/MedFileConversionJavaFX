package hr.riteh.medfileconversionjavafx.converters;

import hr.riteh.medfileconversionjavafx.exceptions.DirectoryNotFoundException;
import hr.riteh.medfileconversionjavafx.helper.SpecimImageType;
import hr.riteh.medfileconversionjavafx.helper.Triplet;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;

import javax.imageio.ImageIO;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class SpecimIQHSIConverter {

    private final String basePath;
    private final String hdfDirectoryPath;
    private int write_file_id = -1;
    private Map<String, Object> hdrMetadata = new HashMap<>();
    private String imageName;
    private int samples;
    private int lines;
    private int bands;
    private Map<String, Object> resultsHdrMetadata = new HashMap<>();
    List<Triplet<String, String, String>> manifestFiles; // (path, type, extension)


    public SpecimIQHSIConverter(String basePath, String hdfDirectoryPath) throws DirectoryNotFoundException {
        if (!Files.exists(Path.of(basePath)))
            throw new DirectoryNotFoundException("Directory with the given path doesn't exist");

        this.basePath = basePath;
        this.hdfDirectoryPath = hdfDirectoryPath;
        manifestFiles = new ArrayList<>();
    }

    public String getHdfDirectoryPath() {
        return hdfDirectoryPath;
    }

    public Map<String, Object> getResultsHdrMetadata() {
        return resultsHdrMetadata;
    }

    public Map<String, Object> getHdrMetadata() {
        return hdrMetadata;
    }

    public String getImageName() {
        return imageName;
    }

    public String getBasePath() {
        return basePath;
    }

    public void run() throws IOException, ParserConfigurationException, TransformerException {
        findImageName();
        findAllFiles();
        createHdfFile();
        storeMetadataXml();

        hdrMetadata = parseHdrFile(Path.of(basePath, "capture", imageName + ".hdr").toString());
        storeHdrMetadata("hdrMetadata", hdrMetadata);

        SpecimImageType.NORMAL.setDimensions(new int[]{Integer.parseInt((String) hdrMetadata.get("lines")), Integer.parseInt((String) hdrMetadata.get("samples")), Integer.parseInt((String) hdrMetadata.get("bands"))});
        SpecimImageType.DARKREF.setDimensions(new int[]{1, Integer.parseInt((String) hdrMetadata.get("samples")), Integer.parseInt((String) hdrMetadata.get("bands"))});
        SpecimImageType.WHITEREF.setDimensions(new int[]{1, Integer.parseInt((String) hdrMetadata.get("samples")), Integer.parseInt((String) hdrMetadata.get("bands"))});
        SpecimImageType.WHITEDARKREF.setDimensions(new int[]{1, Integer.parseInt((String) hdrMetadata.get("samples")), Integer.parseInt((String) hdrMetadata.get("bands"))});

        processImage(imageName, SpecimImageType.NORMAL);
        processImage("DARKREF_" + imageName, SpecimImageType.DARKREF);
        processImage("WHITEREF_" + imageName, SpecimImageType.WHITEREF);
        processImage("WHITEDARKREF_" + imageName, SpecimImageType.WHITEDARKREF);

        // results
        resultsHdrMetadata = parseHdrFile(Path.of(basePath, "results", "REFLECTANCE_" + imageName + ".hdr").toString());
        storeHdrMetadata("resultsHdrMetadata", resultsHdrMetadata);
        readPngFile(Path.of(basePath, "results", "REFLECTANCE_" + imageName + ".png").toString(), "reflectance");
        readPngFile(Path.of(basePath, "results", "RGBBACKGROUND_" + imageName + ".png").toString(), "rgbBackground");
        readPngFile(Path.of(basePath, "results", "RGBSCENE_" + imageName + ".png").toString(), "rgbScene");
        readPngFile(Path.of(basePath, "results", "RGBVIEWFINDER_" + imageName + ".png").toString(), "rgbViewfinder");

        storeManifestXml();
        closeHdfFile();
    }

    private void findImageName() throws IOException {
        Optional<Path> imagePath = Files.list(Path.of(basePath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".png"))
                .filter(path -> !path.getFileName().toString().startsWith("._"))
                .findFirst();

        if (imagePath.isPresent()) {
            String fileName = imagePath.get().getFileName().toString();
            String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));

            imageName = fileNameWithoutExtension;
        }
    }

    private void findAllFiles() throws IOException {
        Set<String> captureFiles = Files.walk(Path.of(basePath))
                .filter(path -> Files.isRegularFile(path)) // Check if it's a regular file
                .filter(path -> !path.getFileName().toString().startsWith("._")) // Exclude files starting with "._"
                .filter(path -> path.getParent() != null && path.getParent().getFileName().toString().equals("capture")) // Check if parent folder is "capture"
                .map(path -> path.getFileName().toString()) // Map to file name
                .collect(Collectors.toSet());

        System.out.println("files: " + captureFiles);

        // Define required patterns and their flags
        boolean containsDarkRefHdr = captureFiles.stream().anyMatch(s -> s.matches("DARKREF.*\\.hdr"));
        boolean containsDarkRefRaw = captureFiles.stream().anyMatch(s -> s.matches("DARKREF.*\\.raw"));
        boolean containsWhiteDarkRefHdr = captureFiles.stream().anyMatch(s -> s.matches("WHITEDARKREF.*\\.hdr"));
        boolean containsWhiteDarkRefRaw = captureFiles.stream().anyMatch(s -> s.matches("WHITEDARKREF.*\\.raw"));
        boolean containsWhiteRefHdr = captureFiles.stream().anyMatch(s -> s.matches("WHITEREF.*\\.hdr"));
        boolean containsWhiteRefRaw = captureFiles.stream().anyMatch(s -> s.matches("WHITEREF.*\\.raw"));
        boolean containsHdr = captureFiles.stream()
                .anyMatch(s -> s.matches(".*\\.hdr") &&
                        !s.matches("DARKREF.*\\.hdr") &&
                        !s.matches("DARKWHITEREF.*\\.hdr") &&
                        !s.matches("WHITEREF.*\\.hdr"));
        boolean containsRaw = captureFiles.stream()
                .anyMatch(s -> s.matches(".*\\.raw") &&
                        !s.matches("DARKREF.*\\.raw") &&
                        !s.matches("DARKWHITEREF.*\\.raw") &&
                        !s.matches("WHITEREF.*\\.raw"));

        // Collect missing file types
        StringBuilder missingFiles = new StringBuilder();
        if (!containsDarkRefHdr) missingFiles.append("Dark reference header file is not found\n");
        if (!containsDarkRefRaw) missingFiles.append("Dark reference raw file is not found\n");
        if (!containsWhiteDarkRefHdr) missingFiles.append("White dark reference header file is not found\n");
        if (!containsWhiteDarkRefRaw) missingFiles.append("White dark reference raw file is not found\n");
        if (!containsWhiteRefHdr) missingFiles.append("White reference header file is not found\n");
        if (!containsWhiteRefRaw) missingFiles.append("White reference raw file is not found\n");
        if (!containsHdr) missingFiles.append("Header file is not found\n");
        if (!containsRaw) missingFiles.append("Raw file is not found\n");

        // Throw an exception if any files are missing
        if (!missingFiles.isEmpty()) {
            throw new FileNotFoundException(missingFiles.toString());
        }

        System.out.println("All required files are present!");
    }

    private void createHdfFile() throws IOException, HDF5LibraryException {
        if (!Files.exists(Path.of(hdfDirectoryPath)))
            throw new DirectoryNotFoundException("Directory with the given path doesn't exist");

        String hdfPath = hdfDirectoryPath + "\\specimiq_hsi.h5";

        Files.deleteIfExists(Paths.get(hdfPath));
        write_file_id = H5.H5Fcreate(hdfPath, HDF5Constants.H5F_ACC_TRUNC,
                HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
    }

    private void storeMetadataXml() {
        try {
            // Get the first file in the metadata directory
            Path metadataDir = Path.of(basePath, "metadata");
            Optional<Path> metadataFilePath = Files.list(metadataDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".xml"))
                    .findFirst();

            if (metadataFilePath.isPresent()) {
                String metadataXml = Files.readString(metadataFilePath.get());
                storeStringAsAscii("metadata", metadataXml);
                stringToDom(metadataXml, hdfDirectoryPath + "\\metadata_specim.xml");

                manifestFiles.add(new Triplet<>("metadata", "metadata", "xml"));
            } else {
                System.err.println("No metadata file found in the directory: " + metadataDir);
            }
        } catch (IOException | HDF5Exception e) {
            e.printStackTrace();
        }
    }

    private void storeHdrMetadata(String filename, Map<String, Object> metadataMap) throws HDF5Exception, IOException, ParserConfigurationException, TransformerException {
        // Create a DocumentBuilder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Create a new Document
        Document document = builder.newDocument();

        // Create root element
        Element root = document.createElement("metadata");
        document.appendChild(root);

        for (Map.Entry<String, Object> entry : metadataMap.entrySet()) {
            if (entry.getKey().equals("wavelength")) {
                Element wavelengthsElement = document.createElement("wavelengths");
                String[] wavelengths = (String[]) entry.getValue();
                StringBuilder wavelengthsStr = new StringBuilder();
                for (String wavelength : wavelengths) {
                    wavelengthsStr.append(wavelength).append(",");
                }
                // Remove the trailing comma
                if (wavelengthsStr.length() > 0) {
                    wavelengthsStr.setLength(wavelengthsStr.length() - 1);
                }
                wavelengthsElement.appendChild(document.createTextNode(wavelengthsStr.toString()));
                root.appendChild(wavelengthsElement);
            } else {
                Element sub = document.createElement("key");
                sub.appendChild(document.createTextNode(entry.getValue().toString()));
                sub.setAttribute("field", entry.getKey());
                root.appendChild(sub);
            }
        }

        // Write to XML file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);

        StringWriter writer = new StringWriter();
        transformer.transform(source, new StreamResult(writer));
        String xmlString = writer.getBuffer().toString();

        storeStringAsAscii(filename, xmlString);

        // Just to check
        stringToDom(xmlString, hdfDirectoryPath + "\\" + filename + ".xml");

        //manifestFiles.add(new Triplet<>(filename, "metadata", "xml"));
        manifestFiles.add(new Triplet<>(filename, filename, "xml"));
    }

//    private void processImage(String name, SpecimImageType imageType) throws IOException, ParserConfigurationException, TransformerException {
//        if (imageType.equals(SpecimImageType.NORMAL)) {
//            hdrMetadata = parseHdrFile(Path.of(basePath, "capture", name + ".hdr").toString());
//            storeHdrMetadata("hdrMetadata", hdrMetadata);
//
//            SpecimImageType.NORMAL.setDimensions(new int[]{Integer.parseInt((String) hdrMetadata.get("lines")), Integer.parseInt((String) hdrMetadata.get("samples")), Integer.parseInt((String) hdrMetadata.get("bands"))});
//            SpecimImageType.DARKREF.setDimensions(new int[]{1, Integer.parseInt((String) hdrMetadata.get("samples")), Integer.parseInt((String) hdrMetadata.get("bands"))});
//            SpecimImageType.WHITEREF.setDimensions(new int[]{1, Integer.parseInt((String) hdrMetadata.get("samples")), Integer.parseInt((String) hdrMetadata.get("bands"))});
//            SpecimImageType.WHITEDARKREF.setDimensions(new int[]{1, Integer.parseInt((String) hdrMetadata.get("samples")), Integer.parseInt((String) hdrMetadata.get("bands"))});
//        }
//        readRawFile(Path.of(basePath, "capture", name + ".raw").toString(), imageType);
//    }

    private void processImage(String name, SpecimImageType imageType) throws IOException, ParserConfigurationException, TransformerException {
        readRawFile(Path.of(basePath, "capture", name + ".raw").toString(), imageType);
    }

//    private Map<String, Object> parseHdrFile(String hdrFilePath) throws IOException, ParserConfigurationException, TransformerException {
//        Map<String, Object> metadata = new HashMap<>();
//        try (BufferedReader br = new BufferedReader(new FileReader(hdrFilePath))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                if (line.startsWith("wavelength")) {
//                    StringBuilder wavelengths = new StringBuilder();
//                    while ((line = br.readLine()) != null && !line.trim().equals("}")) {
//                        wavelengths.append(line.trim()).append(" ");
//                    }
//                    metadata.put("wavelength", wavelengths.toString().trim().split(","));
//                }
//                else if(line.split("=").length == 2) {
//                    metadata.put(line.split("=")[0].trim(), line.split("=")[1].trim());
//                }
//            }
//        }
//
////        System.out.println("Metadata:");
////        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
////            System.out.println(entry.getKey() + ": " + entry.getValue());
////        }
//
//        return metadata;
//        }


    private Map<String, Object> parseHdrFile(String hdrFilePath) throws IOException {
        Map<String, Object> metadata = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(hdrFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("{")) {
                    String key = line.split("=")[0].trim();
                    StringBuilder valueBuilder = new StringBuilder();
                    if (line.contains("}")) {
                        valueBuilder.append(line.split("=")[1].trim().replace("{", "").replace("}", "").replace("\n", "").trim());
                    } else {
                        valueBuilder.append(line.split("=")[1].trim().replace("{", "").replace("\n", "").trim());
                        while (!(line = br.readLine()).contains("}")) {
                            valueBuilder.append(line.trim().replace("\n", "").trim());
                        }
                        valueBuilder.append(line.trim().replace("}", "").replace("\n", "").trim());
                    }

                    if (key.equals("wavelength")) {
                        metadata.put(key, valueBuilder.toString().split(","));
                    } else {
                        metadata.put(key, new String(valueBuilder.toString().getBytes(), StandardCharsets.UTF_8));
                    }
                } else if (line.split("=").length == 2) {
                    metadata.put(line.split("=")[0].trim(), line.split("=")[1].trim());
                }
            }
        }

        System.out.println("Metadata:");
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        return metadata;
    }

    private void readRawFile(String rawFilePath, SpecimImageType imageType) throws IOException {
//        samples = Integer.parseInt((String) hdrMetadata.get("samples"));
//        lines = (!imageType.equals(SpecimImageType.NORMAL) ? 1 : Integer.parseInt((String) hdrMetadata.get("lines")));
//        bands = Integer.parseInt((String) hdrMetadata.get("bands"));

        lines = imageType.getDimensions()[0];
        samples = imageType.getDimensions()[1];
        bands = imageType.getDimensions()[2];

        int dataType = Integer.parseInt((String) hdrMetadata.get("data type"));
        String interleave = ((String) hdrMetadata.get("interleave")).toLowerCase();
        int byteOrder = Integer.parseInt((String) hdrMetadata.get("byte order"));

        short[][][] rawData = new short[lines][samples][bands];

        try (DataInputStream dis = new DataInputStream(new FileInputStream(rawFilePath))) {

            ByteBuffer buffer = ByteBuffer.allocate(samples * lines * bands * 2); // Assuming 16-bit data
            buffer.order(byteOrder == 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
            dis.readFully(buffer.array());

            switch (interleave) {
                case "bil" -> {
                    for (int line = 0; line < lines; line++) {
                        for (int band = 0; band < bands; band++) {
                            for (int sample = 0; sample < samples; sample++) {
                                rawData[line][sample][band] = buffer.getShort();
                            }
                        }
                    }
                }
                case "bip" -> {
                    for (int line = 0; line < lines; line++) {
                        for (int sample = 0; sample < samples; sample++) {
                            for (int band = 0; band < bands; band++) {
                                rawData[line][sample][band] = buffer.getShort();
                            }
                        }
                    }
                }
                case "bsq" -> {
                    for (int band = 0; band < bands; band++) {
                        for (int line = 0; line < lines; line++) {
                            for (int sample = 0; sample < samples; sample++) {
                                rawData[line][sample][band] = buffer.getShort();
                            }
                        }
                    }
                }
            }

            Path path = Paths.get(rawFilePath);
            String fileName = path.getFileName().toString();
//            String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
            // storeData(rawData, "rawData " + fileNameWithoutExtension);

//            storeData(rawData, imageType.getDatasetName() + "_" + fileNameWithoutExtension);
            storeData(rawData, imageType.getDatasetName());


            // ovo pustit
            // if (imageType.equals(SpecimImageType.NORMAL)) generateRgbImage(rawData, samples, lines);
        }
    }

    private void readPngFile(String pngFilePath, String datasetName) throws IOException {
        BufferedImage img = ImageIO.read(new File(pngFilePath));
        Raster raster = img.getData();
        System.out.println("Raster: " + raster.getWidth() + " " + raster.getHeight() + " " + raster.getNumBands() + " " + raster.getSampleModel().getDataType());

        System.out.println(raster.getSampleFloat(0, 0, 0));

        float[][][] rawData = new float[raster.getHeight()][raster.getWidth()][raster.getNumBands()];
        for (int line = 0; line < raster.getHeight(); line++) {
            for (int sample = 0; sample < raster.getWidth(); sample++) {
                for (int band = 0; band < raster.getNumBands(); band++) {
                    rawData[line][sample][band] = raster.getSampleFloat(sample, line, band);
                }
            }
        }

        storePngData(rawData, datasetName);

        // ovo je za provjeru
        //        // Create an image from rawData
//        BufferedImage outputImage = new BufferedImage(raster.getWidth(), raster.getHeight(), BufferedImage.TYPE_INT_RGB);
//        for (int line = 0; line < raster.getHeight(); line++) {
//            for (int sample = 0; sample < raster.getWidth(); sample++) {
//                int r = (int) rawData[line][sample][0];
//                int g = (int) rawData[line][sample][1];
//                int b = (int) rawData[line][sample][2];
//                int rgb = (r << 16) | (g << 8) | b;
//                outputImage.setRGB(sample, line, rgb);
//            }
//        }
//
//        // Save the created image to a file
//        File outputFile = new File("outputImage.png");
//        ImageIO.write(outputImage, "png", outputFile);
//        System.out.println("Image created from rawData and saved as outputImage.png");
    }

    private void storeFirstSliceAsImage(short[][][] rawData) throws IOException {
        int width = samples;
        int height = bands; // Use bands as height to iterate through samples and bands
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int band = 0; band < bands; band++) {
            for (int sample = 0; sample < samples; sample++) {
                int value = rawData[0][sample][band]; // Use the first line for simplicity
                int rgb = value << 16 | value << 8 | value; // Convert to grayscale (R=G=B)
                img.setRGB(sample, band, rgb); // Set the pixel value
            }
        }

        File outputFile = new File("rawImage.png");
        ImageIO.write(img, "png", outputFile);
        System.out.println("First slice stored as rawImage.png");
    }

    private void generateRgbImage(short[][][] data, int samples, int lines) {
        // Default bands for RGB (example: red=70, green=53, blue=19)
        int redBand = 70;
        int greenBand = 53;
        int blueBand = 19;

        // Create an RGB image
        BufferedImage image = new BufferedImage(samples, lines, BufferedImage.TYPE_INT_RGB);
        for (int line = 0; line < lines; line++) {
            for (int sample = 0; sample < samples; sample++) {
                int red = data[line][sample][redBand];
                int green = data[line][sample][greenBand];
                int blue = data[line][sample][blueBand];
                int rgb = (red << 16) | (green << 8) | blue;
                image.setRGB(sample, line, rgb);
            }
        }

        File outputFile = new File("rgbImage.png");
        try {
            ImageIO.write(image, "png", outputFile);
            System.out.println("RGB image stored as rgbImage.png");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    private void storeData(int[][][] data) {
        System.out.println("Storing data to HDF5 file...");
        long[] dims3D = {lines, samples, bands};
        long[] stride = null;  // Null means default to 1 (contiguous)
        long[] count = {1, 1, 1};  // Single block
        long[] block = {1, samples, bands};

        long[] memoryDims = {1, samples, bands};
        int memspace_id = H5.H5Screate_simple(memoryDims.length, memoryDims, null);


        int dataspace_id = H5.H5Screate_simple(3, dims3D, null);
        int dataset_id = H5.H5Dcreate(write_file_id, "reflectance",
                HDF5Constants.H5T_NATIVE_INT16, dataspace_id,
                HDF5Constants.H5P_DEFAULT);

        for (int i=0; i<lines; i++) {
            System.out.println("Storing line " + i + "...");
            long[] start = {i, 0, 0};
            H5.H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, stride, count, block);
            H5.H5Dwrite(dataset_id, HDF5Constants.H5T_NATIVE_INT16, memspace_id, dataspace_id, HDF5Constants.H5P_DEFAULT, data[i]);
        }

        if (dataset_id >= 0)
            H5.H5Dclose(dataset_id);

        if (dataspace_id >= 0)
            H5.H5Sclose( dataspace_id);
        System.out.println("Data stored successfully!");
    }*/

    private void storeData(short[][][] rawData, String datasetName) { // ovo bi se trebalo moci koristiti za sve raw datoteke
        System.out.println("Storing data to HDF5 file...");
        System.out.println("Dataset name: " + datasetName);
        lines = rawData.length;
        samples = rawData[0].length;
        bands = rawData[0][0].length;

        long[] dims3D = {lines, samples, bands};
        long[] memoryDims = {1, samples, bands};
        long[] block = {1, samples, bands};
        long[] count = {1, 1, 1};
        int memspace_id = H5.H5Screate_simple(memoryDims.length, memoryDims, null);

        int dataspace_id = H5.H5Screate_simple(3, dims3D, null);
        int dataset_id = H5.H5Dcreate(write_file_id, datasetName,
                HDF5Constants.H5T_NATIVE_SHORT, dataspace_id,
                HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);

        try {
            for (int i = 0; i < lines; i++) {
//                System.out.println("Storing line " + i + "...");
//                System.out.println("Data: " + data[i].length + " " + data[i][0].length + " " + data[i][0][0]);
                long[] start = {i, 0, 0};

                H5.H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, null, count, block);
                H5.H5Dwrite(dataset_id, HDF5Constants.H5T_NATIVE_SHORT, memspace_id, dataspace_id, HDF5Constants.H5P_DEFAULT, rawData[i]);
            }
        } catch (HDF5Exception e) {
            e.printStackTrace();
        } finally {
            if (dataset_id >= 0) {
                try {
                    H5.H5Dclose(dataset_id);
                } catch (HDF5Exception e) {
                    e.printStackTrace();
                }
            }

            if (dataspace_id >= 0) {
                try {
                    H5.H5Sclose(dataspace_id);
                } catch (HDF5Exception e) {
                    e.printStackTrace();
                }
            }

            if (memspace_id >= 0) {
                try {
                    H5.H5Sclose(memspace_id);
                } catch (HDF5Exception e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Data stored successfully!");
        //manifestFiles.add(new Triplet<>(datasetName, "raw", "h5"));
        manifestFiles.add(new Triplet<>(datasetName, datasetName, "h5"));
    }

    private void storePngData(float[][][] rawData, String datasetName) { // ovo bi se trebalo moci koristiti za sve png datoteke
        System.out.println("Storing data to HDF5 file...");
        System.out.println("Dataset name: " + datasetName);
        lines = rawData.length;
        samples = rawData[0].length;
        bands = rawData[0][0].length;

        long[] dims3D = {lines, samples, bands};
        long[] memoryDims = {1, samples, bands};
        long[] block = {1, samples, bands};
        long[] count = {1, 1, 1};
        int memspace_id = H5.H5Screate_simple(memoryDims.length, memoryDims, null);

        int dataspace_id = H5.H5Screate_simple(3, dims3D, null);
        int dataset_id = H5.H5Dcreate(write_file_id, datasetName,
                HDF5Constants.H5T_NATIVE_FLOAT, dataspace_id,
                HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);

        try {
            for (int i = 0; i < lines; i++) {
//                System.out.println("Storing line " + i + "...");
//                System.out.println("Data: " + data[i].length + " " + data[i][0].length + " " + data[i][0][0]);
                long[] start = {i, 0, 0};

                H5.H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, null, count, block);
                H5.H5Dwrite(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, memspace_id, dataspace_id, HDF5Constants.H5P_DEFAULT, rawData[i]);
            }
        } catch (HDF5Exception e) {
            e.printStackTrace();
        } finally {
            if (dataset_id >= 0) {
                try {
                    H5.H5Dclose(dataset_id);
                } catch (HDF5Exception e) {
                    e.printStackTrace();
                }
            }

            if (dataspace_id >= 0) {
                try {
                    H5.H5Sclose(dataspace_id);
                } catch (HDF5Exception e) {
                    e.printStackTrace();
                }
            }

            if (memspace_id >= 0) {
                try {
                    H5.H5Sclose(memspace_id);
                } catch (HDF5Exception e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Data stored successfully!");
        //manifestFiles.add(new Triplet<>(datasetName, "png", "h5"));
        manifestFiles.add(new Triplet<>(datasetName, datasetName, "h5"));
    }

    public void closeHdfFile() throws HDF5LibraryException {
        H5.H5Fclose(write_file_id);
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
        stringToDom(xmlString, hdfDirectoryPath + "\\manifest_specim.xml");
    }

    private void storeStringAsAscii (String datasetName, String str) throws HDF5Exception {
        long[] dimsStr = new long[]{str.length()};
        int str_dataspace_id = H5.H5Screate_simple(1, dimsStr, null);

        System.out.println("Dataset name: " + datasetName);
//        int str_dataset_id = H5.H5Dcreate(write_file_id, datasetName,
//                HDF5Constants.H5T_NATIVE_CHAR, str_dataspace_id,
//                HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
//////////////////////
        int str_dataset_id = -1;
        try {
            str_dataset_id = H5.H5Dopen(write_file_id, datasetName, HDF5Constants.H5P_DEFAULT);
            if (str_dataset_id >= 0) {
                // Dataset exists, delete it
                H5.H5Dclose(str_dataset_id);
                H5.H5Ldelete(write_file_id, datasetName, HDF5Constants.H5P_DEFAULT);
                System.out.println("Deleted existing dataset: " + datasetName);
            }
        } catch (HDF5Exception e) {
            // Dataset does not exist, continue
        }

        str_dataset_id = H5.H5Dcreate(write_file_id, datasetName,
                HDF5Constants.H5T_NATIVE_CHAR, str_dataspace_id,
                HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
////////////
        int memspace_id = H5.H5Screate_simple(1, dimsStr, null);
        System.out.println("Dataset id: " + str_dataset_id);
        H5.H5Dwrite(str_dataset_id, HDF5Constants.H5T_NATIVE_CHAR, memspace_id, str_dataspace_id, HDF5Constants.H5P_DEFAULT, str.getBytes(StandardCharsets.UTF_8));

        H5.H5Dclose(str_dataset_id);
    }

    private void stringToDom(String xmlSource, String filename)
            throws IOException {
        FileWriter fw = new FileWriter(filename);
        fw.write(xmlSource);
        fw.close();
    }
}

