package com.tut2.group3.store.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    //success with data
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }
    //success without data
    public static Result success() {
        return new Result<>(200, "success", null);
    }
    //success with custom message and data
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }
    //business error
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
    //server error
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

}
