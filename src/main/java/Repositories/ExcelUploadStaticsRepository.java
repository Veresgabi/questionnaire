package Repositories;

import Models.ExcelUploadStatics;
import Utils.Enums;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ExcelUploadStaticsRepository extends CrudRepository<ExcelUploadStatics, Long> {

    ExcelUploadStatics findFirstByTypeOfUpload(Enums.ExcelUploadType type);

    @Query("SELECT s FROM ExcelUploadStatics s where s.typeOfUpload = ?1")
    List<ExcelUploadStatics> findAllByTypeOfUpload(Enums.ExcelUploadType typeOfUpload);

    @Query("SELECT s FROM ExcelUploadStatics s where s.typeOfUpload = ?1 and s.updatedBy = ?2")
    List<ExcelUploadStatics> findAllByTypeOfUpload(Enums.ExcelUploadType typeOfUpload, Enums.Role role);

    @Modifying
    void deleteAll();
}
