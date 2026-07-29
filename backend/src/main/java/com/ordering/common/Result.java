package com.ordering.common;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private T data;
    private String message;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(0);
        r.setData(data);
        r.setMessage("success");
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> error(String message) {
        return error(1, message);
    }
}
