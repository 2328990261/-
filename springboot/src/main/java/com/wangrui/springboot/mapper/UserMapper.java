package com.wangrui.springboot.mapper;

import com.wangrui.springboot.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);

    User findByPhone(@Param("phone") String phone);

    void insert(User user);

    // 添加收藏
    void addCollection(Map<String, Object> params);

    // 取消收藏
    void removeCollection(Map<String, Object> params);

    // 检查收藏状态
    int checkCollection(Map<String, Object> params);

    // 获取用户收藏列表
    List<Map<String, Object>> getCollectionList(Integer userId);

    void updateUserInfo(Map<String, Object> params);

    List<Map<String, Object>> selectUserPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                             @Param("keyword") String keyword, @Param("status") Integer status);
    int selectUserCount(@Param("keyword") String keyword, @Param("status") Integer status);
    void updateStatus(@Param("id") Integer id, @Param("status") Integer status);
    int selectCollectionCount();

    /** 全站收藏的小说 id（含不同用户重复收藏同一书，重复计数以放大热度） */
    List<Integer> selectAllCollectionNovelIds();
}
