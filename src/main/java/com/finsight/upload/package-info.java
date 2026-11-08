/**
 * 업로드 모듈. 파일 저장, 업로드 이력과 처리 상태 관리를 맡는다.
 *
 * <p>공개 API: {@link com.finsight.upload.UploadTracker}, {@link com.finsight.upload.UploadType}
 * <br>발행 이벤트: {@link com.finsight.upload.VideoUploadedEvent} (visitor 모듈이 받아 영상 분석을 시작한다)
 */
@ApplicationModule(displayName = "업로드", allowedDependencies = {"global", "store"})
package com.finsight.upload;

import org.springframework.modulith.ApplicationModule;
