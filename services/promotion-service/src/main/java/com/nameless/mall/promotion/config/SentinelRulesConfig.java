package com.nameless.mall.promotion.config;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel Flow Rules 預設配置。
 * <p>
 * 在應用啟動時載入保守的流量控制規則，防止在 Sentinel Dashboard 尚未推送規則時
 * 出現防護空窗期。
 * </p>
 * <p>
 * <b>設計決策</b>：
 * <ul>
 * <li>預設規則採最小保護原則（QPS 閾值保守設定），避免影響正常呼叫</li>
 * <li>Sentinel Dashboard 或 Nacos Datasource 推送的規則優先覆蓋此處的規則
 * （{@link FlowRuleManager#loadRules} 為全量替換，不合併）</li>
 * <li>Production 環境建議整合 {@code sentinel-datasource-nacos} 動態推送規則，
 * 以實現不重啟更新閾值的能力</li>
 * </ul>
 * </p>
 * <p>
 * 受保護的資源及其閾值：
 * <ul>
 * <li>{@code deductStock} — 50 QPS（秒殺高峰下游呼叫，需嚴格限制）</li>
 * <li>{@code recoverStock} — 100 QPS（補償呼叫，峰值相對分散）</li>
 * <li>{@code syncStock} — 5 QPS（管理員操作，極低頻）</li>
 * <li>{@code getCurrentSession} — 500 QPS（讀取操作，允許較高流量）</li>
 * </ul>
 * </p>
 */
@Slf4j
@Configuration
public class SentinelRulesConfig {

    /**
     * 於 Spring 容器初始化完成後載入 Sentinel Flow Rules。
     * <p>
     * 使用 {@code @PostConstruct} 而非 {@code ApplicationRunner}，
     * 確保 Rules 在任何 HTTP 請求到達前完成載入。
     * </p>
     */
    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // --- 寫入端點（高風險）---
        rules.add(buildQpsRule("deductStock", 50));
        rules.add(buildQpsRule("recoverStock", 100));
        rules.add(buildQpsRule("syncStock", 5));

        // --- 讀取端點（中等風險）---
        rules.add(buildQpsRule("getCurrentSession", 500));

        FlowRuleManager.loadRules(rules);
        log.info("[Sentinel] Flow Rules 初始載入完成，共 {} 條規則（Dashboard 推送可動態覆蓋）", rules.size());
    }

    /**
     * 建立 QPS 類型的流量控制規則。
     *
     * @param resource 資源名（與 {@code @SentinelResource#value} 一致）
     * @param qps      每秒最大請求數
     * @return 已配置的 FlowRule
     */
    private FlowRule buildQpsRule(String resource, double qps) {
        FlowRule rule = new FlowRule(resource);
        rule.setCount(qps);
        rule.setGrade(com.alibaba.csp.sentinel.slots.block.RuleConstant.FLOW_GRADE_QPS);
        return rule;
    }
}
