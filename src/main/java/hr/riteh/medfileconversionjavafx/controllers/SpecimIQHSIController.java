package hr.riteh.medfileconversionjavafx.controllers;

import hr.riteh.medfileconversionjavafx.displayers.SpecimIQHSIDisplayer;
import hr.riteh.medfileconversionjavafx.helper.SpecimImageType;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.embed.swing.SwingFXUtils;

import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SpecimIQHSIController {


    @FXML
    private ImageView imageView;

    @FXML
    private ChoiceBox<SpecimImageType> datasetChoiceBox;

    @FXML
    private Button nextBtn;

    @FXML
    private Button prevBtn;

    @FXML
    private Button slideBtn;

    @FXML
    private ChoiceBox<String> pngChoiceBox;

    @FXML
    private ImageView pngImageView;

    @FXML
    public TextArea resultMetadataTextArea;

    @FXML
    public TextArea captureMetadataTextArea;

    private SpecimIQHSIDisplayer specimIQHSIDisplayer;
    private Timeline slideshowTimeline;
    private static final Set<String> EXCLUDED_KEYS_FOR_DISPLAY = Set.of("wavelength");


    @FXML
    public void initialize() {
        datasetChoiceBox.getItems().addAll(SpecimImageType.values());
        datasetChoiceBox.setValue(SpecimImageType.NORMAL);
        datasetChoiceBox.setOnAction(event -> switchDataset());

        pngChoiceBox.getItems().addAll(List.of("reflectance", "rgbBackground", "rgbScene", "rgbViewfinder"));
        pngChoiceBox.setValue("reflectance");
        pngChoiceBox.setOnAction(event -> switchPng());
    }

    private void switchDataset() {
        SpecimImageType imageType = datasetChoiceBox.getValue();
        specimIQHSIDisplayer.setCurrentDataset(imageType.getDatasetName());
        specimIQHSIDisplayer.setDimensions(imageType.getDimensions());
        specimIQHSIDisplayer.setSelectedSlice(0);
        generateAndDisplayInitialImage();
    }

    private void switchPng() {
        generateAndDisplayPngImage();
    }

    public void setSpecimIQHSIDisplayer(SpecimIQHSIDisplayer specimIQHSIDisplayer) {
        this.specimIQHSIDisplayer = specimIQHSIDisplayer;
    }

    private void generateAndDisplayNewImage() {
/*
        // Generate the BufferedImage using the displayer
        BufferedImage img = specimIQHSIDisplayer.getImg();

        // Save the BufferedImage to a file
        File tempFile = new File("tempImage.png");
        try {
            // Ensure the file does not exist before writing
            if (tempFile.exists()) {
                tempFile.delete(); // Delete existing file
            }

            // Write the BufferedImage using Apache Commons Imaging
            Imaging.writeImage(img, tempFile, ImageFormats.PNG); // Correct usage, no null parameter needed

            // Load the saved image into ImageView
            Image image = new Image(tempFile.toURI().toString());
            imageView.setImage(image);
        } catch (ImageWriteException e) {
            e.printStackTrace();
            System.err.println("Error writing image: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("I/O error: " + e.getMessage());
        }

//        Platform.runLater(() -> currentSlice.setText("Current slice: " + laboratoryHSIDisplayer.getSelectedSlice()));
*/
        BufferedImage img = specimIQHSIDisplayer.getImg();

        // Convert BufferedImage to WritableImage
        WritableImage writableImage = SwingFXUtils.toFXImage(img, null);

        // Set the WritableImage to the ImageView
        imageView.setImage(writableImage);
    }

    @FXML
    protected void onNextBtnClick() {
        specimIQHSIDisplayer.incrementSelectedSlice();
        generateAndDisplayNewImage();
    }

    @FXML
    protected void onPrevBtnClick() {
        specimIQHSIDisplayer.decrementSelectedSlice();
        generateAndDisplayNewImage();
    }

    @FXML
    protected void onSlideBtnClick() {
        if (slideshowTimeline == null || slideshowTimeline.getStatus() == Timeline.Status.STOPPED) {
            startSlideshow();
        } else {
            stopSlideshow();
        }
    }

    private void startSlideshow() {
        slideBtn.setText("Stop Slideshow");
        prevBtn.setDisable(true);
        nextBtn.setDisable(true);
        datasetChoiceBox.setDisable(true);

        slideshowTimeline = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            specimIQHSIDisplayer.incrementSelectedSlice();
            generateAndDisplayNewImage();
        }));
        slideshowTimeline.setCycleCount(Timeline.INDEFINITE);
        slideshowTimeline.play();
    }

    private void stopSlideshow() {
        slideBtn.setText("Start Slideshow");
        prevBtn.setDisable(false);
        nextBtn.setDisable(false);
        datasetChoiceBox.setDisable(false);

        if (slideshowTimeline != null) {
            slideshowTimeline.stop();
        }
    }

    private void updateButtonState() {
        boolean hasMultipleLines = specimIQHSIDisplayer.hasMultipleLines();
        nextBtn.setDisable(!hasMultipleLines);
        prevBtn.setDisable(!hasMultipleLines);
        slideBtn.setDisable(!hasMultipleLines);
    }

    public void setupDisplayer(SpecimIQHSIDisplayer specimIQHSIDisplayer) {
        setSpecimIQHSIDisplayer(specimIQHSIDisplayer);

        specimIQHSIDisplayer.setCurrentDataset(SpecimImageType.NORMAL.getDatasetName());
        specimIQHSIDisplayer.setDimensions(SpecimImageType.NORMAL.getDimensions());
        updateButtonState();

        displayCaptureMetadata();
        displayResultMetadata();
    }

    public void generateAndDisplayInitialImage() {
        generateAndDisplayNewImage();
        updateButtonState();

        generateAndDisplayPngImage();
    }

    /*
    private void generateAndDisplayPngImage() {
        // Generate the BufferedImage using the displayer
        BufferedImage img = specimIQHSIDisplayer.getPngImg(pngChoiceBox.getValue());

        // Save the BufferedImage to a file
        File tempFile = new File("tempPngImage.png");
        try {
            // Ensure the file does not exist before writing
            if (tempFile.exists()) {
                tempFile.delete(); // Delete existing file
            }

            // Write the BufferedImage using Apache Commons Imaging
            Imaging.writeImage(img, tempFile, ImageFormats.PNG); // Correct usage, no null parameter needed

            // Load the saved image into ImageView
            Image image = new Image(tempFile.toURI().toString());
            pngImageView.setImage(image);
        } catch (ImageWriteException e) {
            e.printStackTrace();
            System.err.println("Error writing image: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("I/O error: " + e.getMessage());
        }


//        Platform.runLater(() -> currentSlice.setText("Current slice: " + laboratoryHSIDisplayer.getSelectedSlice()));

    }*/

    private void generateAndDisplayPngImage() {
        // Generate the BufferedImage using the displayer
        BufferedImage img = specimIQHSIDisplayer.getPngImg(pngChoiceBox.getValue());

        // Convert BufferedImage to WritableImage
        WritableImage writableImage = SwingFXUtils.toFXImage(img, null);

        // Set the WritableImage to the ImageView
        pngImageView.setImage(writableImage);
    }

    private void displayResultMetadata() {
        StringBuilder metadata = new StringBuilder();
        for (Map.Entry<String, Object> entry : specimIQHSIDisplayer.getResultMetadataMap().entrySet()) {
            if (!EXCLUDED_KEYS_FOR_DISPLAY.contains(entry.getKey())) {
                metadata.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        resultMetadataTextArea.setText(metadata.toString());
    }

    private void displayCaptureMetadata() {
        StringBuilder metadata = new StringBuilder();
        for (Map.Entry<String, Object> entry : specimIQHSIDisplayer.getCaptureMetadataMap().entrySet()) {
            if (!EXCLUDED_KEYS_FOR_DISPLAY.contains(entry.getKey())) {
                metadata.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        System.out.println("capture metadata " + metadata.toString());
        captureMetadataTextArea.setText(metadata.toString());
    }
}
