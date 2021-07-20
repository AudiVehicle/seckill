
# 基础信息

- spring-boot：2.5.2
- mybatis-plus：3.4.3
- mysql：8.0.25
- jmeter：5.4.1
- redis：
- vm配置：-Xmx500m -Xms500m -Xmn150m -XX:MetaspaceSize=200m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/dump -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:+UseG1GC -Xloggc:/Users/wangquanzhou/ideaProj/seckill/gc.log

# tag 1.0 版本

完成了最基本的商品秒杀功能，并且没有做任何的并发控制。

## 测试过程

使用jmeter往`http://127.0.0.1:8080/seckill/1111`发请求，`

jmeter测试参数及结果：

| 线程数        | 启动时间(秒)   |  是否正常  |
| --------   | :-----:  | :----:  |
| 150     | 5 |   是，余量为0     |
| 150        |   4   |   是，余量为0   |
| 150        |    3    |  是，余量为0  |
| 150        |    2    |  是，余量为0  |
| 150        |    1    |  否，余量为40  |

从测试结果看有两点需要注意：
- 当线程数150，1秒启动所有线程的情况下，秒杀会出现问题，且不是超卖，而是`少卖`了，这是为何呢？
- 其他情况，虽然显示余量为0了，但是是不是意味着真的只有100人（商品总量为100）抢到了商品呢？

## 结果分析

针对上面提出的两个问题，我们分析一下原因。这里重申一下，tag1.0版本我们没有采取任何防并发的措施。

### 为什么会少卖

首先确认数据库的隔离级别是`REPEATABLE-READ`，mysql会采用快照读（snapshot）的方式，这是出现这个问题的根本原因。

举个例子，假如有A、B两个线程，A读取到商品余量为100，然后减1，更新到数据库，此时A还未commit。B线程此时读取商品余量，
由于snapshot的应用，也是100，同样的减1，更新到数据库，

相当于mysql被更新了两次，但是都是从100更新到99。所以才会出现`少卖`的情况。

### 是不是真的只有100人抢到

通过搜索日志，我们发现，`商品还有余量，可以购买`这句日志被打印了134次，说明有134个人抢购成功了，出现了`超卖`的情况，
但是却没有出现商品余量小于0的情况。这是为何？

![1](./image/WX20210704-105732@2x.png)

分析一下抢购的源码，如下所示：
```java
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
```

还是以A、B线程为例，假设现在数据库余量为1，A线程读取商品余量为1，if语句可以通过，然后执行减1的操作，更新到数据库，但是还未commit；
B线程此时由于mysql snapshot的存在，读取到的余量也为1，同样执行减1操作，1-1=0，因此不会出现小于0的情况。

## 性能统计

经过反复多次尝试，使用300个线程，1秒启动，tps大概在200左右。


# tag 2.0 版本

为了解决1.0版本的`少卖`、`超卖`问题，我们需要对代码做点修改，最简单的办法就是加锁，有两种方案：
- 代码加锁
- 数据库加锁

## 代码加锁

最简单的办法就是在秒杀的核心代码上加`synchronized`关键字，如下所示：
```java
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
```

虽然可以解决并发安全问题，但是这无疑也会使新根极具下降，使用300个线程，1秒启动，tps大概只有100左右，与不加锁时相比，直接下降了一半。

## 数据库加锁

其实不是数据库加锁，是通过update语句加上条件判断，并且需要根据update语句影响的行数来判断是否真正秒杀成功。

java代码如下：
```java
    public Boolean sell(Integer productId) {

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>().eq(Product::getProductId, productId);
        Product product = productDao.selectOne(wrapper);

        if (null == product || product.getRest() <= 0) {
            log.info("商品不存在或者已经卖完");
            return Boolean.FALSE;
        }

        log.info("商品还有余量，可以购买 \n");
        int count = productDao.sell(product.getProductId());
        if (count != 1) {
            log.info("未能成功实现商品抢购");
            return Boolean.FALSE;
        }
        log.info("成功抢购到商品");
        return Boolean.TRUE;
    }
```

sql语句如下：
```sql
    <update id="sell">
         update product
              set rest = rest-1
              where product_id=#{productId} and rest > 0
    </update>
```

这种方式虽然也能实现并发控制，且我本机实测性能，300个线程，1秒启动，tps与不加任何锁的情况，差不了多少，大约差10左右。

但是这种方式，相当于将并发的控制完全交给了数据库，一方面数据库本身执行sql压力大，另外一方面也会占用很多数据库连接池资源。
因此，这也是一种不友好的方案。

# tag 3.0 版本

都说redis是提高系统性能的利器，那我们看看我们怎么使用redis既可以提高性能，又能保证并发安全。

参考阿里云的[redis秒杀系统设计](https://help.aliyun.com/document_detail/63920.html?spm=5176.22414175.sslink.10.252c65aaa3OzBs) 
这里我采用的方案也是异步扣减库存的模式，即redis预先使用lua脚本进行库存扣减操作，然后再使用异步线程定时去扫redis里剩余的库存量。

lua脚本如下：
```lua
local resultFlag = 0
local key = tonumber(KEYS[1])
local rest = tonumber(redis.call("GET", key))
if not rest then
    return resultFlag
end
if rest > 0 then
    local ret = redis.call("DECR", key)
    return ret
end
return resultFlag
```
这里我只传入了一个参数`KEYS[1]`，代表了商品的id。脚本首先获取商品的余量，如果余量大于0，就进行库存减1操作。

后续，异步定时任务会每隔2秒将商品余量数据刷新到redis。

实测时，发现tomcat的最大线程数会成为系统瓶颈，将最大线程数改为500，，系统的tps大概可以达到550左右。

# tag 4.0 版本

在这个版本上，我们给抢购加上用户的限制，即一个用户只允许购买一个抢购商品。

为了实现这个功能，我们需要记录哪些用户已经成功抢购了商品，自然而然的就引出了订单的这个概念，之前我们讨论的情况还仅仅限于`减库存`.

基本实现了一个商品抢购、订单生成、订单持久化的逻辑，目前测试性能还不错，后续需要做相关的优化，包括规范变量命名等。

后续实现商品的退单功能。