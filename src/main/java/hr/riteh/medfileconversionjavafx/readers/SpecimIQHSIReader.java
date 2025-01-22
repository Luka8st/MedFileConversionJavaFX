package hr.riteh.medfileconversionjavafx.readers;

import hr.riteh.medfileconversionjavafx.helper.SpecimImageType;
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
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SpecimIQHSIReader {
    private String loadPath;
    private String hdfPath;
    private int readFileId = -1;
    private Map<String, String> manifestMap;
    private Map<String, Object> hdrMetadata;
    private Map<String, Object> resultsHdrMetadata;

    public SpecimIQHSIReader(String loadPath) {
        this.loadPath = loadPath;
        manifestMap = new HashMap<>();
        hdrMetadata = new HashMap<>();
        resultsHdrMetadata = new HashMap<>();
    }

    public String getLoadPath() {
        return loadPath;
    }

    public void run() throws ParserConfigurationException, IOException, SAXException {
        findHdfFile();
        openHdfFile();
        readManifest();

        readMetadata("hdrMetadata", hdrMetadata);
        readMetadata("resultsHdrMetadata", resultsHdrMetadata);

        SpecimImageType.NORMAL.setDimensions(new int[]{Integer.parseInt((String) hdrMetadata.get("lines")), Integer.parseInt((String) hdrMetadata.get("samples")), Integer.parseInt((String) hdrMetadata.get("bands"))});
        SpecimImageType.DARKREF.setDimensions(new int[]{1, Integer.parseInt((String) hdrMetadata.get("samples")), Integer.parseInt((String) hdrMetadata.get("bands"))});
        SpecimImageType.WHITEREF.setDimensions(new int[]{1, Integer.parseInt((String) hdrMetadata.get("samples")), Integer.parseInt((String) hdrMetadata.get("bands"))});
        SpecimImageType.WHITEDARKREF.setDimensions(new int[]{1, Integer.parseInt((String) hdrMetadata.get("samples")), Integer.parseInt((String) hdrMetadata.get("bands"))});

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

    public void readMetadata(String datasetName, Map<String, Object> dataMap) throws ParserConfigurationException, IOException, SAXException {
        System.out.println("Reading metadata for dataset: " + datasetName);
        for (Map.Entry<String, String> entry : manifestMap.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
        if (manifestMap.containsKey(datasetName)) {
            String metadataPath = manifestMap.get(datasetName);
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

            parseMetadataXML(metadataString, dataMap);
        }
        else {
            System.out.println("No metadata found in manifest");
        }
    }

    /*private void parseMetadataXML(String xmlContent, Map<String, Object> dataMap) throws ParserConfigurationException, IOException, SAXException {
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
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }

     */

    private void parseMetadataXML(String xmlContent, Map<String, Object> dataMap) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        ByteArrayInputStream input = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8));
        Document doc = builder.parse(input);
        doc.getDocumentElement().normalize();

        NodeList childNodes = doc.getDocumentElement().getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String tagName = element.getTagName();

                if ("wavelengths".equals(tagName)) {
                    String wavelengthsStr = element.getTextContent().trim();
                    double[] wavelengths = Arrays.stream(wavelengthsStr.split(","))
                            .map(String::trim)
                            .mapToDouble(Double::parseDouble)
                            .toArray();
                    dataMap.put("wavelengths", wavelengths);
                } else if ("key".equals(tagName)) {
                    String field = element.getAttribute("field");
                    String value = element.getTextContent().trim();
                    dataMap.put(field, value);
                }
            }
        }

        // Optional: Print the populated dataMap
        System.out.println("Data Map:");
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }

    public Map<String, Object> getHdrMetadata() {
        return hdrMetadata;
    }

    public Map<String, Object> getResultsHdrMetadata() {
        return resultsHdrMetadata;
    }
}
