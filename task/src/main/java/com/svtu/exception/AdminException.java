package com.svtu.exception;

public class AdminException extends RuntimeException{
    private final Integer code;

    public AdminException(Integer code,String message) {
        super(message);
        this.code = code;
    }
    public Integer getCode() {
        return code;
    }
}
