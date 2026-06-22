package com.app.common;

import java.io.Serializable;

public class CResult<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;

    public static <T> CResult<T> success(String message, T data) {
        CResult<T> r = new CResult<>();
        r.code = 200;
        r.message = message;
        r.data = data;
        return r;
    }

    public static <T> CResult<T> error(String message) {
        CResult<T> r = new CResult<>();
        r.code = 500;
        r.message = message;
        return r;
    }

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
