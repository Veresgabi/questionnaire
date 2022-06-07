package DTOs;

import Models.ExcelUploadStatics;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public class ExcelDTO extends AbstractDTO {

    private MultipartFile file;

    private ExcelUploadStatics statics;

    private List<ExcelUploadStatics> staticsList;

    public ExcelDTO(MultipartFile file, ExcelUploadStatics statics, List<ExcelUploadStatics> staticsList) {
        this.file = file;
        this.statics = statics;
        this.staticsList = staticsList;
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

    public List<ExcelUploadStatics> getStaticsList() {
        return staticsList;
    }

    public void setStaticsList(List<ExcelUploadStatics> staticsList) {
        this.staticsList = staticsList;
    }
}
