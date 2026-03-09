// controller/NovelBookController.java
package com.wangrui.springboot.controller;

import com.wangrui.springboot.mapper.BannerCarouselMapper;
import com.wangrui.springboot.mapper.NovelVolumeMapper;
import com.wangrui.springboot.pojo.NovelBook;
import com.wangrui.springboot.pojo.NovelBookVolume;
import com.wangrui.springboot.pojo.Tag;
import com.wangrui.springboot.service.NovelBookMainService;
import com.wangrui.springboot.service.NovelBookService;
import com.wangrui.springboot.service.TagService;
import com.wangrui.springboot.util.Result;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
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
    // 注入小说主业务层Service（处理小说列表、标签查询、轮播图等）
    @Autowired
    private NovelBookMainService novelBookMainService;

    // 注入小说分卷Mapper（直接操作分卷数据库）
    @Autowired
    private NovelVolumeMapper novelVolumeMapper;

    // 注入小说详情业务层Service（处理小说详情、章节内容查询）
    @Autowired
    private NovelBookService novelBookService;

    @Autowired
    private TagService tagService;

    @Autowired
    private BannerCarouselMapper bannerCarouselMapper;

    @Value("${upload.covers-dir:}")
    private String uploadCoversDir;

    // 接口1：获取所有小说（含封面URL）
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getAllNovels() {
        // 直接调用Service加工后的方法（已拼接封面URL）
        List<Map<String, Object>> novelList = novelBookMainService.getAllNovels();
        return Result.success(novelList);
    }

    /** 公开接口：获取标签列表（按 sort_order 排序），供前台标签栏渲染，后台修改后前台同步 */
    @GetMapping("/tags")
    public Result<List<Tag>> getTags() {
        List<Tag> list = tagService.listAll();
        return Result.success(list);
    }

    // 新增：按多个标签查询小说
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

    // 接口2：按标签查小说（为每个标签单独写接口）
    // 第1行标签
    @GetMapping("/listByDaily")
    public Result<List<Map<String, Object>>> getNovelsByDaily() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByDaily();
        return Result.success(novelList);
    }

    @GetMapping("/listByFantasy")
    public Result<List<Map<String, Object>>> getNovelsByFantasy() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByFantasy();
        return Result.success(novelList);
    }

    @GetMapping("/listBySchool")
    public Result<List<Map<String, Object>>> getNovelsBySchool() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsBySchool();
        return Result.success(novelList);
    }

    @GetMapping("/listByAdventure")
    public Result<List<Map<String, Object>>> getNovelsByAdventure() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByAdventure();
        return Result.success(novelList);
    }

    @GetMapping("/listByIsekai")
    public Result<List<Map<String, Object>>> getNovelsByIsekai() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByIsekai();
        return Result.success(novelList);
    }

    // 第2行标签
    @GetMapping("/listByRelax")
    public Result<List<Map<String, Object>>> getNovelsByRelax() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByRelax();
        return Result.success(novelList);
    }

    @GetMapping("/listByFunny")
    public Result<List<Map<String, Object>>> getNovelsByFunny() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByFunny();
        return Result.success(novelList);
    }

    @GetMapping("/listByHealing")
    public Result<List<Map<String, Object>>> getNovelsByHealing() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByHealing();
        return Result.success(novelList);
    }

    @GetMapping("/listByDepressing")
    public Result<List<Map<String, Object>>> getNovelsByDepressing() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByDepressing();
        return Result.success(novelList);
    }

    @GetMapping("/listBySweet")
    public Result<List<Map<String, Object>>> getNovelsBySweet() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsBySweet();
        return Result.success(novelList);
    }

    @GetMapping("/listByHotblood")
    public Result<List<Map<String, Object>>> getNovelsByHotblood() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByHotblood();
        return Result.success(novelList);
    }

    @GetMapping("/listByLove")
    public Result<List<Map<String, Object>>> getNovelsByLove() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByLove();
        return Result.success(novelList);
    }

    @GetMapping("/listByGrowth")
    public Result<List<Map<String, Object>>> getNovelsByGrowth() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByGrowth();
        return Result.success(novelList);
    }

    // 第3行标签
    @GetMapping("/listByIntellect")
    public Result<List<Map<String, Object>>> getNovelsByIntellect() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByIntellect();
        return Result.success(novelList);
    }

    @GetMapping("/listBySuspense")
    public Result<List<Map<String, Object>>> getNovelsBySuspense() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsBySuspense();
        return Result.success(novelList);
    }

    @GetMapping("/listByDeduction")
    public Result<List<Map<String, Object>>> getNovelsByDeduction() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByDeduction();
        return Result.success(novelList);
    }

    @GetMapping("/listByPsychological")
    public Result<List<Map<String, Object>>> getNovelsByPsychological() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByPsychological();
        return Result.success(novelList);
    }

    @GetMapping("/listByBattle")
    public Result<List<Map<String, Object>>> getNovelsByBattle() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByBattle();
        return Result.success(novelList);
    }

    @GetMapping("/listByCompetition")
    public Result<List<Map<String, Object>>> getNovelsByCompetition() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByCompetition();
        return Result.success(novelList);
    }

    @GetMapping("/listByConstruction")
    public Result<List<Map<String, Object>>> getNovelsByConstruction() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByConstruction();
        return Result.success(novelList);
    }

    // 第4行标签
    @GetMapping("/listByPalace")
    public Result<List<Map<String, Object>>> getNovelsByPalace() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByPalace();
        return Result.success(novelList);
    }

    @GetMapping("/listByVirtualGame")
    public Result<List<Map<String, Object>>> getNovelsByVirtualGame() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByVirtualGame();
        return Result.success(novelList);
    }

    @GetMapping("/listByRealistic")
    public Result<List<Map<String, Object>>> getNovelsByRealistic() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByRealistic();
        return Result.success(novelList);
    }

    @GetMapping("/listByFarming")
    public Result<List<Map<String, Object>>> getNovelsByFarming() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByFarming();
        return Result.success(novelList);
    }

    @GetMapping("/listByReincarnation")
    public Result<List<Map<String, Object>>> getNovelsByReincarnation() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByReincarnation();
        return Result.success(novelList);
    }

    @GetMapping("/listByTransmigration")
    public Result<List<Map<String, Object>>> getNovelsByTransmigration() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByTransmigration();
        return Result.success(novelList);
    }

    @GetMapping("/listByMagic")
    public Result<List<Map<String, Object>>> getNovelsByMagic() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByMagic();
        return Result.success(novelList);
    }

    // 第5行标签
    @GetMapping("/listByAllAges")
    public Result<List<Map<String, Object>>> getNovelsByAllAges() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByAllAges();
        return Result.success(novelList);
    }

    @GetMapping("/listByLightYuri")
    public Result<List<Map<String, Object>>> getNovelsByLightYuri() {
        List<Map<String, Object>> novelList = novelBookMainService.getNovelsByLightYuri();
        return Result.success(novelList);
    }

    // 接口3：查小说分卷内容
    @GetMapping("/volume")
    public Result<List<NovelBookVolume>> getNovelVolume(@RequestParam Integer mainBookId) {
        // 根据小说主ID查询对应的分卷列表
        List<NovelBookVolume> volumes = novelVolumeMapper.selectVolumeByMainBookId(mainBookId);
        return Result.success(volumes);
    }

    // 接口4：轮播图（从 banner_carousel 表读取）
    @GetMapping("/carousel")
    public Result<List<Map<String, Object>>> getCarouselBooks() {
        try {
            // 从轮播图表获取当前有效的轮播图列表
            List<Map<String, Object>> carouselList = bannerCarouselMapper.selectActiveBanners();
            return Result.success(carouselList);
        } catch (Exception e) {
            // 异常捕获：返回失败提示
            e.printStackTrace();
            return Result.fail("获取轮播图失败：" + e.getMessage());
        }
    }

    // 接口5：获取小说详情（含分卷列表）
    // 路径参数id：小说主ID
    @GetMapping("/detail/{id}")
    public Result<NovelBook> getNovelDetail(@PathVariable Integer id) {
        try {
            // 调用Service获取小说详情（含主信息+分卷列表）
            NovelBook novelBook = novelBookService.getNovelDetailByMainId(id);
            if (novelBook == null) {
                // 小说不存在时返回失败提示
                return Result.fail("小说不存在");
            }
            // 成功返回小说详情数据
            return Result.success(novelBook);
        } catch (Exception e) {
            // 异常捕获：返回失败提示（包含异常信息）
            return Result.fail("获取小说详情失败：" + e.getMessage());
        }
    }

    // 接口6：获取章节内容
    // 路径参数id：分卷/章节ID
    @GetMapping("/chapter/{id}")
    public Result<NovelBookVolume> getChapterContent(@PathVariable Integer id) {
        try {
            // 调用Service获取章节详情
            NovelBookVolume volume = novelBookService.getVolumeContentById(id);
            if (volume == null) {
                // 章节不存在时返回失败提示
                return Result.fail("章节不存在");
            }
            // 成功返回章节内容数据
            return Result.success(volume);
        } catch (Exception e) {
            // 异常捕获：返回失败提示（包含异常信息）
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
            if (resource == null || !resource.exists()) {
                resource = new ClassPathResource("static/images/covers/" + decodeName);
            }
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // 设置响应头
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
