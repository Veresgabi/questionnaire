package DTOs;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public class ExcelDTO extends AbstractDTO {

    private MultipartFile file;

    public ExcelDTO(MultipartFile file) {
        this.file = file;
    }

    public ExcelDTO() { }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

}
