package com.tut2.group3.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("users")
public class User {

    private Integer id;
    private String userName;
    private String password;

}
