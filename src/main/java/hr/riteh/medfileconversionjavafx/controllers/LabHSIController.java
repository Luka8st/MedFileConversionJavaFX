package hr.riteh.medfileconversionjavafx.controllers;

import hr.riteh.medfileconversionjavafx.displayers.LaboratoryHSIDisplayer;
import hr.riteh.medfileconversionjavafx.helper.Dimension;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class LabHSIController {
    @FXML
    private Label currentSlice;

    @FXML
    private ImageView imageView;

    @FXML
    private TextField selectedSliceField;

    @FXML
    private Label selectedDimLabel;

    @FXML
    private Label xAxisDimLabel;

    @FXML
    private Label yAxisDimLabel;

    @FXML
    private ChoiceBox dimensionBox;

    @FXML
    private Button nextBtn;

    @FXML
    private Button prevBtn;

    @FXML
    private Button selectBtn;

    @FXML
    private Button slideBtn;

    @FXML
    private Button slideStopBtn;

    @FXML
    private TextArea metadataTextArea;

    private LaboratoryHSIDisplayer laboratoryHSIDisplayer;
    private boolean slideshowActive;
    private static final Set<String> EXCLUDED_KEYS_FOR_DISPLAY = Set.of("wavelength");

    @FXML
    public void initialize() {
        dimensionBox.setValue(Dimension.WAVELENGTHS);
        dimensionBox.setItems(FXCollections.observableArrayList(Dimension.values()));
    }

    public void generateAndDisplayInitialImage() {
        generateAndDisplayNewImage();

        slideBtn.setDisable(false);
        slideStopBtn.setDisable(true);
        prevBtn.setDisable(false);
        nextBtn.setDisable(false);
        selectBtn.setDisable(false);
    }

    private void generateAndDisplayNewImage() {
//        slideBtn.setDisable(true);
//        slideStopBtn.setDisable(false);
//        prevBtn.setDisable(true);
//        nextBtn.setDisable(true);
//        selectBtn.setDisable(true);

        // Generate the BufferedImage using the laboratoryHSIDisplayer.paint() method
        BufferedImage img = laboratoryHSIDisplayer.getImg();

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

        Platform.runLater(() -> currentSlice.setText("Current slice: " + laboratoryHSIDisplayer.getSelectedSlice()));
//        slideBtn.setDisable(false);
//        slideStopBtn.setDisable(true);
//        prevBtn.setDisable(false);
//        nextBtn.setDisable(false);
//        selectBtn.setDisable(false);
    }
    @FXML
    protected void onNextBtnClick() {
        laboratoryHSIDisplayer.incrementSelectedSlice();

        Task<Void> displayTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                nextBtn.setDisable(true);
                prevBtn.setDisable(true);
                selectBtn.setDisable(true);
                slideBtn.setDisable(true);
                selectedSliceField.setDisable(true);
                slideStopBtn.setDisable(true);

                generateAndDisplayNewImage();

                nextBtn.setDisable(false);
                prevBtn.setDisable(false);
                selectBtn.setDisable(false);
                slideBtn.setDisable(false);
                selectedSliceField.setDisable(false);
                return null;
            }
        };
        new Thread(displayTask).start();
    }

    @FXML
    protected void onPrevBtnClick() {
        laboratoryHSIDisplayer.decrementSelectedSlice();

        Task<Void> displayTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                nextBtn.setDisable(true);
                prevBtn.setDisable(true);
                selectBtn.setDisable(true);
                slideBtn.setDisable(true);
                selectedSliceField.setDisable(true);
                slideStopBtn.setDisable(true);

                generateAndDisplayNewImage();

                nextBtn.setDisable(false);
                prevBtn.setDisable(false);
                selectBtn.setDisable(false);
                slideBtn.setDisable(false);
                selectedSliceField.setDisable(false);
                return null;
            }
        };
        new Thread(displayTask).start();
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
        laboratoryHSIDisplayer.setSelectedSlice(slice);
        selectedSliceField.setText("");
        selectedSliceField.setPromptText("Enter selected slice");
        generateAndDisplayNewImage();
    }

    @FXML
    protected void onSlideshowBtnClick() {
        slideshowActive = true;
        Task<Void> slideshowTask = new Task<>() {
            @Override
            protected Void call() throws InterruptedException {
                slideBtn.setDisable(true);
                slideStopBtn.setDisable(false);
                prevBtn.setDisable(true);
                nextBtn.setDisable(true);
                selectBtn.setDisable(true);

                while (slideshowActive) {
                    Platform.runLater(() -> {
                        laboratoryHSIDisplayer.incrementSelectedSlice();
                        generateAndDisplayNewImage();
                    });
                    Thread.sleep(500);  // Delay of 500 milliseconds
                }

                slideBtn.setDisable(false);
                slideStopBtn.setDisable(true);
                prevBtn.setDisable(false);
                nextBtn.setDisable(false);
                selectBtn.setDisable(false);
                return null;
            }
        };

        // Run the task on a separate thread
//        new Thread(slideshowTask).start();

        Thread thread = new Thread(slideshowTask);
        thread.setDaemon(true); // Optional: Allows the thread to exit when the application exits
        thread.start();
    }

    @FXML
    protected void onSlideshowStopBtnClick() {
        slideshowActive = false;
    }

    @FXML
    protected void onDimChoiceMade() {
        if (laboratoryHSIDisplayer == null) {
            System.err.println("LaboratoryHSIDisplayer is not initialized.");
            return; // Early return to avoid NullPointerException
        }

        String selectedDim = dimensionBox.getValue().toString();
        System.out.println(selectedDim.equalsIgnoreCase(laboratoryHSIDisplayer.getSelectedDimension().toString()));
        if (!selectedDim.equalsIgnoreCase(laboratoryHSIDisplayer.getSelectedDimension().toString())) {
//
//            switch (selectedDim) {
//                case "POSITIONS": {
//                    laboratoryHSIDisplayer.setSelectedDimension(Dimension.POSITIONS);
//                    laboratoryHSIDisplayer.setFirstDimension(Dimension.WAVELENGTHS);
//                    laboratoryHSIDisplayer.setSecondDimension(Dimension.SAMPLES);
//
//                    laboratoryHSIDisplayer.setNumSelectedDimension(400);
//                    laboratoryHSIDisplayer.setNumFirstDimension(2048);
//                    laboratoryHSIDisplayer.setNumSecondDimension(1224);
//
//                    selectedDimLabel.setText("Selected dimension: " + laboratoryHSIDisplayer.getSelectedDimension().toString().toLowerCase());
//                    xAxisDimLabel.setText("X-axis: " + laboratoryHSIDisplayer.getFirstDimension().toString().toLowerCase());
//                    yAxisDimLabel.setText("Y-axis: " + laboratoryHSIDisplayer.getSecondDimension().toString().toLowerCase());
//
//                    laboratoryHSIDisplayer.setMemoryDims(new long[]{1, 2048, 1224});
//                    laboratoryHSIDisplayer.setBlock(new long[]{1, 2048, 1224});
//                    generateAndDisplayInitialImage();
//                    break;
//                }
//
//                case "SAMPLES": {
//                    laboratoryHSIDisplayer.setSelectedDimension(Dimension.SAMPLES);
//                    laboratoryHSIDisplayer.setFirstDimension(Dimension.POSITIONS);
//                    laboratoryHSIDisplayer.setSecondDimension(Dimension.WAVELENGTHS);
//
//                    laboratoryHSIDisplayer.setNumSelectedDimension(1224);
//                    laboratoryHSIDisplayer.setNumFirstDimension(400);
//                    laboratoryHSIDisplayer.setNumSecondDimension(2048);
//
//                    selectedDimLabel.setText("Selected dimension: " + laboratoryHSIDisplayer.getSelectedDimension().toString().toLowerCase());
//                    xAxisDimLabel.setText("X-axis: " + laboratoryHSIDisplayer.getFirstDimension().toString().toLowerCase());
//                    yAxisDimLabel.setText("Y-axis: " + laboratoryHSIDisplayer.getSecondDimension().toString().toLowerCase());
//
//                    laboratoryHSIDisplayer.setMemoryDims(new long[]{400, 2048, 1});
//                    laboratoryHSIDisplayer.setBlock(new long[]{400, 2048, 1});
//                    generateAndDisplayInitialImage();
//                    break;
//                }
//
//                case "WAVELENGTHS": {
//                    laboratoryHSIDisplayer.setSelectedDimension(Dimension.WAVELENGTHS);
//                    laboratoryHSIDisplayer.setFirstDimension(Dimension.POSITIONS);
//                    laboratoryHSIDisplayer.setSecondDimension(Dimension.SAMPLES);
//
//                    laboratoryHSIDisplayer.setNumSelectedDimension(2048);
//                    laboratoryHSIDisplayer.setNumFirstDimension(400);
//                    laboratoryHSIDisplayer.setNumSecondDimension(1224);
//
//                    selectedDimLabel.setText("Selected dimension: " + laboratoryHSIDisplayer.getSelectedDimension().toString().toLowerCase());
//                    xAxisDimLabel.setText("X-axis: " + laboratoryHSIDisplayer.getFirstDimension().toString().toLowerCase());
//                    yAxisDimLabel.setText("Y-axis: " + laboratoryHSIDisplayer.getSecondDimension().toString().toLowerCase());
//
//                    laboratoryHSIDisplayer.setMemoryDims(new long[]{400, 1, 1224});
//                    laboratoryHSIDisplayer.setBlock(new long[]{400, 1, 1224});
//                    generateAndDisplayInitialImage();
//                    break;
//                }
//            }

            switch (selectedDim) {
                case "POSITIONS" -> laboratoryHSIDisplayer.resetDisplayerOnSelectedDim(Dimension.POSITIONS, Dimension.WAVELENGTHS, Dimension.SAMPLES);
                case "WAVELENGTHS" -> laboratoryHSIDisplayer.resetDisplayerOnSelectedDim(Dimension.WAVELENGTHS, Dimension.POSITIONS, Dimension.SAMPLES);
                case "SAMPLES" -> laboratoryHSIDisplayer.resetDisplayerOnSelectedDim(Dimension.SAMPLES, Dimension.POSITIONS, Dimension.WAVELENGTHS);
            }

            selectedDimLabel.setText("Selected dimension: " + laboratoryHSIDisplayer.getSelectedDimension().toString().toLowerCase());
            xAxisDimLabel.setText("X-axis: " + laboratoryHSIDisplayer.getFirstDimension().toString().toLowerCase());
            yAxisDimLabel.setText("Y-axis: " + laboratoryHSIDisplayer.getSecondDimension().toString().toLowerCase());

//            generateAndDisplayInitialImage();
            Task<Void> displayTask = new Task<>() {
                @Override
                protected Void call() {
                    nextBtn.setDisable(true);
                    prevBtn.setDisable(true);
                    selectBtn.setDisable(true);
                    slideBtn.setDisable(true);
                    selectedSliceField.setDisable(true);

                    generateAndDisplayInitialImage();

                    nextBtn.setDisable(false);
                    prevBtn.setDisable(false);
                    selectBtn.setDisable(false);
                    slideBtn.setDisable(false);
                    selectedSliceField.setDisable(false);
                    return null;
                }
            };
            new Thread(displayTask).start();
        }
    }

    public void setupDisplayer(LaboratoryHSIDisplayer laboratoryHSIDisplayer) {
        setLaboratoryHSIDisplayer(laboratoryHSIDisplayer);
        selectedDimLabel.setText("Selected dimension: " + laboratoryHSIDisplayer.getSelectedDimension().toString().toLowerCase());
        xAxisDimLabel.setText("X-axis: " + laboratoryHSIDisplayer.getFirstDimension().toString().toLowerCase());
        yAxisDimLabel.setText("Y-axis: " + laboratoryHSIDisplayer.getSecondDimension().toString().toLowerCase());
        displayMetadata();
    }

    public void setLaboratoryHSIDisplayer(LaboratoryHSIDisplayer laboratoryHSIDisplayer) {
        this.laboratoryHSIDisplayer = laboratoryHSIDisplayer;
    }

    private void displayMetadata() {
        StringBuilder metadata = new StringBuilder();
        for (Map.Entry<String, String> entry : laboratoryHSIDisplayer.getMetadataMap().entrySet()) {
            if (!EXCLUDED_KEYS_FOR_DISPLAY.contains(entry.getKey())) {
                metadata.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        metadataTextArea.setText(metadata.toString());
    }
}