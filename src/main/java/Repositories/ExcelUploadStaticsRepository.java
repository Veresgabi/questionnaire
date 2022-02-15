package Repositories;

import Models.ExcelUploadStatics;
import Utils.Enums;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ExcelUploadStaticsRepository extends CrudRepository<ExcelUploadStatics, Long> {

    ExcelUploadStatics findFirstByTypeOfUpload(Enums.ExcelUploadType type);
}
