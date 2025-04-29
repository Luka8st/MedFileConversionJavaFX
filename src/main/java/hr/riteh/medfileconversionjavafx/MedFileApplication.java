package hr.riteh.medfileconversionjavafx;

import hr.riteh.medfileconversionjavafx.constants.SceneConstants;
import hr.riteh.medfileconversionjavafx.controllers.MedFileController;
import hr.riteh.medfileconversionjavafx.converters.LaboratoryHSIConverter;
import hr.riteh.medfileconversionjavafx.displayers.LaboratoryHSIDisplayer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

public class MedFileApplication extends Application {
    private static String loadDirectory;
    private static String displayFile;
    private static String imageFormat;
    private static String displayImageFormat;


    public static String getLoadDirectory() {
        return loadDirectory;
    }

    public static String getDisplayFile() {
        return displayFile;
    }

    public static String getImageFormat() {
        return imageFormat;
    }

    public static String getDisplayImageFormat() {
        return displayImageFormat;
    }

    public static void setLoadDirectory(String loadDir) {
        loadDirectory = loadDir;
    }

    public static void setDisplayFile(String displayFile) {
        displayFile = displayFile;
    }

    public static void setImageFormat(String imageFormat) {
        MedFileApplication.imageFormat = imageFormat;
    }

    public static void setDisplayImageFormat(String displayImageFormat) {
        MedFileApplication.displayImageFormat = displayImageFormat;
    }

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader fxmlLoader = new FXMLLoader(MedFileApplication.class.getResource("controllers/med-file-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), SceneConstants.SCENE_WIDTH, SceneConstants.SCENE_HEIGHT);

        MedFileController medFileController = fxmlLoader.getController();
        medFileController.setStage(stage);

        stage.setTitle("MedFileConversionJavaFX");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}