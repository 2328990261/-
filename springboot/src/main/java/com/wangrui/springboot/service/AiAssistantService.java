package com.wangrui.springboot.service;

import com.wangrui.springboot.pojo.AiChatMessage;
import com.wangrui.springboot.pojo.vo.AiChatResult;
import com.wangrui.springboot.pojo.vo.AdminAiTestRequest;

import java.util.List;

/** AI助手服务接口：会话、消息流式推荐与配置对话测试 */
public interface AiAssistantService {

    /**
     * 根据用户画像与候选书池，返回一句推荐说明 + 推荐书单。
     * history 为当前会话最近的消息（不含本条用户消息）。
     * callback 用于把模型推理过程实时回调给调用方（流式思考框）。
     */
    AiChatResult answer(Integer userId, List<AiChatMessage> history, String content, String imageUrl,
                        AiStreamCallback callback);

    /**
     * 后台「对话测试」：使用管理员提交的草稿人设 / 推荐规则 / 模型连接配置，
     * 进行一次不落库的对话（mode 可选 persona=仅人设 / recommend=完整推荐）。
     */
    AiChatResult test(AdminAiTestRequest request, AiStreamCallback callback);
}