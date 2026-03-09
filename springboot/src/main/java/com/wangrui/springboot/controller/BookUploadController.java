package com.wangrui.springboot.controller;

import com.wangrui.springboot.pojo.NovelBook;
import com.wangrui.springboot.pojo.NovelBookVolume;
import com.wangrui.springboot.mapper.NovelBookMainMapper;
import com.wangrui.springboot.mapper.NovelVolumeMapper;
import com.wangrui.springboot.util.Result;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class BookUploadController {
    @Value("${upload.covers-dir:}")
    private String coversDir;

    @Autowired
    private NovelBookMainMapper novelBookMainMapper;
    @Autowired
    private NovelVolumeMapper novelVolumeMapper;

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
            Path dir = Paths.get(coversDir != null && !coversDir.isEmpty() ? coversDir : System.getProperty("user.dir") + "/uploads/covers");
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
            return Result.success(novelBook.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建失败");
        }
    }

    @PostMapping("/uploadVolumes")
    public Result uploadVolumes(@RequestParam("files") MultipartFile[] files, @RequestParam("mainBookId") Integer mainBookId) {
        try {
            int total = 0;
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                Map<String, byte[]> contents = extractEpubContents(file.getInputStream());
                String volumeName = file.getOriginalFilename().replace(".epub", "");
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
                    novelVolumeMapper.insertVolume(volume);
                    total++;
                }
            }
            return Result.success("成功上传 " + total + " 个分卷");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上传失败");
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
