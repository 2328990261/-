package com.wangrui.springboot.controller;

import com.wangrui.springboot.mapper.BannerCarouselMapper;
import com.wangrui.springboot.pojo.BannerCarousel;
import com.wangrui.springboot.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 后台轮播图管理控制器
 */
@RestController
@RequestMapping("/api/admin/banner")
@CrossOrigin
public class AdminBannerController {

    @Autowired
    private BannerCarouselMapper bannerCarouselMapper;

    @Value("${upload.covers-dir:}")
    private String uploadCoversDir;

    /**
     * 获取所有轮播图列表（后台管理用）
     */
    @GetMapping("/list")
    public Result<List<BannerCarousel>> getAllBanners() {
        try {
            List<BannerCarousel> list = bannerCarouselMapper.selectAll();
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取轮播图列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取轮播图详情
     */
    @GetMapping("/{id}")
    public Result<BannerCarousel> getBannerById(@PathVariable Integer id) {
        try {
            BannerCarousel banner = bannerCarouselMapper.selectById(id);
            if (banner == null) {
                return Result.error("轮播图不存在");
            }
            return Result.success(banner);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取轮播图详情失败：" + e.getMessage());
        }
    }

    /**
     * 上传轮播图图片
     */
    @PostMapping("/upload-image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        try {
            // 获取原始文件名和扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 生成唯一文件名
            String filename = "banner_" + UUID.randomUUID().toString() + extension;

            // 确定保存路径
            String savePath;
            if (uploadCoversDir != null && !uploadCoversDir.isEmpty()) {
                savePath = uploadCoversDir;
            } else {
                // 默认保存到项目的 resources/static/images/covers 目录
                savePath = "src/main/resources/static/images/covers";
            }

            // 创建目录（如果不存在）
            File directory = new File(savePath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 保存文件
            Path filePath = Paths.get(savePath, filename);
            Files.write(filePath, file.getBytes());

            // 返回文件名（不包含路径）
            return Result.success(filename);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 新增轮播图
     */
    @PostMapping("/add")
    public Result<Void> addBanner(@RequestBody BannerCarousel banner) {
        try {
            // 设置默认值
            if (banner.getStatus() == null) {
                banner.setStatus(1);
            }
            if (banner.getSortOrder() == null) {
                banner.setSortOrder(0);
            }
            
            int rows = bannerCarouselMapper.insert(banner);
            if (rows > 0) {
                return Result.success(null);
            }
            return Result.error("新增轮播图失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("新增轮播图失败：" + e.getMessage());
        }
    }

    /**
     * 更新轮播图
     */
    @PutMapping("/update")
    public Result<Void> updateBanner(@RequestBody BannerCarousel banner) {
        try {
            if (banner.getId() == null) {
                return Result.error("轮播图ID不能为空");
            }
            
            int rows = bannerCarouselMapper.update(banner);
            if (rows > 0) {
                return Result.success(null);
            }
            return Result.error("更新轮播图失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新轮播图失败：" + e.getMessage());
        }
    }

    /**
     * 删除轮播图
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteBanner(@PathVariable Integer id) {
        try {
            int rows = bannerCarouselMapper.deleteById(id);
            if (rows > 0) {
                return Result.success(null);
            }
            return Result.error("删除轮播图失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除轮播图失败：" + e.getMessage());
        }
    }

    /**
     * 切换轮播图状态（启用/禁用）
     */
    @PutMapping("/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Integer id) {
        try {
            BannerCarousel banner = bannerCarouselMapper.selectById(id);
            if (banner == null) {
                return Result.error("轮播图不存在");
            }
            
            // 切换状态：0变1，1变0
            banner.setStatus(banner.getStatus() == 1 ? 0 : 1);
            int rows = bannerCarouselMapper.update(banner);
            
            if (rows > 0) {
                return Result.success(null);
            }
            return Result.error("切换状态失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("切换状态失败：" + e.getMessage());
        }
    }
}
