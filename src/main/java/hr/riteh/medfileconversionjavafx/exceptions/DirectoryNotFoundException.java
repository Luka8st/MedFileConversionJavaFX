package hr.riteh.medfileconversionjavafx.exceptions;

import java.io.IOException;

public class DirectoryNotFoundException extends IOException {
    public DirectoryNotFoundException() {
        super();
    }

    public DirectoryNotFoundException(String s) {
        super(s);
    }
}
