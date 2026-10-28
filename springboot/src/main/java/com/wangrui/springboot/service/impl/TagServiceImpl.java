package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.TagMapper;
import com.wangrui.springboot.pojo.Tag;
import com.wangrui.springboot.service.SiteTagHeatService;
import com.wangrui.springboot.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** 标签服务实现：标签增删改查与推荐权重读取 */
@Service
public class TagServiceImpl implements TagService {
    // 无热度时的默认推荐权重
    private static final double DEFAULT_WEIGHT = 0.1;

    // 标签数据访问对象
    @Autowired
    private TagMapper tagMapper;

    // 全站标签热度服务（用于读取全局推荐权重）
    @Autowired
    private SiteTagHeatService siteTagHeatService;

    /** 查询全部标签（按排序字段） */
    @Override
    public List<Tag> listAll() {
        return tagMapper.selectAllOrderBySort();
    }

    /** 按ID查询标签 */
    @Override
    public Tag getById(Integer id) {
        return tagMapper.selectById(id);
    }

    /** 新增标签 */
    @Override
    public int add(Tag tag) {
        return tagMapper.insert(tag);
    }

    /** 更新标签 */
    @Override
    public int update(Tag tag) {
        return tagMapper.update(tag);
    }

    /** 删除标签 */
    @Override
    public int delete(Integer id) {
        return tagMapper.deleteById(id);
    }

    /** 获取全局标签推荐权重 */
    @Override
    public Map<String, Double> getRecommendWeights() {
        return siteTagHeatService.getGlobalRecommendWeights();
    }
}