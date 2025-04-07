package hr.riteh.medfileconversionjavafx.converters;

import hr.riteh.medfileconversionjavafx.exceptions.DirectoryNotFoundException;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.ClassList;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.in.OMETiffReader;
import loci.formats.in.SlideBook6Reader;
import loci.formats.in.TiffReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MicroscopyHSIConverter {
    private String basePath;
    private String hdfDirectoryPath;

    public MicroscopyHSIConverter(String basePath, String hdfDirectoryPath) throws DirectoryNotFoundException {
        if (!Files.exists(Path.of(basePath)))
            throw new DirectoryNotFoundException("Directory with the given path doesn't exist");

        this.basePath = basePath;
        this.hdfDirectoryPath = hdfDirectoryPath;
    }
    
    public void run() throws IOException, ServiceException, DependencyException, FormatException {
        findAllFiles();
        createHdfFile();

        readOmeTifFile("D:\\Faks\\4. godina\\Izborni projekt\\example files\\Microscopy-HSI\\dark\\MMstack_Pos0.ome.tif");
    }

    public void readOmeTifFile(String filePath) throws IOException, FormatException, ServiceException, DependencyException {
        System.out.println("Reading OME-TIFF file: " + filePath);

        ClassList classList = new ClassList(OMETiffReader.class);

        // Initialize the Bio-Formats reader with the modified class list
        ImageReader reader = new ImageReader(classList);

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
    }

    private void createHdfFile() {
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
}
