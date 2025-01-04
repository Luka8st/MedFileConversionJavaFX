package hr.riteh.medfileconversionjavafx.helper;

public enum SpecimImageType {
    NORMAL("rawData", new int[3]),
    DARKREF("darkRef", new int[3]),
    WHITEREF("whiteRef", new int[3]),
    WHITEDARKREF("whiteDarkRef", new int[3]);

    private final String datasetName;
    private int[] dimensions;

    SpecimImageType(String datasetName, int[] dimensions) {
        this.datasetName = datasetName;
        this.dimensions = dimensions;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public int[] getDimensions() {
        return dimensions;
    }

    public void setDimensions(int[] dimensions) {
        this.dimensions = dimensions;
    }
}