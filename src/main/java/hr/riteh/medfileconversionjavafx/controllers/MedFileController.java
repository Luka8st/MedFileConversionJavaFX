package hr.riteh.medfileconversionjavafx.controllers;

import hr.riteh.medfileconversionjavafx.displayers.LaboratoryHSIDisplayer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class MedFileController {
    @FXML
    private Label welcomeText;

    @FXML
    private ImageView imageView;

    private LaboratoryHSIDisplayer laboratoryHSIDisplayer;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");

        // Load and display the initial image
        InputStream inputStream = getClass().getResourceAsStream("/hr/riteh/medfileconversionjavafx/controllers/img/sample1.jfif");
        if (inputStream == null) {
            System.out.println("Image resource not found.");
        } else {
            System.out.println("Image resource found.");
            imageView.setImage(new Image(inputStream));
        }

        // Generate the BufferedImage using the laboratoryHSIDisplayer.paint() method
        BufferedImage img = laboratoryHSIDisplayer.paint();

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
    }

    public void setLaboratoryHSIDisplayer(LaboratoryHSIDisplayer laboratoryHSIDisplayer) {
        this.laboratoryHSIDisplayer = laboratoryHSIDisplayer;
    }
}