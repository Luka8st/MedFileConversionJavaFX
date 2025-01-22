package hr.riteh.medfileconversionjavafx.controllers;

import hr.riteh.medfileconversionjavafx.MedFileApplication;
import hr.riteh.medfileconversionjavafx.constants.SceneConstants;
import hr.riteh.medfileconversionjavafx.converters.LaboratoryHSIConverter;
import hr.riteh.medfileconversionjavafx.converters.SpecimIQHSIConverter;
import hr.riteh.medfileconversionjavafx.displayers.LaboratoryHSIDisplayer;
import hr.riteh.medfileconversionjavafx.displayers.SpecimIQHSIDisplayer;
import hr.riteh.medfileconversionjavafx.exceptions.DirectoryNotFoundException;
import hr.riteh.medfileconversionjavafx.readers.LaboratoryHSIReader;
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
import javafx.stage.Stage;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MedFileController {
    private File loadDirectory;
    private File storeDirectory;
    private boolean loadDirectorySet = false;
    private boolean storeDirectorySet = false;
    private Stage stage;

    private File displayDirectory;
    private boolean displayDirectorySet = false;

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
    private ChoiceBox<String> imageFormatBox;

    @FXML 
    private Separator verticalSeparator;



    @FXML
    private DirectoryChooser displayDirectoryChooser = new DirectoryChooser();
    @FXML
    private Label displayDirectoryLabel;
    @FXML
    private Button displayBtn;
    @FXML
    private Button selectDisplayDirectoryBtn;
    @FXML
    private ChoiceBox<String> displayImageFormatBox;

    @FXML
    public void initialize() {
        // Set the default value for imageFormatBox
        imageFormatBox.setValue("Laboratory-HSI");
        displayImageFormatBox.setValue("Laboratory-HSI");
        // verticalSeparator.prefHeightProperty().bind(mainHBox.heightProperty().multiply(0.5));

        leftVBox.setPrefWidth(0.3*SceneConstants.SCENE_WIDTH);
        rightVBox.setPrefWidth(0.3*SceneConstants.SCENE_WIDTH);

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

    @FXML
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
    }

    @FXML
    protected void onLoadBtnClick() throws DirectoryNotFoundException {

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

        if (imageFormatBox.getValue().equals("Laboratory-HSI")) loadLabHsiScreen(currentScene, stackPane);
        else if (imageFormatBox.getValue().equals("SpecimIQ-HSI")) loadSpecimIQScreen(currentScene, stackPane);
    }

    private void loadLabHsiScreen(Scene currentScene, Pane stackPane) throws DirectoryNotFoundException {
        FXMLLoader fxmlLabHSILoader = new FXMLLoader(MedFileApplication.class.getResource("controllers/lab-hsi-view.fxml"));
        LaboratoryHSIConverter laboratoryHSIConverter = new LaboratoryHSIConverter(loadDirectory.getAbsolutePath(), storeDirectory.getAbsolutePath());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                selectLoadDirectoryBtn.setDisable(true);
                selectStoreDirectoryBtn.setDisable(true);
                loadBtn.setDisable(true);
                selectDisplayDirectoryBtn.setDisable(true);
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
                        laboratoryHSIConverter.getHdfDirectoryPath(),
                        scene
                );
                LabHSIController labHSIController = fxmlLabHSILoader.getController();
                labHSIController.setupDisplayer(laboratoryHSIDisplayer);

                System.out.println("metadata");
                laboratoryHSIDisplayer.printMetadata();

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
            selectDisplayDirectoryBtn.setDisable(false);
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
                    specimIQHSIConverter.getHdfDirectoryPath(),
                    specimIQHSIConverter.getHdrMetadata(),
                    specimIQHSIConverter.getResultsHdrMetadata()
            );
            SpecimIQHSIController specimIQHSIController = fxmlSpecimIQHSILoader.getController();
            specimIQHSIController.setupDisplayer(specimIQHSIDisplayer);

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

        if (displayImageFormatBox.getValue().equals("Laboratory-HSI")) loadLabHsiDisplayScreen(currentScene, stackPane);
        else if (displayImageFormatBox.getValue().equals("SpecimIQ-HSI")) loadSpecimIQDisplayScreen(currentScene, stackPane);
    }

    private void loadLabHsiDisplayScreen(Scene currentScene, StackPane stackPane) {
        FXMLLoader fxmlLabHSILoader = new FXMLLoader(MedFileApplication.class.getResource("controllers/lab-hsi-view.fxml"));
        LaboratoryHSIReader laboratoryHSIReader = new LaboratoryHSIReader(displayDirectory.getAbsolutePath());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                selectLoadDirectoryBtn.setDisable(true);
                selectStoreDirectoryBtn.setDisable(true);
                loadBtn.setDisable(true);
                selectDisplayDirectoryBtn.setDisable(true);
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
                        laboratoryHSIReader.getLoadPath(),
                        scene
                );
                LabHSIController labHSIController = fxmlLabHSILoader.getController();
                labHSIController.setupDisplayer(laboratoryHSIDisplayer);

                System.out.println("metadata");
                laboratoryHSIDisplayer.printMetadata();

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
            selectDisplayDirectoryBtn.setDisable(false);
            displayBtn.setDisable(true);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true); // Optional: Allows the thread to exit when the application exits
        thread.start();
    }

    private void loadSpecimIQDisplayScreen(Scene currentScene, Pane stackPane) throws DirectoryNotFoundException {
        FXMLLoader fxmlSpecimIQHSILoader = new FXMLLoader(MedFileApplication.class.getResource("controllers/specimiq-hsi-view.fxml"));
        SpecimIQHSIReader specimIQHSIReader = new SpecimIQHSIReader(displayDirectory.getAbsolutePath());

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
                    specimIQHSIReader.getLoadPath(),
                    specimIQHSIReader.getHdrMetadata(),
                    specimIQHSIReader.getResultsHdrMetadata()
            );
            SpecimIQHSIController specimIQHSIController = fxmlSpecimIQHSILoader.getController();
            specimIQHSIController.setupDisplayer(specimIQHSIDisplayer);

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
    protected void onFormatChoiceMade() {
        System.out.println("Selected format: " + imageFormatBox.getValue());
    }

    @FXML
    protected void onDisplayFormatChoiceMade() {
        System.out.println("Selected format: " + displayImageFormatBox.getValue());
    }
}