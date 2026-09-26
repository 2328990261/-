// controller/NovelBookController.java
package com.wangrui.springboot.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wangrui.springboot.mapper.BannerCarouselMapper;
import com.wangrui.springboot.mapper.NovelVolumeMapper;
import com.wangrui.springboot.pojo.NovelBook;
import com.wangrui.springboot.pojo.NovelBookVolume;
import com.wangrui.springboot.pojo.Tag;
import com.wangrui.springboot.service.NovelBookMainService;
import com.wangrui.springboot.service.RedisCacheService;
import com.wangrui.springboot.service.NovelBookService;
import com.wangrui.springboot.service.TagService;
import com.wangrui.springboot.util.Result;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.Map;


/**
 * 小说模块控制器
 * 处理小说相关的所有接口请求，路径前缀：/api/novel
 */
@RestController
@RequestMapping("/novel")
public class NovelBookController {
    @Autowired
    private NovelBookMainService novelBookMainService;

    @Autowired
    private NovelVolumeMapper novelVolumeMapper;

    @Autowired
    private NovelBookService novelBookService;

    @Autowired
    private TagService tagService;

    @Autowired
    private BannerCarouselMapper bannerCarouselMapper;
    @Autowired
    private RedisCacheService redisCacheService;

    @Value("${upload.covers-dir:}")
    private String uploadCoversDir;

    @Value("${upload.banners-dir:}")
    private String uploadBannersDir;

    // 接口1：获取所有小说（含封面URL）
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getAllNovels() {
        List<Map<String, Object>> novelList = novelBookMainService.getAllNovels();
        return Result.success(novelList);
    }

    /** 公开接口：获取标签列表（按 sort_order 排序），供前台标签栏渲染 */
    @GetMapping("/tags")
    public Result<List<Tag>> getTags() {
        List<Tag> list = tagService.listAll();
        return Result.success(list);
    }

    /** 按多个标签查询小说（AND 逻辑） */
    @PostMapping("/listByLabels")
    public Result<List<Map<String, Object>>> getNovelsByLabels(@RequestBody List<String> labels) {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByLabels(labels);
        return Result.success(novelList);
    }

    /** 搜索：支持 ID 精确查询，或 书名/作者 模糊查询 */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> searchNovels(@RequestParam(value = "keyword", required = false) String keyword) {
        List<Map<String, Object>> list = novelBookMainService.searchNovels(keyword);
        return Result.success(list);
    }

    // 接口2：按标签查询小说（统一参数化接口，传中文标签名）
    @GetMapping("/listByLabel")
    public Result<List<Map<String, Object>>> getNovelsByLabel(@RequestParam("label") String label) {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByLabel(label);
        return Result.success(novelList);
    }

    // 接口3：获取首页热门轮播图
    @GetMapping("/carousel")
    public Result<List<Map<String, Object>>> getCarouselBooks() {
        try {
            String cacheKey = "novel:public:carousel";
            List<Map<String, Object>> cached = redisCacheService.get(
                    cacheKey, new TypeReference<List<Map<String, Object>>>() {});
            if (cached != null) {
                return Result.success(cached);
            }
            List<Map<String, Object>> carouselList = bannerCarouselMapper.selectActiveBanners();
            redisCacheService.set(cacheKey, carouselList,
                    redisCacheService.properties().getPublicTtl());
            return Result.success(carouselList);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("获取轮播图失败：" + e.getMessage());
        }
    }

    // 接口4：查小说分卷内容
    @GetMapping("/volume")
    public Result<List<NovelBookVolume>> getNovelVolume(@RequestParam Integer mainBookId) {
        List<NovelBookVolume> volumes = novelVolumeMapper.selectVolumeByMainBookId(mainBookId);
        return Result.success(volumes);
    }

    // 接口5：获取小说详情（含分卷列表）
    @GetMapping("/detail/{id}")
    public Result<NovelBook> getNovelDetail(@PathVariable Integer id) {
        try {
            NovelBook novelBook = novelBookService.getNovelDetailByMainId(id);
            if (novelBook == null) {
                return Result.fail("小说不存在");
            }
            return Result.success(novelBook);
        } catch (Exception e) {
            return Result.fail("获取小说详情失败：" + e.getMessage());
        }
    }

    // 接口6：获取章节内容
    @GetMapping("/chapter/{id}")
    public Result<NovelBookVolume> getChapterContent(@PathVariable Integer id) {
        try {
            NovelBookVolume volume = novelBookService.getVolumeContentById(id);
            if (volume == null) {
                return Result.fail("章节不存在");
            }
            return Result.success(volume);
        } catch (Exception e) {
            return Result.fail("获取章节内容失败：" + e.getMessage());
        }
    }

    @GetMapping("/cover/{coverName}")
    public ResponseEntity<Resource> getNovelCover(@PathVariable String coverName) {
        try {
            String decodeName = URLDecoder.decode(coverName, StandardCharsets.UTF_8);
            Resource resource = null;
            if (uploadCoversDir != null && !uploadCoversDir.isEmpty()) {
                Path filePath = Paths.get(uploadCoversDir).resolve(decodeName);
                if (Files.exists(filePath)) {
                    resource = new FileSystemResource(filePath.toFile());
                }
            }
            if ((resource == null || !resource.exists()) && uploadBannersDir != null && !uploadBannersDir.isEmpty()) {
                Path filePath = Paths.get(uploadBannersDir).resolve(decodeName);
                if (Files.exists(filePath)) {
                    resource = new FileSystemResource(filePath.toFile());
                }
            }
            if (resource == null || !resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentLength(resource.contentLength());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // 接口7：增加阅读量
    @PostMapping("/increaseReadCount/{id}")
    public Result<Void> increaseReadCount(@PathVariable Integer id) {
        try {
            novelBookMainService.increaseReadCount(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.fail("增加阅读量失败：" + e.getMessage());
        }
    }

}