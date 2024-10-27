package hr.riteh.medfileconversionjavafx.helper;

public enum Dimension {
    WAVELENGTHS, POSITIONS, SAMPLES;

    public String singular() {
        if (this.equals(Dimension.POSITIONS)) return "position";
        if (this.equals(Dimension.WAVELENGTHS)) return "wavelength";
        return "sample";
    }
}
