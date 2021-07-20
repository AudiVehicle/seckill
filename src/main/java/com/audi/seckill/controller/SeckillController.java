package com.audi.seckill.controller;

import com.audi.seckill.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class SeckillController {

    @Autowired
    private ProductService productService;

    @GetMapping("seckill/{productId}")
    public Boolean seckill(@PathVariable("productId") Integer productId) {
        log.info("received seckill req, productId = {}", productId);
        return productService.sell(productId);

    }

    /**
     * 携带用户id进行抢购，一个用户至多只能购买一个抢购商品
     *
     * @param productId
     * @param userId
     * @return
     */
    @GetMapping("seckill/{productId}/{userId}")
    public Boolean seckillWithUserInfo(@PathVariable("productId") Integer productId,
                                       @PathVariable("userId") String userId) {
        log.info("received seckill req, productId = {}, userId = {}", productId, userId);
        return productService.sellWithUserInfo(productId, userId);

    }
}
