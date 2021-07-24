package com.audi.seckill.product.config;

import com.audi.seckill.product.constant.RedisKeyConst;
import com.audi.seckill.product.dao.ProductDao;
import com.audi.seckill.product.entity.Product;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
public class LoadDataConfig {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ProductDao productDao;

    /**
     * 预加载商品余量数据到redis
     * <p>
     * 其实标准的做法应该是写成一个专用接口，并控制好权限，可以通过web调用来进行数据预加载
     */
    @PostConstruct
    public void loadProductData() {
        log.info("开始预加载商品数据到redis");
        Integer productId = 1111;
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>().eq(Product::getProductId, productId);
        Product product = productDao.selectOne(wrapper);
        // 商品余量信息加载到redis，key = product_1111  value = 商品余量
        redisTemplate.opsForValue().set(RedisKeyConst.PRODUCT_PREFIX + productId, product.getRest());
        log.info("预加载商品数据到redis成功");
    }
}
