package hr.riteh.medfileconversionjavafx.controllers;

import hr.riteh.medfileconversionjavafx.MedFileApplication;
import hr.riteh.medfileconversionjavafx.constants.SceneConstants;
import hr.riteh.medfileconversionjavafx.converters.MicroscopyHSIConverter;
import hr.riteh.medfileconversionjavafx.displayers.MicroscopyHSIDisplayer;
import hr.riteh.medfileconversionjavafx.helper.SpecimImageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MicroscopyHSIController {
    private Stage stage;
    private MicroscopyHSIConverter microscopyHSIConverter;
    private MicroscopyHSIDisplayer microscopyHSIDisplayer;
    private int numImages;
    private Map<String, String> metadataMap = new HashMap<>();

    @FXML
    private ImageView imageView;

    @FXML
    private TextField selectedSliceField;

    @FXML
    private Button selectBtn;

    @FXML
    private Slider slider;

    @FXML
    private javafx.scene.control.Label startSliceLabel;

    @FXML
    private Label endSliceLabel;

    @FXML
    private TextArea metadataTextArea;

    @FXML
    private ChoiceBox<String> datasetChoiceBox;

    @FXML
    private Label currentSlice;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setMicroscopyHSIConverter(MicroscopyHSIConverter microscopyHSIConverter) {
        this.microscopyHSIConverter = microscopyHSIConverter;
    }

    public void setMicroscopyHSIDisplayer(MicroscopyHSIDisplayer microscopyHSIDisplayer) {
        this.microscopyHSIDisplayer = microscopyHSIDisplayer;
    }

    public void setupDisplayer(MicroscopyHSIDisplayer microscopyHSIDisplayer) {
        setMicroscopyHSIDisplayer(microscopyHSIDisplayer);

        currentSlice.setText("Current slice: 0");

        numImages = microscopyHSIDisplayer.getNumDarkImages();
        metadataMap = microscopyHSIDisplayer.getDarkMetadataMap();

        startSliceLabel.setText("0");
        endSliceLabel.setText(Integer.valueOf(numImages - 1).toString());
        displayMetadata();
    }

    public void generateAndDisplayInitialImage() {
        generateAndDisplayNewImage();

        selectBtn.setDisable(false);
    }

    private void generateAndDisplayNewImage() {
        BufferedImage img = microscopyHSIDisplayer.getImg();

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
            javafx.scene.image.Image image = new Image(tempFile.toURI().toString());
            imageView.setImage(image);
        } catch (ImageWriteException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Platform.runLater(() -> currentSlice.setText("Current slice: " + microscopyHSIDisplayer.getSelectedSlice()));
    }

    @FXML
    public void initialize() {
        datasetChoiceBox.getItems().addAll("dark", "white", "green");
        datasetChoiceBox.setValue("dark");
        datasetChoiceBox.setOnAction(event -> switchDataset());
    }

    private void switchDataset() {
        String selectedDataset = datasetChoiceBox.getValue();
        microscopyHSIDisplayer.setSelectedDataset(selectedDataset);
        switch (selectedDataset) {
            case "dark":
                numImages = microscopyHSIDisplayer.getNumDarkImages();
                metadataMap = microscopyHSIDisplayer.getDarkMetadataMap();
                break;
            case "white":
                numImages = microscopyHSIDisplayer.getNumWhiteImages();
                metadataMap = microscopyHSIDisplayer.getWhiteMetadataMap();
                break;
            case "green":
                numImages = microscopyHSIDisplayer.getNumGreenImages();
                metadataMap = microscopyHSIDisplayer.getGreenMetadataMap();
                break;
        }

        updateStage();

        generateAndDisplayInitialImage();
    }

    private void updateStage() {
        startSliceLabel.setText("0");
        endSliceLabel.setText(Integer.valueOf(numImages - 1).toString());
        //slider.setMax(numImages);
        //System.out.println("Max value of slider: " + slider.getMax());
        slider.setValue(0);
        selectedSliceField.setText("");
        selectedSliceField.setPromptText("Enter selected slice");
        displayMetadata();
    }

    @FXML
    protected void onSelectBtnClick() {
        int slice = 0;
        try {
            slice = Integer.parseInt(selectedSliceField.getText());
            System.out.println("Selected slice: " + slice);
            slice = Math.min(Math.max(slice, 0), numImages - 1);
            System.out.println("Selected slice after clamping: " + slice);
        } catch (NumberFormatException e) {
//            throw new RuntimeException(e);
            selectedSliceField.setText("");
            selectedSliceField.setPromptText("Please enter a valid number");
            return;
        }
        microscopyHSIDisplayer.setSelectedSlice(slice);
        selectedSliceField.setText("");
        selectedSliceField.setPromptText("Enter selected slice");
        generateAndDisplayNewImage();

        System.out.println("Selected slice: " + slice + " out of " + numImages);
        slider.setValue((double) (slice + 1) * 100 / numImages);
    }

    @FXML
    protected void onSliderChange() {
        System.out.println("Slider value: " + slider.getValue());
        microscopyHSIDisplayer.setSelectedSlice((int) slider.getValue() * (numImages - 1) /100);
        generateAndDisplayNewImage();
    }

    @FXML
    protected void onReturnBtnClick() throws IOException {
        FXMLLoader fxmlMedFileLoader = new FXMLLoader(MedFileApplication.class.getResource("controllers/med-file-view.fxml"));

        Scene scene = new Scene(fxmlMedFileLoader.load(), SceneConstants.SCENE_WIDTH, SceneConstants.SCENE_HEIGHT);
        stage.setScene(scene);

        MedFileController medFileController = fxmlMedFileLoader.getController();
        medFileController.setStage(stage);
    }

    private void displayMetadata() {
        StringBuilder metadata = new StringBuilder();
        for (Map.Entry<String, String> entry : metadataMap.entrySet()) {
            metadata.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        metadataTextArea.setText(metadata.toString());
    }
}
