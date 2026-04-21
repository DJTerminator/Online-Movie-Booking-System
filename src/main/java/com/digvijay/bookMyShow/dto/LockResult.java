package com.digvijay.bookMyShow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LockResult<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime lockExpiry;

    public static <T> LockResult<T> success(T data) {
        return new LockResult<>(true, "Success", data, null);
    }

    public static <T> LockResult<T> success(T data, LocalDateTime expiry) {
        return new LockResult<>(true, "Success", data, expiry);
    }

    public static <T> LockResult<T> failure(String message) {
        return new LockResult<>(false, message, null, null);
    }
}
