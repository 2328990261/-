package com.wangrui.springboot.controller;

import com.wangrui.springboot.service.CacheInvalidationService;
import com.wangrui.springboot.pojo.NovelBook;
import com.wangrui.springboot.pojo.NovelBookVolume;
import com.wangrui.springboot.mapper.NovelBookMainMapper;
import com.wangrui.springboot.mapper.NovelVolumeMapper;
import com.wangrui.springboot.util.Result;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class BookUploadController {
    private static final Logger log = LoggerFactory.getLogger(BookUploadController.class);

    @Value("${upload.covers-dir:}")
    private String coversDir;

    @Autowired
    private NovelBookMainMapper novelBookMainMapper;
    @Autowired
    private NovelVolumeMapper novelVolumeMapper;
    @Autowired
    private CacheInvalidationService cacheInvalidationService;
    @Autowired
    @Qualifier("uploadExecutor")
    private Executor uploadExecutor;

    /** 封面上传：保存到后端专门目录，返回文件名供数据库存储 */
    @PostMapping("/uploadCover")
    public Result<String> uploadCover(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择封面图片");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.matches("(?i).*\\.(jpg|jpeg|png|gif|webp)$")) {
            return Result.error("仅支持 jpg/png/gif/webp 格式");
        }
        try {
            String ext = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf('.')) : ".jpg";
            String saveName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path dir = Paths.get(coversDir != null && !coversDir.isEmpty() ? coversDir : System.getProperty("user.dir") + "/../uploads/covers");
            Files.createDirectories(dir);
            Path target = dir.resolve(saveName);
            file.transferTo(target.toFile());
            return Result.success(saveName);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("封面上传失败：" + e.getMessage());
        }
    }

    @PostMapping("/createMainBook")
    public Result createMainBook(@RequestBody Map<String, String> params) {
        try {
            NovelBook novelBook = new NovelBook();
            novelBook.setBookMainName(params.get("bookName"));
            novelBook.setAuthor(params.get("author"));
            novelBook.setLabel(params.get("label"));
            novelBook.setCover(params.get("cover"));
            novelBook.setReadCount(0);
            novelBook.setStatus(1);
            novelBook.setCreateTime(new Date());
            novelBookMainMapper.insertNovel(novelBook);
            cacheInvalidationService.invalidateCategories();
            return Result.success(novelBook.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建失败");
        }
    }

    @PostMapping("/uploadVolumes")
    public Result uploadVolumes(@RequestParam("files") MultipartFile[] files, @RequestParam("mainBookId") Integer mainBookId) {
        if (files == null || files.length == 0 || mainBookId == null) {
            return Result.error("请选择要上传的 EPUB 文件");
        }

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        boolean rejected = false;
        try {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;

                String originalFilename = file.getOriginalFilename();
                String volumeName = originalFilename != null
                        ? originalFilename.replace(".epub", "")
                        : "未命名分卷";
                File tempFile = null;
                try {
                    tempFile = File.createTempFile("novel-epub-", ".epub");
                    file.transferTo(tempFile);
                    File workerFile = tempFile;
                    futures.add(CompletableFuture.supplyAsync(
                            () -> processVolume(workerFile, mainBookId, volumeName), uploadExecutor));
                } catch (RejectedExecutionException e) {
                    log.warn("upload executor rejected file={} thread={} queueSaturated=true",
                            volumeName, Thread.currentThread().getName(), e);
                    deleteTempFile(tempFile, volumeName);
                    rejected = true;
                } catch (Exception e) {
                    log.warn("EPUB upload dispatch failed file={} mainBookId={} thread={}",
                            volumeName, mainBookId, Thread.currentThread().getName(), e);
                    deleteTempFile(tempFile, volumeName);
                }
            }

            if (futures.isEmpty()) {
                return Result.error("没有可处理的 EPUB 文件");
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(10, TimeUnit.MINUTES);
            long total = futures.stream().filter(CompletableFuture::join).count();
            if (total > 0) {
                cacheInvalidationService.invalidateBookDetail(mainBookId);
            }
            if (rejected) {
                return Result.error("上传队列已满，请稍后重试");
            }
            return Result.success("成功上传 " + total + " 个分卷");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("上传被中断");
        } catch (TimeoutException e) {
            log.warn("upload processing timed out mainBookId={} thread={}",
                    mainBookId, Thread.currentThread().getName(), e);
            return Result.error("上传处理超时");
        } catch (ExecutionException e) {
            log.warn("upload processing failed mainBookId={} thread={}",
                    mainBookId, Thread.currentThread().getName(), e);
            return Result.error("上传失败");
        } catch (Exception e) {
            log.warn("upload failed mainBookId={} thread={}",
                    mainBookId, Thread.currentThread().getName(), e);
            return Result.error("上传失败");
        }
    }

    private boolean processVolume(File tempFile, Integer mainBookId, String volumeName) {
        try (InputStream inputStream = Files.newInputStream(tempFile.toPath())) {
            log.info("EPUB processing started file={} mainBookId={} thread={}",
                    volumeName, mainBookId, Thread.currentThread().getName());
            Map<String, byte[]> contents = extractEpubContents(inputStream);
            StringBuilder fullContent = new StringBuilder();
            for (String contentFile : extractContentFiles(contents)) {
                byte[] data = contents.get(contentFile);
                if (data != null) {
                    String content = cleanHtmlContent(new String(data, StandardCharsets.UTF_8));
                    if (!content.trim().isEmpty()) fullContent.append(content).append("\n\n");
                }
            }
            if (fullContent.length() > 0) {
                NovelBookVolume volume = new NovelBookVolume();
                volume.setMainBookId(mainBookId);
                volume.setVolumeName(volumeName);
                volume.setContent(fullContent.toString().trim());
                volume.setCreateTime(new Date());
                int insertedRows = novelVolumeMapper.insertVolume(volume);
                if (insertedRows <= 0) {
                    log.warn("EPUB volume insert returned no rows file={} mainBookId={} volumeId={} thread={}",
                            volumeName, mainBookId, volume.getId(), Thread.currentThread().getName());
                    return false;
                }
                log.info("EPUB processing completed file={} mainBookId={} volumeId={} insertedRows={} thread={}",
                        volumeName, mainBookId, volume.getId(), insertedRows, Thread.currentThread().getName());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("EPUB file processing failed file={} thread={}",
                    volumeName, Thread.currentThread().getName(), e);
            return false;
        } finally {
            deleteTempFile(tempFile, volumeName);
        }
    }

    private void deleteTempFile(File tempFile, String volumeName) {
        if (tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile.toPath());
        } catch (IOException cleanupException) {
            log.warn("EPUB temporary file cleanup failed file={} thread={}",
                    volumeName, Thread.currentThread().getName(), cleanupException);
        }
    }

    private Map<String, byte[]> extractEpubContents(InputStream inputStream) throws Exception {
        Map<String, byte[]> contents = new HashMap<>();
        try (ZipArchiveInputStream zipInput = new ZipArchiveInputStream(inputStream, "UTF-8")) {
            ZipArchiveEntry entry;
            while ((entry = zipInput.getNextZipEntry()) != null) {
                if (!entry.isDirectory()) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zipInput.read(buffer)) != -1) output.write(buffer, 0, len);
                    contents.put(entry.getName(), output.toByteArray());
                }
            }
        }
        return contents;
    }

    private List<String> extractContentFiles(Map<String, byte[]> contents) {
        List<String> files = new ArrayList<>();
        for (String key : contents.keySet()) {
            if (key.endsWith(".html") || key.endsWith(".xhtml") || key.endsWith(".htm")) files.add(key);
        }
        Collections.sort(files);
        return files;
    }

    private String cleanHtmlContent(String html) {
        String text = html.replaceAll("<script[^>]*>.*?</script>", "").replaceAll("<style[^>]*>.*?</style>", "").replaceAll("<[^>]+>", "");
        text = text.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").replace("&quot;", "\"");
        return text.replaceAll("\\s+", " ").trim();
    }
}