package com.wangrui.springboot.service.impl;

import com.wangrui.springboot.mapper.TagMapper;
import com.wangrui.springboot.pojo.Tag;
import com.wangrui.springboot.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl implements TagService {
    private static final double DEFAULT_WEIGHT = 0.1;

    @Autowired
    private TagMapper tagMapper;

    @Override
    public List<Tag> listAll() {
        return tagMapper.selectAllOrderBySort();
    }

    @Override
    public Tag getById(Integer id) {
        return tagMapper.selectById(id);
    }

    @Override
    public int add(Tag tag) {
        return tagMapper.insert(tag);
    }

    @Override
    public int update(Tag tag) {
        return tagMapper.update(tag);
    }

    @Override
    public int delete(Integer id) {
        return tagMapper.deleteById(id);
    }

    @Override
    public Map<String, Double> getRecommendWeights() {
        List<Tag> tags = tagMapper.selectAll();
        Map<String, Double> map = new HashMap<>();
        for (Tag t : tags) {
            if (t.getName() != null && t.getRecommendWeight() != null) {
                map.put(t.getName().trim(), t.getRecommendWeight().doubleValue());
            }
        }
        return map;
    }
}
