package com.audi.seckill.product.constant;


/**
 * redis的key常量
 *
 * @author: WangQuanzhou
 * @date: 2021-07-24 6:05 PM
 */
public interface RedisKeyConst {

    /**
     * 订单redis缓存前缀
     */
    String BILL_PREFIX = "bill_";

    /**
     * 商品redis缓存前缀
     */
    String PRODUCT_PREFIX = "product_";
}
