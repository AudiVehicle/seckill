package com.audi.seckill.product.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * 商品id
     */
    private Integer productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品总量
     */
    private Integer total;

    /**
     * 商品余量
     */
    private Integer rest;

}
