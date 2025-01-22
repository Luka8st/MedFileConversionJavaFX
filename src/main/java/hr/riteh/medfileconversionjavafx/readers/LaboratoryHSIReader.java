package hr.riteh.medfileconversionjavafx.readers;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LaboratoryHSIReader {
    private String loadPath;
    private String hdfPath;
    private Map<String, String> dataMap;
    private Map<String, String> manifestMap;
    private double[] positions;
    private double[] wavelengths;

    int readFileId = -1;


    public LaboratoryHSIReader(String loadPath) {
        this.loadPath = loadPath;
        dataMap = new HashMap<>();
        manifestMap = new HashMap<>();
    }

    public String getHdfPath() {
        return hdfPath;
    }

    public String getLoadPath() {
        return loadPath;
    }

    public Map<String, String> getDataMap() {
        return dataMap;
    }

    public double[] getPositions() {
        return positions;
    }

    public double[] getWavelengths() {
        return wavelengths;
    }

    public void run() throws ParserConfigurationException, IOException, SAXException {
        findHdfFile();
        System.out.println("Reading Laboratory HSI file: " + hdfPath);
        openHdfFile();
        readManifest();
        readMetadata();


        closeHdfFile();
    }

    public void findHdfFile() {
        File folder = new File(loadPath);
        File[] files = folder.listFiles();

        assert files != null;
        for (File file : files) {
            if (file.getName().endsWith(".h5")) {
                hdfPath = file.getAbsolutePath();
                System.out.println("Found HDF file: " + hdfPath);
                return;
            }
        }
    }

    public void openHdfFile() {
        readFileId = H5.H5Fopen(hdfPath, HDF5Constants.H5F_ACC_RDONLY, HDF5Constants.H5P_DEFAULT);
        System.out.println("Opened HDF file: " + readFileId);
    }

    public void closeHdfFile() {
        H5.H5Fclose(readFileId);
    }

    public void readManifest() throws ParserConfigurationException, IOException, SAXException {
        int manifestId = H5.H5Dopen(readFileId, "/manifest", HDF5Constants.H5P_DEFAULT);
        int manifestSpaceId = H5.H5Dget_space(manifestId);

        long[] dims = new long[1];
        H5.H5Sget_simple_extent_dims(manifestSpaceId, dims, null);

        byte[] manifestData = new byte[(int) dims[0]];
        H5.H5Dread(manifestId, HDF5Constants.H5T_NATIVE_UCHAR, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, manifestData);

        String manifestString = new String(manifestData);
        String[] manifestLines = manifestString.split("\n");
        System.out.println("Manifest:");
        for (String line : manifestLines) {
            System.out.println(line);
        }

        parseManifestXML(manifestString);
    }

    private void parseManifestXML(String xmlContent) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        ByteArrayInputStream input = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8));
        Document doc = builder.parse(input);
        doc.getDocumentElement().normalize();

        NodeList fileNodes = doc.getElementsByTagName("file");

        for (int i = 0; i < fileNodes.getLength(); i++) {
            Node fileNode = fileNodes.item(i);
            if (fileNode.getNodeType() == Node.ELEMENT_NODE) {
                Element fileElement = (Element) fileNode;
                String type = fileElement.getAttribute("type");
                String extension = fileElement.getAttribute("extension");
                String content = fileElement.getTextContent().trim();

                System.out.println(type + " " + extension + " " + content);
                manifestMap.put(type, content);
            }
        }
    }

    public void readMetadata() throws ParserConfigurationException, IOException, SAXException {
        if (manifestMap.containsKey("metadata")) {
            String metadataPath = manifestMap.get("metadata");
            System.out.println("Reading metadata: " + metadataPath);

            int metadataId = H5.H5Dopen(readFileId, metadataPath, HDF5Constants.H5P_DEFAULT);
            int metadataSpaceId = H5.H5Dget_space(metadataId);

            long[] dims = new long[1];
            H5.H5Sget_simple_extent_dims(metadataSpaceId, dims, null);

            byte[] metadata = new byte[(int) dims[0]];
            H5.H5Dread(metadataId, HDF5Constants.H5T_NATIVE_UCHAR, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, metadata);

            String metadataString = new String(metadata);
            String[] metadataLines = metadataString.split("\n");
            System.out.println("Metadata:");
            for (String line : metadataLines) {
                System.out.println(line);
            }

            parseMetadataXML(metadataString);
        }
        else {
            System.out.println("No metadata found in manifest");
        }
    }

    private void parseMetadataXML(String xmlContent) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        ByteArrayInputStream input = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8));
        Document doc = builder.parse(input);
        doc.getDocumentElement().normalize();

        // Get all child nodes of <metadata>
        NodeList sections = doc.getDocumentElement().getChildNodes();

        for (int i = 0; i < sections.getLength(); i++) {
            Node section = sections.item(i);
            if (section.getNodeType() == Node.ELEMENT_NODE) {
                Element sectionElement = (Element) section;
                String sectionName = sectionElement.getTagName();

                NodeList keyNodes = sectionElement.getElementsByTagName("key");

                for (int j = 0; j < keyNodes.getLength(); j++) {
                    Node keyNode = keyNodes.item(j);
                    if (keyNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element keyElement = (Element) keyNode;
                        String field = keyElement.getAttribute("field");
                        String value = keyElement.getTextContent().trim();

                        // Create a composite key with section name
//                        String compositeKey = sectionName + "_" + field.replaceAll("\\s+", "_").toLowerCase();
//                        dataMap.put(compositeKey, value);

                        dataMap.put(field, value);
                    }
                }
            }
        }

        // Optional: Print the populated dataMap
        System.out.println("Data Map:");
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }

        List<String> wavelengthsStr = Arrays.stream(dataMap.get("wavelength").split(", ")).toList();
//        wavelengths = wavelengthsStr.stream()
//                .map(Double::valueOf)
//                .toArray(Double[]::new);

        wavelengths = wavelengthsStr.stream()
                .mapToDouble(Double::valueOf)
                .toArray();
    }
}
