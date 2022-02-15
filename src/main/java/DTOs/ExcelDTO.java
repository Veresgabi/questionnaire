package DTOs;

import Models.ExcelUploadStatics;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public class ExcelDTO extends AbstractDTO {

    private MultipartFile file;

    private ExcelUploadStatics statics;

    public ExcelDTO(MultipartFile file, ExcelUploadStatics statics) {
        this.file = file;
        this.statics = statics;
    }

    public ExcelDTO() { }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public ExcelUploadStatics getStatics() {
        return statics;
    }

    public void setStatics(ExcelUploadStatics statics) {
        this.statics = statics;
    }
}
