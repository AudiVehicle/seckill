package com.audi.seckill.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;


/**
 * 账单表
 *
 * @author: WangQuanzhou
 * @date: 2021-07-18 11:04 PM
 */
@Data
public class Bill {

    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * 商品id
     */
    private Integer productId;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 创建时间
     */
    private Long createAt;

    /**
     * 更新时间，主要用于用户取消订单的情况
     */
    private Long updateAt;
}
