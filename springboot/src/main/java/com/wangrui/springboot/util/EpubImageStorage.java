package com.wangrui.springboot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * EPUB 插画落盘目录管理：uploads/epub-images/{主卷ID}/{分卷UUID}/
 * 负责目录解析与整卷删除（删除分卷时同步清理磁盘，避免孤儿文件）。
 */
@Component
public class EpubImageStorage {
    private static final Logger log = LoggerFactory.getLogger(EpubImageStorage.class);
    private final String baseDir;

    public EpubImageStorage(@Value("${upload.epub-images-dir:}") String baseDir) {
        this.baseDir = (baseDir != null && !baseDir.isEmpty())
                ? baseDir
                : System.getProperty("user.dir") + "/../uploads/epub-images";
    }

    public String getBaseDir() {
        return baseDir;
    }

    public Path volumeDir(Integer mainBookId, String volumeUuid) {
        return Paths.get(baseDir, String.valueOf(mainBookId), volumeUuid);
    }

    public void deleteVolumeDir(Integer mainBookId, String volumeUuid) {
        if (mainBookId == null || volumeUuid == null || volumeUuid.isEmpty()) return;
        try {
            Path dir = volumeDir(mainBookId, volumeUuid);
            if (dir == null || !Files.exists(dir)) return;
            try (var stream = Files.walk(dir)) {
                stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        log.warn("delete epub image file failed path={}", p, e);
                    }
                });
            }
        } catch (IOException e) {
            log.warn("delete epub image dir failed mainBookId={} volumeUuid={}", mainBookId, volumeUuid, e);
        }
    }
}
