package com.audi.seckill.product.service;

public interface ProductService {

    Boolean sell(Integer productId);

    /**
     * 携带用户id进行抢购，一个用户至多只能购买一个抢购商品
     *
     * @param productId
     * @param userId
     * @return
     */
    Boolean sellWithUserInfo(Integer productId, String userId);
}
