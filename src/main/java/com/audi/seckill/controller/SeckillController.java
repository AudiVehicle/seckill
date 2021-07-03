package com.audi.seckill.controller;

import com.audi.seckill.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class SeckillController {

    @Autowired
    private ProductService productService;

    @GetMapping("seckill/{product_id}")
    public Boolean seckill(@PathVariable("product_id") Integer productId) {
        log.info("received seckill req, productId = {}", productId);
        return productService.sell(productId);

    }
}
