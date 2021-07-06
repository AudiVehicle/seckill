package com.audi.seckill.product.service.impl;

import com.audi.seckill.product.dao.ProductDao;
import com.audi.seckill.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductDao productDao;

    @Autowired
    private RedisTemplate redisTemplate;

    private static RedisScript<Long> script = null;

    /**
     * 卖出商品
     *
     * @param productId
     * @return
     */
    @Override
//    public synchronized Boolean sell(Integer productId) {
    public Boolean sell(Integer productId) {

        Integer rest = (Integer) redisTemplate.opsForValue().get(productId);

        if (null == rest || rest <= 0) {
            log.info("商品不存在或者已经卖完");
            return Boolean.FALSE;
        }

        log.info("商品还有余量，可以购买 \n");
        getRedisScript();
        Long stillRest = (Long) redisTemplate.execute(script, Arrays.asList(productId), 1);
        if (stillRest >= 0) {
            log.info("成功抢购到商品");
            return Boolean.TRUE;

        } else {
            log.info("未能成功实现商品抢购");
            return Boolean.FALSE;
        }
    }

    private void getRedisScript() {
        if (script != null) {
            return;
        }
        ScriptSource scriptSource = new ResourceScriptSource(new ClassPathResource("seckill.lua"));
        String str = null;
        try {
            str = scriptSource.getScriptAsString();
        } catch (IOException e) {
            log.error("获取lua脚本异常:" + e.getMessage(), e);
        }
        script = RedisScript.of(str, Long.class);
    }

}
