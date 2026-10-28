package com.wangrui.springboot.controller;

import com.wangrui.springboot.mapper.NovelBookMainMapper;
import com.wangrui.springboot.mapper.NovelIllustrationMapper;
import com.wangrui.springboot.mapper.NovelVolumeMapper;
import com.wangrui.springboot.pojo.NovelBook;
import com.wangrui.springboot.pojo.NovelBookIllustration;
import com.wangrui.springboot.pojo.NovelBookVolume;
import com.wangrui.springboot.cache.CacheInvalidationService;
import com.wangrui.springboot.util.EpubImageStorage;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 后台书籍上传控制器
 * 负责解析并导入 EPUB/压缩包小说，并存储封面与插画资源，路径前缀：/api/admin
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class BookUploadController {
    private static final Logger log = LoggerFactory.getLogger(BookUploadController.class);

    /** 匹配 <img ...> 标签 */
    private static final Pattern IMG_TAG = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    /** 匹配 img 标签内的 src 属性 */
    private static final Pattern SRC_ATTR = Pattern.compile("\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    /** 正文中的插画占位标记前缀：[插图n]，n 为插画在分卷内的出现顺序 */
    private static final String ILLUS_MARKER = "[插图";

    @Value("${upload.covers-dir:}")
    private String coversDir;

    @Autowired
    private NovelBookMainMapper novelBookMainMapper;
    @Autowired
    private NovelVolumeMapper novelVolumeMapper;
    @Autowired
    private NovelIllustrationMapper novelIllustrationMapper;
    @Autowired
    private CacheInvalidationService cacheInvalidationService;
    @Autowired
    private EpubImageStorage epubImageStorage;
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

    /** 兼容接口：仅创建主卷（一步式上架请用 /publishBook） */
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

    /** 一步式上架：主卷信息 + EPUB 分卷一次提交，后端先建主卷再逐卷解析入库 */
    @PostMapping("/publishBook")
    public Result publishBook(
            @RequestParam("bookName") String bookName,
            @RequestParam("author") String author,
            @RequestParam(value = "label", required = false) String label,
            @RequestParam(value = "cover", required = false) String cover,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        try {
            NovelBook novelBook = new NovelBook();
            novelBook.setBookMainName(bookName);
            novelBook.setAuthor(author);
            novelBook.setLabel(label);
            novelBook.setCover(cover);
            novelBook.setReadCount(0);
            novelBook.setStatus(1);
            novelBook.setCreateTime(new Date());
            novelBookMainMapper.insertNovel(novelBook);
            cacheInvalidationService.invalidateCategories();
            Integer mainBookId = novelBook.getId();

            UploadBatchResult batch = processVolumes(files, mainBookId);
            if (batch.rejected) return Result.error("上传队列已满，请稍后重试");
            if (batch.timeout) return Result.error("上传处理超时");
            if (batch.executionError) return Result.error("上传失败");
            if (batch.successCount > 0) {
                cacheInvalidationService.invalidateBookDetail(mainBookId);
            }
            return Result.success("成功上传 " + batch.successCount + " 个分卷", batch.toData(mainBookId));
        } catch (Exception e) {
            log.warn("publishBook failed bookName={} thread={}", bookName, Thread.currentThread().getName(), e);
            return Result.error("发布失败：" + e.getMessage());
        }
    }
    /** 给已有主卷补充分卷（后台书籍管理用），与一步式上架共用同一套 EPUB 解析与插画提取 */
    @PostMapping("/uploadVolumes")
    public Result uploadVolumes(@RequestParam("files") MultipartFile[] files, @RequestParam("mainBookId") Integer mainBookId) {
        if (files == null || files.length == 0 || mainBookId == null) {
            return Result.error("请选择要上传的 EPUB 文件");
        }
        UploadBatchResult batch = processVolumes(files, mainBookId);
        if (batch.rejected) return Result.error("上传队列已满，请稍后重试");
        if (batch.timeout) return Result.error("上传处理超时");
        if (batch.executionError) return Result.error("上传失败");
        if (batch.successCount > 0) {
            cacheInvalidationService.invalidateBookDetail(mainBookId);
        }
        return Result.success("成功上传 " + batch.successCount + " 个分卷", batch.toData(mainBookId));
    }

    /** 分发分卷到上传线程池并汇总结果 */
    private UploadBatchResult processVolumes(MultipartFile[] files, Integer mainBookId) {
        UploadBatchResult batch = new UploadBatchResult();
        if (files == null || files.length == 0) return batch;

        List<CompletableFuture<VolumeResult>> futures = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String volumeName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().replace(".epub", "")
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
                batch.rejected = true;
            } catch (Exception e) {
                log.warn("EPUB upload dispatch failed file={} mainBookId={} thread={}",
                        volumeName, mainBookId, Thread.currentThread().getName(), e);
                deleteTempFile(tempFile, volumeName);
            }
        }

        if (futures.isEmpty()) return batch;
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            batch.timeout = true;
        } catch (TimeoutException e) {
            log.warn("upload processing timed out mainBookId={} thread={}",
                    mainBookId, Thread.currentThread().getName(), e);
            batch.timeout = true;
        } catch (ExecutionException e) {
            log.warn("upload processing failed mainBookId={} thread={}",
                    mainBookId, Thread.currentThread().getName(), e);
            batch.executionError = true;
        }
        for (CompletableFuture<VolumeResult> future : futures) {
            VolumeResult vr;
            try {
                vr = future.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                vr = new VolumeResult();
                vr.volumeName = "未知分卷";
                vr.error = "处理结果获取失败";
            }
            batch.volumes.add(vr);
            if (vr.success) batch.successCount++;
            else batch.failedCount++;
        }
        return batch;
    }

    /** 解析单个 EPUB：提取正文（含插画标记）、插图落盘、分卷与插画入库 */
    private VolumeResult processVolume(File tempFile, Integer mainBookId, String volumeName) {
        VolumeResult result = new VolumeResult();
        result.volumeName = volumeName;
        String volumeUuid = UUID.randomUUID().toString().replace("-", "");
        try (InputStream inputStream = Files.newInputStream(tempFile.toPath())) {
            log.info("EPUB processing started file={} mainBookId={} thread={}",
                    volumeName, mainBookId, Thread.currentThread().getName());
            Map<String, byte[]> contents = extractEpubContents(inputStream);

            List<ExtractedImage> images = new ArrayList<>();
            StringBuilder fullContent = new StringBuilder();
            for (String contentFile : extractContentFiles(contents)) {
                byte[] data = contents.get(contentFile);
                if (data == null) continue;
                String parsed = parseHtmlWithImages(
                        new String(data, StandardCharsets.UTF_8), contentFile, contents, images);
                if (!parsed.trim().isEmpty()) fullContent.append(parsed.trim()).append("\n\n");
            }
            String content = fullContent.toString().trim();
            if (content.isEmpty()) {
                result.error = "未解析到正文内容";
                return result;
            }

            // 1. 插图落盘：uploads/epub-images/{主卷ID}/{分卷UUID}/，先写图、卷插入失败再整体清理
            List<NovelBookIllustration> illustrations =
                    saveIllustrations(images, mainBookId, volumeUuid);

            // 2. 插入分卷（带业务唯一键）
            NovelBookVolume volume = new NovelBookVolume();
            volume.setMainBookId(mainBookId);
            volume.setVolumeUuid(volumeUuid);
            volume.setVolumeName(volumeName);
            volume.setContent(content);
            volume.setCreateTime(new Date());
            int insertedRows = novelVolumeMapper.insertVolume(volume);
            if (insertedRows <= 0) {
                log.warn("EPUB volume insert returned no rows file={} mainBookId={} volumeUuid={} thread={}",
                        volumeName, mainBookId, volumeUuid, Thread.currentThread().getName());
                epubImageStorage.deleteVolumeDir(mainBookId, volumeUuid);
                result.error = "分卷入库失败";
                return result;
            }

            // 3. 插画入库（需要分卷 ID 关联）
            int savedIllustrations = 0;
            for (NovelBookIllustration illustration : illustrations) {
                illustration.setVolumeId(volume.getId());
                try {
                    novelIllustrationMapper.insertIllustration(illustration);
                    savedIllustrations++;
                } catch (Exception e) {
                    log.warn("illustration insert failed volumeId={} sortOrder={}",
                            volume.getId(), illustration.getSortOrder(), e);
                }
            }

            result.volumeId = volume.getId();
            result.illustrationCount = savedIllustrations;
            result.success = true;
            log.info("EPUB processing completed file={} mainBookId={} volumeId={} illustrations={} thread={}",
                    volumeName, mainBookId, volume.getId(), savedIllustrations, Thread.currentThread().getName());
            return result;
        } catch (Exception e) {
            log.warn("EPUB file processing failed file={} thread={}",
                    volumeName, Thread.currentThread().getName(), e);
            result.error = e.getMessage();
            return result;
        } finally {
            deleteTempFile(tempFile, volumeName);
        }
    }
    /** 解析单个 XHTML：剥标签取正文，遇到 <img> 则提取图片并把占位标记 [插图n] 保留在正文原位 */
    private String parseHtmlWithImages(String html, String contentFile,
                                       Map<String, byte[]> contents, List<ExtractedImage> images) {
        String parentDir = contentFile.contains("/")
                ? contentFile.substring(0, contentFile.lastIndexOf('/'))
                : "";
        StringBuilder sb = new StringBuilder();
        Matcher m = IMG_TAG.matcher(html);
        int last = 0;
        while (m.find()) {
            sb.append(cleanHtmlContent(html.substring(last, m.start())));
            String tag = m.group();
            Matcher sm = SRC_ATTR.matcher(tag);
            String src = sm.find() ? sm.group(1).trim() : null;
            String entryPath = null;
            try {
                entryPath = src != null ? resolveEntryPath(parentDir, src) : null;
            } catch (Exception e) {
                log.warn("EPUB image src resolve failed file={} src={}", contentFile, src, e);
            }
            byte[] bytes = entryPath != null ? contents.get(entryPath) : null;
            if (bytes == null && entryPath != null) {
                // 兜底：部分 EPUB 的 src 不是相对 XHTML 目录的路径，按文件名后缀匹配 zip 条目
                String baseName = entryPath.contains("/")
                        ? entryPath.substring(entryPath.lastIndexOf('/') + 1)
                        : entryPath;
                if (!baseName.isEmpty()) {
                    for (Map.Entry<String, byte[]> entry : contents.entrySet()) {
                        String key = entry.getKey();
                        if (key.equals(baseName) || key.endsWith("/" + baseName)) {
                            bytes = entry.getValue();
                            entryPath = key;
                            break;
                        }
                    }
                }
            }
            if (bytes != null && looksLikeImage(entryPath)) {
                int order = images.size() + 1;
                String originalName = entryPath.contains("/")
                        ? entryPath.substring(entryPath.lastIndexOf('/') + 1)
                        : entryPath;
                String savedName = String.format("%04d_%s", order, originalName);
                images.add(new ExtractedImage(order, originalName, savedName, bytes));
                sb.append("\n").append(ILLUS_MARKER).append(order).append("]\n");
            } else {
                log.warn("EPUB image skipped file={} src={} entry={}", contentFile, src, entryPath);
            }
            last = m.end();
        }
        sb.append(cleanHtmlContent(html.substring(last)));
        return sb.toString();
    }

    /** 把 EPUB 内相对路径的图片 src 解析为 zip 内条目路径 */
    private String resolveEntryPath(String parentDir, String src) {
        String decoded = URLDecoder.decode(src, StandardCharsets.UTF_8);
        int q = decoded.indexOf('?');
        if (q >= 0) decoded = decoded.substring(0, q);
        int h = decoded.indexOf('#');
        if (h >= 0) decoded = decoded.substring(0, h);
        decoded = decoded.replace('\\', '/');
        if (decoded.startsWith("/")) return decoded.substring(1);
        Path resolved = Paths.get(parentDir.isEmpty() ? decoded : parentDir + "/" + decoded).normalize();
        return resolved.toString().replace('\\', '/');
    }

    private boolean looksLikeImage(String entryPath) {
        String lower = entryPath.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".webp");
    }

    /** 插图落盘并组装入库对象（单张失败跳过，不拖垮整卷） */
    private List<NovelBookIllustration> saveIllustrations(List<ExtractedImage> images,
                                                          Integer mainBookId, String volumeUuid) {
        List<NovelBookIllustration> illustrations = new ArrayList<>();
        if (images.isEmpty()) return illustrations;
        Path dir = epubImageStorage.volumeDir(mainBookId, volumeUuid);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.warn("create epub-images dir failed dir={}", dir, e);
            return illustrations;
        }
        for (ExtractedImage image : images) {
            Path target = dir.resolve(image.savedName);
            try {
                Files.write(target, image.bytes);
                NovelBookIllustration illustration = new NovelBookIllustration();
                illustration.setSortOrder(image.sortOrder);
                illustration.setUrl("/epub-images/" + mainBookId + "/" + volumeUuid + "/" + image.savedName);
                illustration.setOriginalName(image.originalName);
                illustrations.add(illustration);
            } catch (IOException e) {
                log.warn("illustration save failed name={}", image.savedName, e);
            }
        }
        return illustrations;
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

    private static class VolumeResult {
        String volumeName;
        Integer volumeId;
        int illustrationCount;
        boolean success;
        String error;
    }

    private static class ExtractedImage {
        final int sortOrder;
        final String originalName;
        final String savedName;
        final byte[] bytes;

        ExtractedImage(int sortOrder, String originalName, String savedName, byte[] bytes) {
            this.sortOrder = sortOrder;
            this.originalName = originalName;
            this.savedName = savedName;
            this.bytes = bytes;
        }
    }

    private static class UploadBatchResult {
        final List<VolumeResult> volumes = new ArrayList<>();
        int successCount;
        int failedCount;
        boolean rejected;
        boolean timeout;
        boolean executionError;

        Map<String, Object> toData(Integer mainBookId) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("mainBookId", mainBookId);
            data.put("successCount", successCount);
            data.put("failedCount", failedCount);
            List<Map<String, Object>> volumeRows = new ArrayList<>();
            for (VolumeResult vr : volumes) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("volumeName", vr.volumeName);
                row.put("volumeId", vr.volumeId);
                row.put("illustrationCount", vr.illustrationCount);
                row.put("success", vr.success);
                row.put("error", vr.error);
                volumeRows.add(row);
            }
            data.put("volumes", volumeRows);
            return data;
        }
    }
}