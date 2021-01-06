package com.imooc.controller;

import com.imooc.dto.OrderDTO;
import com.imooc.enums.ResultEnum;
import com.imooc.exception.SellException;
import com.imooc.service.OrderService;
import com.imooc.service.PayService;
import com.lly835.bestpay.model.PayResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * 支付
 * Created by 廖师兄
 * 2017-07-04 00:49
 */
@Controller
@RequestMapping("/pay")
public class PayController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PayService payService;

    //实现逻辑：
    //第一步，前端页面调用接口"/sell/pay/create"
    //第二步，通过BestPayServiceImpl发起支付，传入xml格式参数，调用API：https://api.mch.weixin.qq.com/pay/unifiedorder(必须要传入异步接收微信支付结果通知的回调地址notify_url)
    //第三步，返回到"pay/create"模板，并将PayResponse返回给该模板中的JSAPI接口（getBrandWCPayRequest）作为请求参数
    //第四步，跳转至https://jianlin.natapp4.cc/#/order/"相应的orderId"（前端页面）
    
    //这里的入参returnUrl为https://jianlin.natapp4.cc/#/order/"相应的orderId"（前端页面）;
    @GetMapping("/create")
    public ModelAndView create(@RequestParam("orderId") String orderId,
                               @RequestParam("returnUrl") String returnUrl,
                               Map<String, Object> map) {
        //1. 查询订单
        OrderDTO orderDTO = orderService.findOne(orderId);
        if (orderDTO == null) {
            throw new SellException(ResultEnum.ORDER_NOT_EXIST);
        }

        //2. 发起支付，调用JSAPI，业务逻辑详见https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=7_7&index=6
        PayResponse payResponse = payService.create(orderDTO);

        map.put("payResponse", payResponse);
        map.put("returnUrl", returnUrl);

        return new ModelAndView("pay/create", map);
    }

    /**
     * 微信异步通知
     * @param notifyData
     */
    @PostMapping("/notify")
    public ModelAndView notify(@RequestBody String notifyData) {
        payService.notify(notifyData);

        //返回给微信处理结果
        return new ModelAndView("pay/success");
    }
}
