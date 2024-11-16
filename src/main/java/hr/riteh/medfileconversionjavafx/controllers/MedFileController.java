package hr.riteh.medfileconversionjavafx.controllers;

import hr.riteh.medfileconversionjavafx.MedFileApplication;
import hr.riteh.medfileconversionjavafx.constants.SceneConstants;
import hr.riteh.medfileconversionjavafx.converters.LaboratoryHSIConverter;
import hr.riteh.medfileconversionjavafx.displayers.LaboratoryHSIDisplayer;
import hr.riteh.medfileconversionjavafx.exceptions.DirectoryNotFoundException;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
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

//    @FXML
//    protected void onLoadBtnClick() throws Exception {
//        FXMLLoader fxmlLabHSILoader = new FXMLLoader(MedFileApplication.class.getResource("controllers/lab-hsi-view.fxml"));
//
//        LaboratoryHSIConverter laboratoryHSIConverter = new LaboratoryHSIConverter(loadDirectory.getAbsolutePath(), storeDirectory.getAbsolutePath());
//
//        Scene scene = new Scene(fxmlLabHSILoader.load(), SceneConstants.SCENE_HEIGHT, SceneConstants.SCENE_WIDTH);
//        stage.setScene(scene);
//
//        Task<Void> task = new Task<>() {
//            @Override
//            protected Void call() throws Exception {
//                laboratoryHSIConverter.run();
//                return null;
//            }
//        };
//
//
//        Thread thread = new Thread(task);
//        thread.setDaemon(true); // Optional: Allows the thread to exit when the application exits
//        thread.start();
//
//        LaboratoryHSIDisplayer laboratoryHSIDisplayer = new LaboratoryHSIDisplayer(
//                laboratoryHSIConverter.getDataMap(),
//                laboratoryHSIConverter.getPositions(),
//                laboratoryHSIConverter.getWavelengths(),
//                scene
//        );
//
//        LabHSIController labHSIController = fxmlLabHSILoader.getController();
//        labHSIController.setupDisplayer(laboratoryHSIDisplayer);
////        labHSIController.generateAndDisplayInitialImage();
//
//        Task<Void> displayTask = new Task<>() {
//            @Override
//            protected Void call() throws Exception {
//                labHSIController.generateAndDisplayInitialImage();
//                return null;
//            }
//        };
//        new Thread(displayTask).start();
//    }

    /*
    @FXML
    protected void onLoadBtnClick() {
        try {
            FXMLLoader fxmlLabHSILoader = new FXMLLoader(MedFileApplication.class.getResource("controllers/lab-hsi-view.fxml"));
            LaboratoryHSIConverter laboratoryHSIConverter = new LaboratoryHSIConverter(loadDirectory.getAbsolutePath(), storeDirectory.getAbsolutePath());
            Scene scene = new Scene(fxmlLabHSILoader.load(), SceneConstants.SCENE_HEIGHT, SceneConstants.SCENE_WIDTH);
            stage.setScene(scene);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        laboratoryHSIConverter.run();
                        return null;
                    } catch (FileNotFoundException e) {
                        System.out.println("Error: " + e.getMessage());
                        throw e;
                    }
                }
            };

            task.setOnSucceeded(event -> {
                LaboratoryHSIDisplayer laboratoryHSIDisplayer = new LaboratoryHSIDisplayer(
                        laboratoryHSIConverter.getDataMap(),
                        laboratoryHSIConverter.getPositions(),
                        laboratoryHSIConverter.getWavelengths(),
                        laboratoryHSIConverter.getHdfDirectoryPath(),
                        scene
                );
                LabHSIController labHSIController = fxmlLabHSILoader.getController();
                labHSIController.setupDisplayer(laboratoryHSIDisplayer);

                Task<Void> displayTask = new Task<>() {
                    @Override
                    protected Void call() {
                        labHSIController.generateAndDisplayInitialImage();
                        return null;
                    }
                };
                new Thread(displayTask).start();
            });

            task.setOnFailed(event -> {
                Throwable e = task.getException();
                if (e instanceof FileNotFoundException) {
                    System.out.println("Error: " + e.getMessage());
                    // Display an alert dialog if a FileNotFoundException is thrown
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("File Not Found");
                    alert.setHeaderText(null);
                    alert.setContentText("Error: " + e.getMessage());
                    alert.showAndWait();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Unexpected Error");
                    alert.setHeaderText(null);
                    alert.setContentText("An unexpected error occurred. Please try again.");
                    alert.showAndWait();
                }
            });

            Thread thread = new Thread(task);
            thread.setDaemon(true); // Optional: Allows the thread to exit when the application exits
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
     */

    @FXML
    protected void onLoadBtnClick() throws DirectoryNotFoundException {

        FXMLLoader fxmlLabHSILoader = new FXMLLoader(MedFileApplication.class.getResource("controllers/lab-hsi-view.fxml"));
        LaboratoryHSIConverter laboratoryHSIConverter = new LaboratoryHSIConverter(loadDirectory.getAbsolutePath(), storeDirectory.getAbsolutePath());


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



        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                selectLoadDirectoryBtn.setDisable(true);
                selectStoreDirectoryBtn.setDisable(true);
                loadBtn.setDisable(true);
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
}