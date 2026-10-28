package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 标签数据访问接口，负责标签的增删改查与排序 */
@Mapper
public interface TagMapper {
    List<Tag> selectAll();
    List<Tag> selectAllOrderBySort();
    Tag selectById(@Param("id") Integer id);
    Tag selectByName(@Param("name") String name);
    int insert(Tag tag);
    int update(Tag tag);
    int deleteById(@Param("id") Integer id);
}