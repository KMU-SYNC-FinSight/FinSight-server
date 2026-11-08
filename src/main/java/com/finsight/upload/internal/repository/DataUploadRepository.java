package com.finsight.upload.internal.repository;

import com.finsight.upload.internal.domain.DataUpload;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataUploadRepository extends JpaRepository<DataUpload, Long> {
}