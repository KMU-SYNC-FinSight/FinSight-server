package com.finsight.upload.repository;

import com.finsight.upload.domain.DataUpload;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataUploadRepository extends JpaRepository<DataUpload, Long> {
}