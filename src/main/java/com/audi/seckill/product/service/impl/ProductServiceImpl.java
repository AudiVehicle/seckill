package com.audi.seckill.product.service.impl;

import com.audi.seckill.product.dao.ProductDao;
import com.audi.seckill.product.entity.Bill;
import com.audi.seckill.product.entity.Product;
import com.audi.seckill.product.service.BillService;
import com.audi.seckill.product.service.ProductService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductDao productDao;

    @Autowired
    private BillService billService;

    @Autowired
    private RedisTemplate redisTemplate;

    private static RedisScript<Long> script = null;

    /**
     * 卖出商品（无锁化）
     *
     * @param productId
     * @return
     */
    @Override
//    public synchronized Boolean sell(Integer productId) {
    public Boolean sell(Integer productId) {

        String redisKey = "product_" + productId;

        Integer rest = (Integer) redisTemplate.opsForValue().get(redisKey);

        if (null == rest || rest <= 0) {
            log.info("商品不存在或者已经卖完");
            return Boolean.FALSE;
        }

        log.info("商品还有余量，可以购买 \n");
        getRedisScript(Boolean.FALSE);

        Long stillRest = (Long) redisTemplate.execute(script, Arrays.asList(redisKey), 1);
        if (stillRest >= 0) {
            log.info("成功抢购到商品");
            return Boolean.TRUE;

        } else {
            log.info("未能成功实现商品抢购");
            return Boolean.FALSE;
        }
    }

    /**
     * 携带用户id进行抢购，一个用户至多只能购买一个抢购商品
     *
     * @param productId
     * @param userId
     * @return
     */
    @Override
    public Boolean sellWithUserInfo(Integer productId, String userId) {

        String billKey = "bill_" + productId;
        String productKey = "product_" + productId;

        Integer rest = (Integer) redisTemplate.opsForValue().get(productKey);

        if (null == rest || rest <= 0) {
            log.info("商品不存在或者已经卖完");
            return Boolean.FALSE;
        }

        log.info("商品还有余量，可以购买 \n");
        getRedisScript(Boolean.TRUE);
        Long stillRest = (Long) redisTemplate.execute(script, Arrays.asList(billKey, userId, productKey), 1);
        if (stillRest >= 0) {
            log.info("成功抢购到商品");
            return Boolean.TRUE;

        } else {
            log.info("未能成功实现商品抢购");
            return Boolean.FALSE;
        }
    }

    /**
     * 定时获取redis的商品余量刷新到数据库
     */
//    @Scheduled(initialDelay = 10 * 1000, fixedRate = 2 * 1000)
    public synchronized void refreshProduct() {
        log.info("开始刷新redis商品余量信息到数据库");

        Integer productId = 1111;
        String redisKey = "product_1111";
        Integer rest = (Integer) redisTemplate.opsForValue().get(redisKey);

        log.info("从redis获取到商品 {} 的余量为 {}", productId, rest);

        Product product = new Product();
        product.setRest(rest);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>().eq(Product::getProductId, productId);

        productDao.update(product, wrapper);


        log.info("结束刷新redis商品余量信息到数据库");
    }

    /**
     * 定时获取redis的商品余量、订单信息刷新到数据库
     * <p>
     * 这里更为委托的做法其实应该是使用分布式锁，但是考虑渐变直接使用synchronized关键字加锁
     */
    @Scheduled(initialDelay = 10 * 1000, fixedRate = 3 * 1000)
    public synchronized void refreshBill() {

        refreshProduct();

        Integer productId = 1111;

        log.info("定时获取redis的订单信息刷新到数据库");

        // 这里数据量大的时候，应该使用游标
        // 但是考虑到实际情况，其实不可能会很大，因为抢购的商品数量是有限的
        Map<String, Integer> map = (Map<String, Integer>) redisTemplate.opsForHash().entries("bill_" + productId);
        // 过滤掉已经持久化过的订单数据
        map = map.entrySet().stream().filter(e -> e.getValue().equals(0)).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));


        if (CollectionUtils.isEmpty(map)) {
            log.info("订单数据已经持久化到数据库，无需后续操作");
            return;
        }


        List<Bill> billList = map.entrySet().stream().map(e -> {
            Bill bill = new Bill();
            bill.setProductId(productId);
            bill.setUserId(e.getKey());
            bill.setCreateAt(System.currentTimeMillis());
            bill.setUpdateAt(System.currentTimeMillis());
            return bill;
        }).collect(Collectors.toList());

        billService.saveBatch(billList);
        log.info("成功刷新订单数据到数据库");

        // 更新redis数据，标记已经持久化过的数据
        map.entrySet().forEach(e -> e.setValue(1));
        redisTemplate.opsForHash().putAll("bill_" + productId, map);
        log.info("成功修改订单数据的持久化标志位");

    }

    /**
     * 根据是否鞋底用户信息判断返回的脚本数据
     *
     * @param userInfo
     */
    private void getRedisScript(Boolean userInfo) {
        if (script != null) {
            return;
        }
        ScriptSource scriptSource = userInfo ? new ResourceScriptSource(new ClassPathResource("seckillWithUserInfo.lua")) :
                new ResourceScriptSource(new ClassPathResource("seckill.lua"));
        String str = null;
        try {
            str = scriptSource.getScriptAsString();
        } catch (IOException e) {
            log.error("获取lua脚本异常:" + e.getMessage(), e);
        }
        script = RedisScript.of(str, Long.class);
    }

}
