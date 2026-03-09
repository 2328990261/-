package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.BannerCarousel;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface BannerCarouselMapper {
    
    /**
     * 查询当前有效的轮播图列表（按排序）
     * 返回轮播图信息 + 关联的小说信息
     */
    List<Map<String, Object>> selectActiveBanners();
    
    /**
     * 查询所有轮播图（后台管理用）
     */
    List<BannerCarousel> selectAll();
    
    /**
     * 根据ID查询
     */
    BannerCarousel selectById(Integer id);
    
    /**
     * 新增轮播图
     */
    int insert(BannerCarousel banner);
    
    /**
     * 更新轮播图
     */
    int update(BannerCarousel banner);
    
    /**
     * 删除轮播图
     */
    int deleteById(Integer id);
    
    /**
     * 更新点击次数
     */
    int updateClickCount(Integer id);
}
