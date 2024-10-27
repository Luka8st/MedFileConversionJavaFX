package hr.riteh.medfileconversionjavafx;

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
    @Override
    public void start(Stage stage) throws Exception {

        LaboratoryHSIConverter laboratoryHSIConverter = new LaboratoryHSIConverter();
        laboratoryHSIConverter.run();

        FXMLLoader fxmlLoader = new FXMLLoader(MedFileApplication.class.getResource("controllers/med-file-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);

        LaboratoryHSIDisplayer laboratoryHSIDisplayer = new LaboratoryHSIDisplayer(
                laboratoryHSIConverter.getDataMap(),
                laboratoryHSIConverter.getPositions(),
                laboratoryHSIConverter.getWavelengths(),
                scene
        );

        MedFileController controller = fxmlLoader.getController();
        controller.setLaboratoryHSIDisplayer(laboratoryHSIDisplayer);


        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}