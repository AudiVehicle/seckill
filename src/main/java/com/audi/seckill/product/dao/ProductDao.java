package com.audi.seckill.product.dao;

import com.audi.seckill.product.entity.Product;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductDao extends BaseMapper<Product> {

    int sell(@Param("productId") Integer productId);
}
