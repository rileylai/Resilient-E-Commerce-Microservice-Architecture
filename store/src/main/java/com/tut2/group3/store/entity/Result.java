package com.tut2.group3.store.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//
@AllArgsConstructor
@NoArgsConstructor

@Data
public class Result<T> {
    private int code;   //status code, 200=success; 400=parameter wrong; 500=server error
    private String message;
    private T data;

    //  success: without data
    public static <T> Result<T> success() {
        return new Result<T>(200, "success", null);
    }

    //success: with data
    public static <T> Result<T> success(T data) {
        return new Result<T>(200, "success", data);
    }

    //error
    public static <T> Result<T> error(int code, String message) {
        return new Result<T>(code, message, null);
    }

}
