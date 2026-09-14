package com.svtu.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;

    public Result(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Result() {
    }

    // 成功
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("success");
        result.setData(data);
        return result;
    }
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("success");
        result.setData(null);
        return result;
    }

    // 失败
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMsg(message);
        result.setData(null);
        return result;
    }



    // ====================== 重点：你缺少这个 ======================
    // 参数错误 / 业务异常（400）
    public static <T> Result<T> badRequest(String message) {
        Result<T> result = new Result<>();
        result.setCode(400);
        result.setMsg(message);
        result.setData(null);
        return result;
    }
}
