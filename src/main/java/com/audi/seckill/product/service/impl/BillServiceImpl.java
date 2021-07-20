package com.audi.seckill.product.service.impl;

import com.audi.seckill.product.dao.BillDao;
import com.audi.seckill.product.entity.Bill;
import com.audi.seckill.product.service.BillService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class BillServiceImpl  extends ServiceImpl<BillDao, Bill> implements BillService {
}
