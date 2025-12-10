package com.example.edog.utils;

import lombok.Data;

@Data
public class Result<T> {
    private int code; // 0:成功，1:失败
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(0);
        result.setMessage("成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(1);
        result.setMessage(message);
        result.setData(null);
        return result;
    }
}
