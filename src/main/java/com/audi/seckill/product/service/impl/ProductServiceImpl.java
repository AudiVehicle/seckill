package com.audi.seckill.product.service.impl;

import com.audi.seckill.product.dao.ProductDao;
import com.audi.seckill.product.entity.Product;
import com.audi.seckill.product.service.ProductService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductDao productDao;

    /**
     * 卖出商品
     *
     * @param productId
     * @return
     */
    @Override
//    public synchronized Boolean sell(Integer productId) {
    public synchronized Boolean sell(Integer productId) {

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>().eq(Product::getProductId, productId);
        Product product = productDao.selectOne(wrapper);

        if (null == product || product.getRest() <= 0) {
            log.info("商品不存在或者已经卖完");
            return Boolean.FALSE;
        }

        log.info("商品还有余量，可以购买 \n");
        product.setRest(product.getRest() - 1);
        productDao.updateById(product);

        return Boolean.TRUE;
    }
}
