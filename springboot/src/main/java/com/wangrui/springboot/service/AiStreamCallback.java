package com.wangrui.springboot.service;

/**
 * AI 荐书流式回调，用于把模型推理过程（reasoning_content）逐段推送给调用方。
 */
@FunctionalInterface
public interface AiStreamCallback {

    void onThinking(String delta);
}
