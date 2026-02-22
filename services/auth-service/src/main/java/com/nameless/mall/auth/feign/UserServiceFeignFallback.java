package com.nameless.mall.auth.feign;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.core.enums.ResultCodeEnum;
import com.nameless.mall.user.api.dto.SocialUserDTO;
import com.nameless.mall.user.api.dto.UserAuthDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * UserServiceFeignClient 降級工廠。
 * 透過 FallbackFactory 取得觸發降級的原始異常，便於定位根因。
 */
@Component
public class UserServiceFeignFallback implements FallbackFactory<UserServiceFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(UserServiceFeignFallback.class);

    @Override
    public UserServiceFeignClient create(Throwable cause) {
        return new UserServiceFeignClient() {
            @Override
            public Result<UserAuthDTO> loadUserByUsername(String username) {
                log.error("降級 | UserServiceFeignClient.loadUserByUsername 失敗, username: {}, cause: {}",
                        username, cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "用戶服務暫時不可用");
            }

            @Override
            public Result<UserAuthDTO> findOrCreateBySocial(SocialUserDTO socialUserDTO) {
                log.error("降級 | UserServiceFeignClient.findOrCreateBySocial 失敗, provider: {}, cause: {}",
                        socialUserDTO != null ? socialUserDTO.getProvider() : "unknown",
                        cause.getMessage(), cause);
                return Result.fail(ResultCodeEnum.SERVICE_UNAVAILABLE, "用戶服務暫時不可用");
            }
        };
    }
}
