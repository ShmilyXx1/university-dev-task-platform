package com.svtu.exception;


import lombok.Getter;

@Getter

//用户异常报501
//管理员异常报502
public class UserException extends RuntimeException{
    private final Integer code;

    public UserException(Integer code,String message) {
        super(message);
        this.code = code;
    }

}
