package sdis.Storage;

import java.io.File;
import java.io.IOException;

public class FileTooLargeException extends IOException {
    public FileTooLargeException(File file) {
        super(file.getAbsolutePath());
    }
}
