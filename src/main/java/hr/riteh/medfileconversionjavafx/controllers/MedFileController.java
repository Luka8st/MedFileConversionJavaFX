package hr.riteh.medfileconversionjavafx.controllers;

import hr.riteh.medfileconversionjavafx.MedFileApplication;
import hr.riteh.medfileconversionjavafx.constants.SceneConstants;
import hr.riteh.medfileconversionjavafx.converters.LaboratoryHSIConverter;
import hr.riteh.medfileconversionjavafx.converters.MicroscopyHSIConverter;
import hr.riteh.medfileconversionjavafx.converters.SpecimIQHSIConverter;
import hr.riteh.medfileconversionjavafx.displayers.LaboratoryHSIDisplayer;
import hr.riteh.medfileconversionjavafx.displayers.MicroscopyHSIDisplayer;
import hr.riteh.medfileconversionjavafx.displayers.SpecimIQHSIDisplayer;
import hr.riteh.medfileconversionjavafx.exceptions.DirectoryNotFoundException;
import hr.riteh.medfileconversionjavafx.readers.LaboratoryHSIReader;
import hr.riteh.medfileconversionjavafx.readers.MicroscopyHSIReader;
import hr.riteh.medfileconversionjavafx.readers.SpecimIQHSIReader;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MedFileController {
    private static final String LABHSI_HEADER_FILENAME_TEMPLATE = "_white_white.hdr";
    private static final String LABHSI_RAW_DATA_FILENAME_TEMPLATE = "_sample_raw_hsi.mat";
    private static final String LABHSI_WHITE_REFERENCE_FILENAME_TEMPLATE  = "_white_white.img";

    private File loadDirectory;
    private File storeDirectory;
    private boolean loadDirectorySet = false;
    private boolean storeDirectorySet = false;
    private Stage stage;

    private boolean displayFileSet = false;
    private File displayFile;

    @FXML
    private HBox mainHBox;
    @FXML
    private VBox leftVBox;
    @FXML
    private VBox rightVBox;
    @FXML
    private DirectoryChooser loadDirectoryChooser = new DirectoryChooser();
    @FXML
    private DirectoryChooser storeDirectoryChooser = new DirectoryChooser();
    @FXML
    private Label loadDirectoryLabel;
    @FXML
    private Label storeDirectoryLabel;
    @FXML
    private Button loadBtn;
    @FXML
    private Button selectLoadDirectoryBtn;
    @FXML
    private Button selectStoreDirectoryBtn;

    @FXML 
    private Separator verticalSeparator;

    @FXML
    private DirectoryChooser displayDirectoryChooser = new DirectoryChooser();

    @FXML
    private FileChooser displayFileChooser = new FileChooser();

    @FXML
    private Label displayDirectoryLabel;

    @FXML
    private Label displayFileLabel;

    @FXML
    private Button displayBtn;
    @FXML
    private Button selectDisplayFileBtn;

    @FXML
    public void initialize() {
        // Set the default value for imageFormatBox
        // verticalSeparator.prefHeightProperty().bind(mainHBox.heightProperty().multiply(0.5));

        leftVBox.setPrefWidth(0.3*SceneConstants.SCENE_WIDTH);
        rightVBox.setPrefWidth(0.3*SceneConstants.SCENE_WIDTH);

        loadDirectoryLabel.setText("Selected: " + (MedFileApplication.getLoadDirectory() != null ? MedFileApplication.getLoadDirectory() : "None"));
        if (MedFileApplication.getLoadDirectory() != null) {
            loadDirectorySet = true;
            loadDirectory = new File(MedFileApplication.getLoadDirectory());
        }

        displayFileLabel.setText("Selected: " + (MedFileApplication.getDisplayFile() != null ? MedFileApplication.getDisplayFile() : "None"));
        if (MedFileApplication.getDisplayFile() != null) {
            displayFileSet = true;
            displayBtn.setDisable(false);
            displayFile = new File(MedFileApplication.getDisplayFile());
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void onSelectLoadDirectoryClick() {
        loadDirectoryChooser.setTitle("Select a Directory");
        loadDirectoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        loadDirectory = loadDirectoryChooser.showDialog(null);

        if (loadDirectory != null) {
            System.out.println("Selected Directory: " + loadDirectory.getAbsolutePath());
            loadDirectoryLabel.setText("Selected: " + loadDirectory.getAbsolutePath());
            loadDirectorySet = true;

            if (storeDirectorySet) loadBtn.setDisable(false);
        } else {
            System.out.println("No directory selected.");
        }
    }

    @FXML
    private void onSelectStoreDirectoryClick() {
        storeDirectoryChooser.setTitle("Select a Directory");
        storeDirectoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        storeDirectory = storeDirectoryChooser.showDialog(null);

        if (storeDirectory != null) {
            System.out.println("Selected Directory: " + storeDirectory.getAbsolutePath());
            storeDirectoryLabel.setText("Selected: " + storeDirectory.getAbsolutePath());
            storeDirectorySet = true;

            if (loadDirectorySet) loadBtn.setDisable(false);
        } else {
            System.out.println("No directory selected.");
        }
    }

    /*@FXML
    private void onSelectDisplayDirectoryClick() {
        displayDirectoryChooser.setTitle("Select a Directory");
        displayDirectoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        displayDirectory = displayDirectoryChooser.showDialog(null);

        if (displayDirectory != null) {
            System.out.println("Selected Directory: " + displayDirectory.getAbsolutePath());
            displayDirectoryLabel.setText("Selected: " + displayDirectory.getAbsolutePath());
            displayDirectorySet = true;

            displayBtn.setDisable(false);
        } else {
            System.out.println("No directory selected.");
        }
    }*/

    @FXML
    private void onSelectDisplayFileClick() {
        displayFileChooser.setTitle("Select a File");
        displayFileChooser.setInitialFileName(String.valueOf(new File(System.getProperty("user.home"))));

        //displayDirectory = displayFileChooser.showDialog(null);
        displayFile = displayFileChooser.showOpenDialog(null);

        if (displayFile != null) {
            System.out.println("Selected File: " + displayFile.getAbsolutePath());
            displayFileLabel.setText("Selected: " + displayFile.getAbsolutePath());
            displayFileSet = true;

            displayBtn.setDisable(false);
        } else {
            System.out.println("No file selected.");
        }
    }

    @FXML
    protected void onLoadBtnClick() throws IOException {

        // Create a ProgressIndicator
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(100, 100);

        // Create a StackPane to hold the ProgressIndicator
        StackPane stackPane = new StackPane(progressIndicator);
        stackPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0);");
        stackPane.setAlignment(Pos.CENTER);

        // Add the StackPane to the current scene
        Scene currentScene = stage.getScene();
        ((Pane) currentScene.getRoot()).getChildren().add(stackPane);

        /*if (imageFormatBox.getValue().equals("Laboratory-HSI")) loadLabHsiScreen(currentScene, stackPane);
        else if (imageFormatBox.getValue().equals("SpecimIQ-HSI")) loadSpecimIQScreen(currentScene, stackPane);
        else if (imageFormatBox.getValue().equals("Microscopy-HSI")) loadMicroscopyScreen(currentScene, stackPane);
*/
        String format = findFormat();
        System.out.println("Format: " + format);

        switch (format) {
            case "Laboratory-HSI" -> loadLabHsiScreen(currentScene, stackPane);
            case "SpecimIQ-HSI" -> loadSpecimIQScreen(currentScene, stackPane);
            case "Microscopy-HSI" -> loadMicroscopyScreen(currentScene, stackPane);
            default -> {
                showAlert("Error", "Unsupported file format. Please select a valid directory.");
                ((Pane) currentScene.getRoot()).getChildren().remove(stackPane);
            }
        }
    }

    private String findFormat() throws IOException {
        if (checkFormatLabHsi()) return "Laboratory-HSI";
        else if (checkFormatSpecimIQ()) return "SpecimIQ-HSI";
        else if (checkFormatMicroscopyHsi()) return "Microscopy-HSI";
        else return "";
    }

    private boolean checkFormatMicroscopyHsi() throws IOException {
        Set<String> files;

        try (Stream<Path> paths = Files.walk(Path.of(loadDirectory.getAbsolutePath()))) {
            files = paths
                    .filter(Files::isRegularFile) // Only include regular files
                    .filter(path -> !path.getFileName().toString().startsWith("._")) // Exclude files starting with "._"
                    .map(path -> Path.of(loadDirectory.getAbsolutePath()).relativize(path).toString()) // Get file names
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

        return containsWhite && containsDark && containsGreen;
    }

    private boolean checkFormatSpecimIQ() throws IOException {
        Set<String> captureFiles = Files.walk(Path.of(loadDirectory.getAbsolutePath()))
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

        return containsDarkRefHdr && containsDarkRefRaw &&
                containsWhiteDarkRefHdr && containsWhiteDarkRefRaw &&
                containsWhiteRefHdr && containsWhiteRefRaw &&
                containsHdr && containsRaw;
    }

    private boolean checkFormatLabHsi() {
        Set<String> files = Stream.of(new File(loadDirectory.getAbsolutePath()).listFiles())
                .filter(file -> !file.isDirectory() && !file.getName().startsWith("._"))
                .map(File::getName)
                .collect(Collectors.toSet());

        boolean containsHeader = false, containsRaw = false, containsWhite = false;
        for (String s:files.stream().toArray(String[]::new)) {
            if (s.contains(LABHSI_HEADER_FILENAME_TEMPLATE)) {
                containsHeader = true;
            }
            else if (s.contains(LABHSI_RAW_DATA_FILENAME_TEMPLATE)) {
                containsRaw = true;
            }
            else if (s.contains(LABHSI_WHITE_REFERENCE_FILENAME_TEMPLATE)) {
                containsWhite = true;
            }
        }
        return containsHeader && containsRaw && containsWhite;
    }

    private void loadMicroscopyScreen(Scene currentScene, StackPane stackPane) throws DirectoryNotFoundException {
        FXMLLoader fxmlMicroscopyHSILoader = new FXMLLoader(MedFileApplication.class.getResource("controllers/microscopy-hsi-view.fxml"));
        MicroscopyHSIConverter microscopyHSIConverter = new MicroscopyHSIConverter(loadDirectory.getAbsolutePath(), storeDirectory.getAbsolutePath());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                selectLoadDirectoryBtn.setDisable(true);
                selectStoreDirectoryBtn.setDisable(true);
                loadBtn.setDisable(true);
                selectDisplayFileBtn.setDisable(true);
                displayBtn.setDisable(true);

                microscopyHSIConverter.run();
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            try {
                ((Pane) currentScene.getRoot()).getChildren().remove(stackPane);

                Scene scene = new Scene(fxmlMicroscopyHSILoader.load(), SceneConstants.SCENE_WIDTH, SceneConstants.SCENE_HEIGHT);
                stage.setScene(scene);

                MicroscopyHSIDisplayer microscopyHSIDisplayer = new MicroscopyHSIDisplayer(microscopyHSIConverter.getDarkMetadataMap(), microscopyHSIConverter.getNumDarkImages(),
                        microscopyHSIConverter.getGreenMetadataMap(), microscopyHSIConverter.getNumGreenImages(),
                        microscopyHSIConverter.getWhiteMetadataMap(), microscopyHSIConverter.getNumWhiteImages(),
                        true, microscopyHSIConverter.getHdfDirectoryPath());

                MicroscopyHSIController microscopyHSIController = fxmlMicroscopyHSILoader.getController();
                microscopyHSIController.setupDisplayer(microscopyHSIDisplayer);
                microscopyHSIController.setStage(stage);
                microscopyHSIController.setMicroscopyHSIConverter(microscopyHSIConverter);

                //System.out.println("metadata");
                //laboratoryHSIDisplayer.printMetadata();

                MedFileApplication.setLoadDirectory(microscopyHSIConverter.getBasePath());
                MedFileApplication.setImageFormat("Microscopy-HSI");

                Task<Void> displayTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        microscopyHSIController.generateAndDisplayInitialImage();
                        return null;
                    }
                };
                new Thread(displayTask).start();
            } catch (IOException e) {
                // e.printStackTrace();
                showAlert("Error", "Failed to load the new scene: " + e.getMessage());
                System.out.println("Error: " + e.getMessage());
            }
        });

        task.setOnFailed(event -> {
            Throwable e = task.getException();
            if (e instanceof FileNotFoundException) {
                showAlert("File Not Found", "Error: " + e.getMessage());
            } else if (e instanceof DirectoryNotEmptyException) {
                showAlert("Directory Not Empty", "Error: " + e.getMessage());
            } else {
                showAlert("Unexpected Error", "An unexpected error occurred. Please try again.");
            }
            e.printStackTrace();

            ((Pane) currentScene.getRoot()).getChildren().remove(stackPane);

            loadDirectorySet = false;
            storeDirectorySet = false;
            loadDirectoryLabel.setText("Selected: None");
            storeDirectoryLabel.setText("Selected: None");

            selectLoadDirectoryBtn.setDisable(false);
            selectStoreDirectoryBtn.setDisable(false);
            loadBtn.setDisable(true);
            selectDisplayFileBtn.setDisable(false);
            displayBtn.setDisable(true);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true); // Optional: Allows the thread to exit when the application exits
        thread.start();
    }

    private void loadLabHsiScreen(Scene currentScene, Pane stackPane) throws DirectoryNotFoundException {
        FXMLLoader fxmlLabHSILoader = new FXMLLoader(MedFileApplication.class.getResource("controllers/lab-hsi-view.fxml"));
        LaboratoryHSIConverter laboratoryHSIConverter = new LaboratoryHSIConverter(
                loadDirectory.getAbsolutePath(),
                storeDirectory.getAbsolutePath()
        );

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                selectLoadDirectoryBtn.setDisable(true);
                selectStoreDirectoryBtn.setDisable(true);
                loadBtn.setDisable(true);
                selectDisplayFileBtn.setDisable(true);
                displayBtn.setDisable(true);

                laboratoryHSIConverter.run();
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            try {
                ((Pane) currentScene.getRoot()).getChildren().remove(stackPane);

                Scene scene = new Scene(fxmlLabHSILoader.load(), SceneConstants.SCENE_WIDTH, SceneConstants.SCENE_HEIGHT);
                stage.setScene(scene);

                LaboratoryHSIDisplayer laboratoryHSIDisplayer = new LaboratoryHSIDisplayer(
                        laboratoryHSIConverter.getDataMap(),
                        laboratoryHSIConverter.getPositions(),
                        laboratoryHSIConverter.getWavelengths(),
                        true,
                        laboratoryHSIConverter.getHdfDirectoryPath(),
                        scene
                );
                LabHSIController labHSIController = fxmlLabHSILoader.getController();
                labHSIController.setupDisplayer(laboratoryHSIDisplayer);
                labHSIController.setStage(stage);
                labHSIController.setLabHSIConverter(laboratoryHSIConverter);

                System.out.println("metadata");
                laboratoryHSIDisplayer.printMetadata();

                MedFileApplication.setLoadDirectory(laboratoryHSIConverter.getBasePath());
                MedFileApplication.setImageFormat("Laboratory-HSI");

                Task<Void> displayTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        labHSIController.generateAndDisplayInitialImage();
                        return null;
                    }
                };
                new Thread(displayTask).start();
            } catch (IOException e) {
                // e.printStackTrace();
                showAlert("Error", "Failed to load the new scene: " + e.getMessage());
                System.out.println("Error: " + e.getMessage());
            }
        });

        task.setOnFailed(event -> {
            Throwable e = task.getException();
            if (e instanceof FileNotFoundException) {
                showAlert("File Not Found", "Error: " + e.getMessage());
            } else if (e instanceof DirectoryNotEmptyException) {
                showAlert("Directory Not Empty", "Error: " + e.getMessage());
            } else {
                showAlert("Unexpected Error", "An unexpected error occurred. Please try again.");
            }
            //e.printStackTrace();

            ((Pane) currentScene.getRoot()).getChildren().remove(stackPane);

            loadDirectorySet = false;
            storeDirectorySet = false;
            loadDirectoryLabel.setText("Selected: None");
            storeDirectoryLabel.setText("Selected: None");

            selectLoadDirectoryBtn.setDisable(false);
            selectStoreDirectoryBtn.setDisable(false);
            loadBtn.setDisable(true);
            selectDisplayFileBtn.setDisable(false);
            displayBtn.setDisable(true);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true); // Optional: Allows the thread to exit when the application exits
        thread.start();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadSpecimIQScreen(Scene currentScene, Pane stackPane) throws DirectoryNotFoundException {
        FXMLLoader fxmlSpecimIQHSILoader = new FXMLLoader(MedFileApplication.class.getResource("controllers/specimiq-hsi-view.fxml"));
        SpecimIQHSIConverter specimIQHSIConverter = new SpecimIQHSIConverter(loadDirectory.getAbsolutePath(), storeDirectory.getAbsolutePath());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                selectLoadDirectoryBtn.setDisable(true);
                selectStoreDirectoryBtn.setDisable(true);
                loadBtn.setDisable(true);
                specimIQHSIConverter.run();
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            ((Pane) currentScene.getRoot()).getChildren().remove(stackPane);

            Scene scene = null;
            try {
                scene = new Scene(fxmlSpecimIQHSILoader.load(), SceneConstants.SCENE_WIDTH, SceneConstants.SCENE_HEIGHT);
            } catch (IOException e) {
                showAlert("Error", "Failed to load the new scene: " + e.getMessage());
            }
            stage.setScene(scene);

            SpecimIQHSIDisplayer specimIQHSIDisplayer = new SpecimIQHSIDisplayer(
                    true,
                    specimIQHSIConverter.getHdfDirectoryPath(),
                    specimIQHSIConverter.getHdrMetadata(),
                    specimIQHSIConverter.getResultsHdrMetadata()
            );
            SpecimIQHSIController specimIQHSIController = fxmlSpecimIQHSILoader.getController();
            specimIQHSIController.setupDisplayer(specimIQHSIDisplayer);
            specimIQHSIController.setStage(stage);

            MedFileApplication.setLoadDirectory(specimIQHSIConverter.getBasePath());
            MedFileApplication.setImageFormat("SpecimIQ-HSI");

            Task<Void> displayTask = new Task<>() {
                @Override
                protected Void call() {
                    specimIQHSIController.generateAndDisplayInitialImage();
                    return null;
                }
            };
            new Thread(displayTask).start();
        });

        task.setOnFailed(event -> {
            Throwable e = task.getException();
            if (e instanceof FileNotFoundException) {
                showAlert("File Not Found", "Error: " + e.getMessage());
            } else {
                showAlert("Unexpected Error", "An unexpected error occurred. Please try again.");
                System.out.println("Error: " + e.getMessage());
                System.out.println("Cause: " + e.getCause());
                e.printStackTrace();
            }
            // e.printStackTrace();

            ((Pane) currentScene.getRoot()).getChildren().remove(stackPane);

            loadDirectorySet = false;
            storeDirectorySet = false;
            loadDirectoryLabel.setText("Selected: None");
            storeDirectoryLabel.setText("Selected: None");

            selectLoadDirectoryBtn.setDisable(false);
            selectStoreDirectoryBtn.setDisable(false);
            loadBtn.setDisable(true);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true); // Optional: Allows the thread to exit when the application exits
        thread.start();
    }

    @FXML
    protected void onDisplayBtnClick() throws DirectoryNotFoundException {

        // Create a ProgressIndicator
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(100, 100);

        // Create a StackPane to hold the ProgressIndicator
        StackPane stackPane = new StackPane(progressIndicator);
        stackPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0);");
        stackPane.setAlignment(Pos.CENTER);

        // Add the StackPane to the current scene
        Scene currentScene = stage.getScene();
        ((Pane) currentScene.getRoot()).getChildren().add(stackPane);

        /*if (displayImageFormatBox.getValue().equals("Laboratory-HSI")) loadLabHsiDisplayScreen(currentScene, stackPane);
        else if (displayImageFormatBox.getValue().equals("SpecimIQ-HSI")) loadSpecimIQDisplayScreen(currentScene, stackPane);
        else if (displayImageFormatBox.getValue().equals("Microscopy-HSI")) loadMicroscopyHsiDisplayScreen(currentScene, stackPane);
        */

        String format = findDisplayFormat();
        switch (format) {
            case "Laboratory-HSI" -> loadLabHsiDisplayScreen(currentScene, stackPane);
            case "SpecimIQ-HSI" -> loadSpecimIQDisplayScreen(currentScene, stackPane);
            case "Microscopy-HSI" -> loadMicroscopyHsiDisplayScreen(currentScene, stackPane);
            default -> {
                showAlert("Error", "Unsupported file format. Please select a valid directory.");
                ((Pane) currentScene.getRoot()).getChildren().remove(stackPane);
            }
        }
    }

    private String findDisplayFormat() {
        try {
            /*File folder = new File(displayDirectory.getAbsolutePath());
            File[] files = folder.listFiles();

            String hdfPath = "";

            assert files != null;
            for (File file : files) {
                if (file.getName().endsWith(".h5")) {
                    hdfPath = file.getAbsolutePath();
                    System.out.println("Found HDF file: " + hdfPath);
                    break;
                }
            }*/

            String hdfPath = displayFile.getAbsolutePath();
            if (!hdfPath.endsWith(".h5")) {
                showAlert("Error", "Selected file is not a valid HDF5 file.");
                return "";
            }

            int readFileId = H5.H5Fopen(hdfPath, HDF5Constants.H5F_ACC_RDONLY, HDF5Constants.H5P_DEFAULT);

            // Open the attribute
            String attributeName = "format"; // Replace with the name of your attribute
            int attributeId = H5.H5Aopen(readFileId, attributeName, HDF5Constants.H5P_DEFAULT);

            // Get the size of the attribute
            int dataspaceId = H5.H5Aget_space(attributeId);
            long[] dims = new long[1];
            H5.H5Sget_simple_extent_dims(dataspaceId, dims, null);
            int size = (int) dims[0];

            // Read the attribute value
            byte[] buffer = new byte[size];
            H5.H5Aread(attributeId, HDF5Constants.H5T_NATIVE_CHAR, buffer);

            // Convert the byte array to a string
            String attributeValue = new String(buffer, StandardCharsets.UTF_8);
            System.out.println("Attribute Value: " + attributeValue);

            // Close resources
            H5.H5Sclose(dataspaceId);
            H5.H5Aclose(attributeId);
            H5.H5Fclose(readFileId);

            return attributeValue;
        } catch (Exception e) {
            showAlert("Error", "Failed to read an attribute");
            return "";
        }
    }

    private void loadMicroscopyHsiDisplayScreen(Scene currentScene, StackPane stackPane) {
        FXMLLoader fxmlLabHSILoader = new FXMLLoader(MedFileApplication.class.getResource("controllers/microscopy-hsi-view.fxml"));
        MicroscopyHSIReader microscopyHSIReader = new MicroscopyHSIReader(displayFile.getAbsolutePath());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                selectLoadDirectoryBtn.setDisable(true);
                selectStoreDirectoryBtn.setDisable(true);
                loadBtn.setDisable(true);
                selectDisplayFileBtn.setDisable(true);
                displayBtn.setDisable(true);

                microscopyHSIReader.run();
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            try {
                ((Pane) currentScene.getRoot()).getChildren().remove(stackPane);

                Scene scene = new Scene(fxmlLabHSILoader.load(), SceneConstants.SCENE_WIDTH, SceneConstants.SCENE_HEIGHT);
                stage.setScene(scene);

                MicroscopyHSIDisplayer microscopyHSIDisplayer = new MicroscopyHSIDisplayer(microscopyHSIReader.getDarkMetadataMap(), microscopyHSIReader.getNumDarkImages(),
                        microscopyHSIReader.getGreenMetadataMap(), microscopyHSIReader.getNumGreenImages(),
                        microscopyHSIReader.getWhiteMetadataMap(), microscopyHSIReader.getNumWhiteImages(),
                        false, microscopyHSIReader.getHdfPath());

                MicroscopyHSIController microscopyHSIController = fxmlLabHSILoader.getController();
                microscopyHSIController.setupDisplayer(microscopyHSIDisplayer);
                microscopyHSIController.setStage(stage);

                MedFileApplication.setDisplayFile(microscopyHSIReader.getHdfPath());
                MedFileApplication.setImageFormat("Microscopy-HSI");

                Task<Void> displayTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        microscopyHSIController.generateAndDisplayInitialImage();
                        return null;
                    }
                };
                new Thread(displayTask).start();
            } catch (IOException e) {
                // e.printStackTrace();
                showAlert("Error", "Failed to load the new scene: " + e.getMessage());
                System.out.println("Error: " + e.getMessage());
            }
            });

        task.setOnFailed(event -> {
            Throwable e = task.getException();
            if (e instanceof FileNotFoundException) {
                showAlert("File Not Found", "Error: " + e.getMessage());
            } else {
                showAlert("Unexpected Error", "An unexpected error occurred. Please try again.");
            }
            // e.printStackTrace();

            ((Pane) currentScene.getRoot()).getChildren().remove(stackPane);

            loadDirectorySet = false;
            storeDirectorySet = false;
            loadDirectoryLabel.setText("Selected: None");
            storeDirectoryLabel.setText("Selected: None");

            selectLoadDirectoryBtn.setDisable(false);
            selectStoreDirectoryBtn.setDisable(false);
            loadBtn.setDisable(true);
            selectDisplayFileBtn.setDisable(false);
            displayBtn.setDisable(true);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true); // Optional: Allows the thread to exit when the application exits
        thread.start();
    }

    private void loadLabHsiDisplayScreen(Scene currentScene, StackPane stackPane) {
        FXMLLoader fxmlLabHSILoader = new FXMLLoader(MedFileApplication.class.getResource("controllers/lab-hsi-view.fxml"));
        //LaboratoryHSIReader laboratoryHSIReader = new LaboratoryHSIReader(displayDirectory.getAbsolutePath());
        LaboratoryHSIReader laboratoryHSIReader = new LaboratoryHSIReader(displayFile.getAbsolutePath());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                selectLoadDirectoryBtn.setDisable(true);
                selectStoreDirectoryBtn.setDisable(true);
                loadBtn.setDisable(true);
                selectDisplayFileBtn.setDisable(true);
                displayBtn.setDisable(true);

                laboratoryHSIReader.run();
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            try {
                ((Pane) currentScene.getRoot()).getChildren().remove(stackPane);

                Scene scene = new Scene(fxmlLabHSILoader.load(), SceneConstants.SCENE_WIDTH, SceneConstants.SCENE_HEIGHT);
                stage.setScene(scene);

                LaboratoryHSIDisplayer laboratoryHSIDisplayer = new LaboratoryHSIDisplayer(
                        laboratoryHSIReader.getDataMap(),
                        laboratoryHSIReader.getPositions(),
                        laboratoryHSIReader.getWavelengths(),
                        false,
                        laboratoryHSIReader.getHdfPath(),
                        scene
                );
                LabHSIController labHSIController = fxmlLabHSILoader.getController();
                labHSIController.setupDisplayer(laboratoryHSIDisplayer);
                labHSIController.setStage(stage);

                System.out.println("metadata");
                laboratoryHSIDisplayer.printMetadata();

                MedFileApplication.setDisplayFile(laboratoryHSIReader.getHdfPath());
                MedFileApplication.setImageFormat("Laboratory-HSI");

                Task<Void> displayTask = new Task<>() {
                    @Override
                    protected Void call() {
                        labHSIController.generateAndDisplayInitialImage();
                        System.out.println("generateAndDisplayInitialImage");
                        return null;
                    }
                };
                new Thread(displayTask).start();
            } catch (IOException e) {
                // e.printStackTrace();
                showAlert("Error", "Failed to load the new scene: " + e.getMessage());
                System.out.println("Error: " + e.getMessage());
            }

        });

        task.setOnFailed(event -> {
            Throwable e = task.getException();
            if (e instanceof FileNotFoundException) {
                showAlert("File Not Found", "Error: " + e.getMessage());
            } else {
                showAlert("Unexpected Error", "An unexpected error occurred. Please try again.");
            }
             e.printStackTrace();

            ((Pane) currentScene.getRoot()).getChildren().remove(stackPane);

            loadDirectorySet = false;
            storeDirectorySet = false;
            loadDirectoryLabel.setText("Selected: None");
            storeDirectoryLabel.setText("Selected: None");

            selectLoadDirectoryBtn.setDisable(false);
            selectStoreDirectoryBtn.setDisable(false);
            loadBtn.setDisable(true);
            selectDisplayFileBtn.setDisable(false);
            displayBtn.setDisable(true);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true); // Optional: Allows the thread to exit when the application exits
        thread.start();
    }

    private void loadSpecimIQDisplayScreen(Scene currentScene, Pane stackPane) throws DirectoryNotFoundException {
        FXMLLoader fxmlSpecimIQHSILoader = new FXMLLoader(MedFileApplication.class.getResource("controllers/specimiq-hsi-view.fxml"));
        SpecimIQHSIReader specimIQHSIReader = new SpecimIQHSIReader(displayFile.getAbsolutePath());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                selectLoadDirectoryBtn.setDisable(true);
                selectStoreDirectoryBtn.setDisable(true);
                loadBtn.setDisable(true);
                specimIQHSIReader.run();
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            ((Pane) currentScene.getRoot()).getChildren().remove(stackPane);

            Scene scene = null;
            try {
                scene = new Scene(fxmlSpecimIQHSILoader.load(), SceneConstants.SCENE_WIDTH, SceneConstants.SCENE_HEIGHT);
            } catch (IOException e) {
                showAlert("Error", "Failed to load the new scene: " + e.getMessage());
            }
            stage.setScene(scene);

            SpecimIQHSIDisplayer specimIQHSIDisplayer = new SpecimIQHSIDisplayer(
                    false,
                    specimIQHSIReader.getHdfPath(),
                    specimIQHSIReader.getHdrMetadata(),
                    specimIQHSIReader.getResultsHdrMetadata()
            );
            SpecimIQHSIController specimIQHSIController = fxmlSpecimIQHSILoader.getController();
            specimIQHSIController.setupDisplayer(specimIQHSIDisplayer);
            specimIQHSIController.setStage(stage);

            MedFileApplication.setDisplayFile(specimIQHSIReader.getHdfPath());
            MedFileApplication.setImageFormat("SpecimIQ-HSI");

            Task<Void> displayTask = new Task<>() {
                @Override
                protected Void call() {
                    specimIQHSIController.generateAndDisplayInitialImage();
                    return null;
                }
            };
            new Thread(displayTask).start();
        });

        task.setOnFailed(event -> {
            Throwable e = task.getException();
            if (e instanceof FileNotFoundException) {
                showAlert("File Not Found", "Error: " + e.getMessage());
            } else {
                showAlert("Unexpected Error", "An unexpected error occurred. Please try again.");
                System.out.println("Error: " + e.getMessage());
                System.out.println("Cause: " + e.getCause());
                e.printStackTrace();
            }
            // e.printStackTrace();

            ((Pane) currentScene.getRoot()).getChildren().remove(stackPane);

            loadDirectorySet = false;
            storeDirectorySet = false;
            loadDirectoryLabel.setText("Selected: None");
            storeDirectoryLabel.setText("Selected: None");

            selectLoadDirectoryBtn.setDisable(false);
            selectStoreDirectoryBtn.setDisable(false);
            loadBtn.setDisable(true);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true); // Optional: Allows the thread to exit when the application exits
        thread.start();
    }
}