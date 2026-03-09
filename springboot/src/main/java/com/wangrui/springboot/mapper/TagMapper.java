package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
