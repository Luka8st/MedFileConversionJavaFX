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
import java.util.HashMap;
import java.util.Map;

public class MicroscopyHSIReader {
    private String loadPath;
    private String hdfPath;
    private String hdfDirectoryPath;
    private Map<String, String> manifestMap;
    private Map<String, String> darkMetadataMap;
    private Map<String, String> greenMetadataMap;
    private Map<String, String> whiteMetadataMap;
    int readFileId = -1;

    public MicroscopyHSIReader(String hdfPath) {
        this.hdfPath = hdfPath;
        manifestMap = new HashMap<>();
        darkMetadataMap = new HashMap<>();
        greenMetadataMap = new HashMap<>();
        whiteMetadataMap = new HashMap<>();
    }

    public Map<String, String> getDarkMetadataMap() {
        return darkMetadataMap;
    }

    public Map<String, String> getGreenMetadataMap() {
        return greenMetadataMap;
    }

    public Map<String, String> getWhiteMetadataMap() {
        return whiteMetadataMap;
    }

    public int getNumDarkImages() {
        return Integer.parseInt(darkMetadataMap.get("Image Count"));
    }

    public int getNumGreenImages() {
        return Integer.parseInt(greenMetadataMap.get("Image Count"));
    }

    public int getNumWhiteImages() {
        return Integer.parseInt(whiteMetadataMap.get("Image Count"));
    }

    public String getLoadPath() {
        return loadPath;
    }

    public String getHdfPath() {
        return hdfPath;
    }

    public String getHdfDirectoryPath() {
        return hdfDirectoryPath;
    }

    public void run() throws ParserConfigurationException, IOException, SAXException {
        //findHdfFile();
        System.out.println("Reading Laboratory HSI file: " + hdfPath);
        openHdfFile();
        readManifest();

        //readMetadata();
        readMetadata("darkMetadata", darkMetadataMap);
        readMetadata("greenMetadata", greenMetadataMap);
        readMetadata("whiteMetadata", whiteMetadataMap);

        closeHdfFile();
    }

    public void findHdfFile() {
        File folder = new File(loadPath);
        File[] files = folder.listFiles();

        assert files != null;
        for (File file : files) {
            if (file.getName().endsWith(".h5")) {
                hdfPath = file.getAbsolutePath();
                hdfDirectoryPath = file.getParent();
                System.out.println("Found HDF file: " + hdfPath);
                return;
            }
        }
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

    public void readMetadata(String datasetName, Map<String, String> metadataMap) throws ParserConfigurationException, IOException, SAXException {
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

            parseMetadataXML(metadataString, metadataMap);
        }
        else {
            System.out.println("No metadata found in manifest");
        }
    }

    private void parseMetadataXML(String xmlContent, Map<String, String> metadataMap) throws ParserConfigurationException, IOException, SAXException {
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
                System.out.println("Tag name: " + tagName + ", field: " + element.getAttribute("field") + ", value: " + element.getAttribute("field"));

                metadataMap.put(element.getAttribute("field"), element.getTextContent().trim());
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
}
