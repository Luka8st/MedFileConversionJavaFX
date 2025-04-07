package hr.riteh.medfileconversionjavafx.controllers;

import hr.riteh.medfileconversionjavafx.converters.MicroscopyHSIConverter;
import hr.riteh.medfileconversionjavafx.displayers.MicroscopyHSIDisplayer;
import javafx.stage.Stage;

public class MicroscopyHSIController {
    private Stage stage;
    private MicroscopyHSIConverter microscopyHSIConverter;
    private MicroscopyHSIDisplayer microscopyHSIDisplayer;

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
    }
}
