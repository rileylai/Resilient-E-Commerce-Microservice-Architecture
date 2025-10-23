package com.tut2.group3.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tut2.group3.warehouse.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
