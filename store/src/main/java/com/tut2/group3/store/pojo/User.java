package com.tut2.group3.store.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("users")
public class User {

    //Lombok annotation indicating the id field is auto-incrementing
    @TableId(type =IdType.AUTO)
    private Long id;

    @TableField("user_name")
    private String username;
    @JsonIgnore
    private String password;
    private String email;
}
