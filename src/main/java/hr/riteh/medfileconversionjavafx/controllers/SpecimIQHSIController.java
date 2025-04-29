package hr.riteh.medfileconversionjavafx.controllers;

import hr.riteh.medfileconversionjavafx.MedFileApplication;
import hr.riteh.medfileconversionjavafx.constants.SceneConstants;
import hr.riteh.medfileconversionjavafx.displayers.SpecimIQHSIDisplayer;
import hr.riteh.medfileconversionjavafx.helper.Dimension;
import hr.riteh.medfileconversionjavafx.helper.SpecimImageType;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.embed.swing.SwingFXUtils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
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

    /*@FXML
    private Button nextBtn;

    @FXML
    private Button prevBtn;

    @FXML
    private Button slideBtn;*/

    @FXML
    private ChoiceBox<String> pngChoiceBox;

    @FXML
    private ImageView pngImageView;

    @FXML
    public TextArea resultMetadataTextArea;

    @FXML
    public TextArea captureMetadataTextArea;

    @FXML
    public Slider slider;

    @FXML
    public Button selectBtn;

    @FXML
    public TextField selectedSliceField;

    @FXML
    public Label currentSlice;

    @FXML
    private Label startSliceLabel;

    @FXML
    private Label endSliceLabel;

    private SpecimIQHSIDisplayer specimIQHSIDisplayer;
    private Timeline slideshowTimeline;
    private static final Set<String> EXCLUDED_KEYS_FOR_DISPLAY = Set.of("wavelength", "wavelengths");
    private Stage stage;

    private String[] wavelengths;

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

        updateStage(imageType.getDimensions()[0]);

        generateAndDisplayInitialImage();
    }

    private void updateStage(int numImages) {
        startSliceLabel.setText("0");
        endSliceLabel.setText(Integer.valueOf(numImages - 1).toString());
        //slider.setMax(numImages);
        //System.out.println("Max value of slider: " + slider.getMax());
        slider.setValue(0);
        selectedSliceField.setText("");
        selectedSliceField.setPromptText("Enter selected slice");

        if (numImages > 1) {
            slider.setDisable(false);
            selectBtn.setDisable(false);
            selectedSliceField.setDisable(false);
        } else {
            slider.setDisable(true);
            selectBtn.setDisable(true);
            selectedSliceField.setDisable(true);
        }
    }

    private void switchPng() {
        generateAndDisplayPngImage();
    }

    public void setSpecimIQHSIDisplayer(SpecimIQHSIDisplayer specimIQHSIDisplayer) {
        this.specimIQHSIDisplayer = specimIQHSIDisplayer;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void generateAndDisplayNewImage() {
        BufferedImage img = specimIQHSIDisplayer.getImg();

        // Convert BufferedImage to WritableImage
        WritableImage writableImage = SwingFXUtils.toFXImage(img, null);

        // Set the WritableImage to the ImageView
        imageView.setImage(writableImage);

        /*Platform.runLater(() -> currentSlice.setText("Current slice: " + specimIQHSIDisplayer.getSelectedSlice() +
                "; wavelength = " + wavelengths[specimIQHSIDisplayer.getSelectedSlice()] + " nm " + wavelengths.length))*/

        Platform.runLater(() -> currentSlice.setText("Current slice: " + specimIQHSIDisplayer.getSelectedSlice()));
    }

    @FXML
    protected void onSliderChange() {
        specimIQHSIDisplayer.setSelectedSlice((int) slider.getValue() * (specimIQHSIDisplayer.getLines()-1)/100);
        generateAndDisplayNewImage();
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

    @FXML
    protected void onSelectBtnClick() {
        int slice = 0;
        try {
            slice = Integer.parseInt(selectedSliceField.getText());
        } catch (NumberFormatException e) {
//            throw new RuntimeException(e);
            selectedSliceField.setText("");
            selectedSliceField.setPromptText("Please enter a valid number");
            return;
        }
        specimIQHSIDisplayer.setSelectedSlice(slice);
        selectedSliceField.setText("");
        selectedSliceField.setPromptText("Enter selected slice");
        generateAndDisplayNewImage();
        slider.setValue((double) (slice + 1) * 100 / specimIQHSIDisplayer.getLines());
    }

    @FXML
    protected void onReturnBtnClick() throws IOException {
        FXMLLoader fxmlMedFileLoader = new FXMLLoader(MedFileApplication.class.getResource("controllers/med-file-view.fxml"));

        Scene scene = new Scene(fxmlMedFileLoader.load(), SceneConstants.SCENE_WIDTH, SceneConstants.SCENE_HEIGHT);
        stage.setScene(scene);

        MedFileController medFileController = fxmlMedFileLoader.getController();
        medFileController.setStage(stage);
    }


    private void startSlideshow() {
        /*slideBtn.setText("Stop Slideshow");
        prevBtn.setDisable(true);
        nextBtn.setDisable(true);*/
        datasetChoiceBox.setDisable(true);

        slideshowTimeline = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            specimIQHSIDisplayer.incrementSelectedSlice();
            generateAndDisplayNewImage();
        }));
        slideshowTimeline.setCycleCount(Timeline.INDEFINITE);
        slideshowTimeline.play();
    }

    private void stopSlideshow() {
        /*slideBtn.setText("Start Slideshow");
        prevBtn.setDisable(false);
        nextBtn.setDisable(false);*/
        datasetChoiceBox.setDisable(false);

        if (slideshowTimeline != null) {
            slideshowTimeline.stop();
        }
    }

    private void updateButtonState() {
        boolean hasMultipleLines = specimIQHSIDisplayer.hasMultipleLines();
        /*nextBtn.setDisable(!hasMultipleLines);
        prevBtn.setDisable(!hasMultipleLines);
        slideBtn.setDisable(!hasMultipleLines);*/
    }

    public void setupDisplayer(SpecimIQHSIDisplayer specimIQHSIDisplayer) {
        setSpecimIQHSIDisplayer(specimIQHSIDisplayer);

        specimIQHSIDisplayer.setCurrentDataset(SpecimImageType.NORMAL.getDatasetName());
        specimIQHSIDisplayer.setDimensions(SpecimImageType.NORMAL.getDimensions());
        updateButtonState();

        displayCaptureMetadata();
        displayResultMetadata();
        wavelengths = specimIQHSIDisplayer.getWavelengths();

        startSliceLabel.setText("0");
        endSliceLabel.setText(Integer.valueOf(specimIQHSIDisplayer.getLines() - 1).toString());
    }

    public void generateAndDisplayInitialImage() {
        generateAndDisplayNewImage();
        updateButtonState();

        generateAndDisplayPngImage();
    }

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
        System.out.println("capture metadata " + metadata);
        captureMetadataTextArea.setText(metadata.toString());
    }
}
