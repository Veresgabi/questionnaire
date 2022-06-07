package Models;

import Utils.Enums;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "ExcelUploadStatics")
public class ExcelUploadStatics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "lastUploadedFile", nullable = false)
    private String lastUploadedFile;

    @Column(name = "lastUpload", nullable = false)
    private LocalDateTime lastUpload;

    @Column(name = "updatedBy")
    private Enums.Role updatedBy;

    @Transient
    private String formattedLastUpdate;

    @Column(name = "uploadType", nullable = false)
    private Enums.ExcelUploadType typeOfUpload;

    @Transient
    private Integer numberOfActiveElements;

    @Transient
    private Integer numberOfInactiveElements;

    public ExcelUploadStatics(Long id, String lastUploadedFile, LocalDateTime lastUpload, String formattedLastUpdate,
                              Enums.Role updatedBy, Enums.ExcelUploadType typeOfUpload, Integer numberOfActiveElements,
                              Integer numberOfInactiveElements) {
        this.id = id;
        this.lastUploadedFile = lastUploadedFile;
        this.lastUpload = lastUpload;
        this.formattedLastUpdate = formattedLastUpdate;
        this.updatedBy = updatedBy;
        this.typeOfUpload = typeOfUpload;
        this.numberOfActiveElements = numberOfActiveElements;
        this.numberOfInactiveElements = numberOfInactiveElements;
    }

    public ExcelUploadStatics() {}

    @Transient
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLastUploadedFile() {
        return lastUploadedFile;
    }

    public void setLastUploadedFile(String lastUploadedFile) {
        this.lastUploadedFile = lastUploadedFile;
    }

    public String getFormattedLastUpdate() {
        if (lastUpload != null) this.formattedLastUpdate = lastUpload.format(formatter);
        return formattedLastUpdate;
    }

    public void setFormattedLastUpdate(String formattedLastUpdate) {
        this.formattedLastUpdate = formattedLastUpdate;
    }

    public LocalDateTime getLastUpload() {
        return lastUpload;
    }

    public void setLastUpload(LocalDateTime lastUpload) {
        this.lastUpload = lastUpload;
    }

    public Enums.Role getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Enums.Role updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Enums.ExcelUploadType getTypeOfUpload() {
        return typeOfUpload;
    }

    public void setTypeOfUpload(Enums.ExcelUploadType typeOfUpload) {
        this.typeOfUpload = typeOfUpload;
    }

    public Integer getNumberOfActiveElements() {
        return numberOfActiveElements;
    }

    public void setNumberOfActiveElements(Integer numberOfActiveElements) {
        this.numberOfActiveElements = numberOfActiveElements;
    }

    public Integer getNumberOfInactiveElements() {
        return numberOfInactiveElements;
    }

    public void setNumberOfInactiveElements(Integer numberOfInactiveElements) {
        this.numberOfInactiveElements = numberOfInactiveElements;
    }
}
