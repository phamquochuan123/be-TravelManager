package com.example.travelManager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.lang.reflect.Method;

/**
 * Không có handler này, exception ném ra trong method @Async void (như gửi email)
 * sẽ biến mất hoàn toàn không có log — chỉ có thể bắt qua AsyncUncaughtExceptionHandler.
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return this::handle;
    }

    private void handle(Throwable ex, Method method, Object... params) {
        log.error("Async method {} failed: {}", method.getName(), ex.getMessage(), ex);
    }
}
