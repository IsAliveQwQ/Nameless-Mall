package com.nameless.mall.search;

import com.nameless.mall.search.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication(exclude = {
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class
}, scanBasePackages = {
                "com.nameless.mall.search",
                "com.nameless.mall.core"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.nameless.mall")
public class SearchServiceApplication {
        public static void main(String[] args) {
                SpringApplication.run(SearchServiceApplication.class, args);
        }

        /**
         * 啟動時自動觸發全量索引同步。
         */
        @Bean
        public CommandLineRunner syncOnStartup(SearchService searchService) {
                return args -> {
                        log.info("========== [自動同步] 開始執行全量數據同步 ==========");
                        try {
                                int count = searchService.syncAll();
                                log.info("========== [自動同步] 同步完成，共計: {} 筆項目 ==========", count);
                        } catch (Exception e) {
                                log.error("========== [自動同步] 同步過程中發生嚴重錯誤: {} ==========", e.getMessage(), e);
                        }
                };
        }
}
