package com.imooc.service.impl;

import com.imooc.dto.OrderDTO;
import com.imooc.enums.ResultEnum;
import com.imooc.exception.SellException;
import com.imooc.service.OrderService;
import com.imooc.service.PayService;
import com.imooc.utils.JsonUtil;
import com.imooc.utils.MathUtil;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.model.RefundRequest;
import com.lly835.bestpay.model.RefundResponse;
import com.lly835.bestpay.service.impl.BestPayServiceImpl;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import lombok.extern.slf4j.Slf4j;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by 廖师兄
 * 2017-07-04 00:54
 */
/**
 * @author Administrator
 *
 */
@Service
@Slf4j
public class PayServiceImpl implements PayService {

    private static final String ORDER_NAME = "微信点餐订单";

    @Autowired
    private BestPayServiceImpl bestPayService;

    @Autowired
    private OrderService orderService;

    //service实现逻辑：
    //第一步，yml文件：配置相关的ID与密钥；
    //第二步，WechatAccountConfig：引入配置属性（公众平台Id、公众平台密钥、商户号、商户密钥、商户证书路径、微信支付异步通知地址）
    //第三步，WechatPayConfig（配置容器）：将WechatAccountConfig配置属性写入到WxPayH5Config对象（BEAN）中，再将WxPayH5Config对象配置引入到BestPayServiceImpl对象（BEAN）中
    //第四步，PayServiceImpl：已配置好的BestPayServiceImpl对象（BEAN），调用pay方法发起微信支付，pay方法要求payRequest入参，方法内形成XML格式参数，调用统一下单接口（https://api.mch.weixin.qq.com/pay/unifiedorder），并将XML返回结果转为PayResponse
    //第五步，PayResponse返回给网页端中的接口（getBrandWCPayRequest）作为请求参数
    
    
    @Override
    public PayResponse create(OrderDTO orderDTO) {
        PayRequest payRequest = new PayRequest();
        payRequest.setOpenid(orderDTO.getBuyerOpenid());
        payRequest.setOrderAmount(orderDTO.getOrderAmount().doubleValue());
        payRequest.setOrderId(orderDTO.getOrderId());
        payRequest.setOrderName(ORDER_NAME);
        payRequest.setPayTypeEnum(BestPayTypeEnum.WXPAY_H5);
        log.info("【微信支付】发起支付, request={}", JsonUtil.toJson(payRequest));

        //BestPayServiceImpl可支持微信支付、支付宝支付
        PayResponse payResponse = bestPayService.pay(payRequest);
        log.info("【微信支付】发起支付, response={}", JsonUtil.toJson(payResponse));
        return payResponse;
    }

    //支付成功后，返回给微信前端的通知
    @Override
    public PayResponse notify(String notifyData) {
        //1. 验证签名
        //2. 支付的状态
        //3. 支付金额
        //4. 支付人(下单人 == 支付人)

        PayResponse payResponse = bestPayService.asyncNotify(notifyData);
        log.info("【微信支付】异步通知, payResponse={}", JsonUtil.toJson(payResponse));

        //查询订单
        OrderDTO orderDTO = orderService.findOne(payResponse.getOrderId());

        //判断订单是否存在
        if (orderDTO == null) {
            log.error("【微信支付】异步通知, 订单不存在, orderId={}", payResponse.getOrderId());
            throw new SellException(ResultEnum.ORDER_NOT_EXIST);
        }

        //判断金额是否一致(0.10   0.1)
        if (!MathUtil.equals(payResponse.getOrderAmount(), orderDTO.getOrderAmount().doubleValue())) {
            log.error("【微信支付】异步通知, 订单金额不一致, orderId={}, 微信通知金额={}, 系统金额={}",
                    payResponse.getOrderId(),
                    payResponse.getOrderAmount(),
                    orderDTO.getOrderAmount());
            throw new SellException(ResultEnum.WXPAY_NOTIFY_MONEY_VERIFY_ERROR);
        }

        //修改订单的支付状态
        orderService.paid(orderDTO);

        return payResponse;
    }

    /**
     * 退款 调用的接口https://api.mch.weixin.qq.com/secapi/pay/refund
     * @param orderDTO
     */
    @Override
    public RefundResponse refund(OrderDTO orderDTO) {
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setOrderId(orderDTO.getOrderId());
        refundRequest.setOrderAmount(orderDTO.getOrderAmount().doubleValue());
        refundRequest.setPayTypeEnum(BestPayTypeEnum.WXPAY_H5);
        log.info("【微信退款】request={}", JsonUtil.toJson(refundRequest));

        RefundResponse refundResponse = bestPayService.refund(refundRequest);
        log.info("【微信退款】response={}", JsonUtil.toJson(refundResponse));

        return refundResponse;
    }
}

/**
 *  备注：WxPayServiceImpl中的pay及refund命令均用到OkHttp及retrofit2
 *  一、OkHttp：ref=https://www.jianshu.com/p/9aa969dd1b4d
 *  （一）作用：网络请示框架
 *  （二）使用步骤——以Post请示为例
 *  1、拿到OkHttpClient对象：OkHttpClient client = new OkHttpClient();
 *  2、构建FormBody,传入参数：
 *		FormBody formBody = new FormBody.Builder()
 *                   .add("username", "admin")
 *                   .add("password", "admin")
 *                   .build();
 *     //（1）json字符串的构造方法，以向服务端发送json字符串：RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain;charset=utf-8"), "{username:admin;password:admin}");
 *     //（2）构建表单提交（注意FormBody是RequestBody的子类）代码：
 *      File file = new File(Environment.getExternalStorageDirectory(), "1.png");
 *		if (!file.exists()){
 *		    Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
 *		    return;
 *		}
 *		RequestBody muiltipartBody = new MultipartBody.Builder()
 *		        //一定要设置这句
 *		        .setType(MultipartBody.FORM)
 *		        .addFormDataPart("username", "admin")//
 *		        .addFormDataPart("password", "admin")//
 *		        .addFormDataPart("myfile", "1.png", RequestBody.create(MediaType.parse("application/octet-stream"), file))
 *		        .build();
 *	  //如果提交的是表单,一定要设置setType(MultipartBody.FORM)这一句；
 *	  //myfile就是类似于键值对的键,是供服务端使用的,就类似于网页表单里面的name属性；
 *  3、构建Request,将FormBody作为Post方法的参数传入：
 *  	final Request request = new Request.Builder()
 *                   .url("http://www.jianshu.com/")
 *                   .post(formBody)
 *                   .build();
 *  4、将Request封装为Call：
 *  	Call call = client.newCall(request);
 *  5、根据需要调用同步或者异步请求方法：
 *  //同步调用,返回Response,会抛出IO异常：Response response = call.execute();
 *  //异步调用,并设置回调函数：call.enqueue(new Callback() {...});
 *  
 *  二、retrofit2：ref=https://blog.csdn.net/qq_35229022/article/details/93849660
 *  （一）作用：OkHttp的加强版，它也是一个网络加载框架。底层是使用OKHttp封装的。网络请求的工作本质上是OkHttp完成，而 Retrofit 仅负责网络请求接口的封装。
 *  （二）好外：
 *  1、超级解耦：API接口定义和API接口使用，传参、回调等方面的解耦
 *  2、可以配置不同HttpClient来实现网络请求，如OkHttp、HttpClient
 *  3、支持同步、异步和RxJava
 *  4、可以配置不同的反序列化工具来解析数据，如json、xml
 *  5、请求速度快，使用非常方便灵活
 *  （三）使用步骤：
 *  1、创建接收服务器返回数据的类（如WxPaySyncResponse类），根据返回数据的格式和数据解析方式（Json、XML等）定义
 *  2、创建用于描述网络请求的接口（如WxPayApi接口）：
 *   ①Retrofit将Http请求抽象成Java接口，并在接口里面采用注解来配置网络请求参数。用动态代理将该接口的注解“翻译”成一个Http请求，最后再执行Http请求
 *    注意：接口中的每个方法的参数都需要使用注解标注@Body（接口的请求参数），否则会报错
 *   ②APi接口中的Responsebody是Retrofit网络请求回来的原始数据类，没经过Gson转换；如果不想转换，可把Call的泛型定义为Call<ResponseBody>
 *   ③POST注解：说白了就是我们的Post请求方式，注解的属性值会与BaseUrl拼接。
 *  3、创建Retrofit对象
 *   Retrofit retrofit = new Retrofit.Builder()
 *   设置数据解析器：.addConverterFactory(SimpleXmlConverterFactory.create())，使得来自接口的xml结果会自动解析成定义好的字段和类型都相符的xml对象接受类。
 *   设置网络请求的Url地址：.baseUrl("http://apis.baidu.com/txapi/")
 *   创建：.build();
 *  4、创建网络请求接口的实例
 *   mApi = retrofit.create(APi.class);
 *  5、发起网络请求
 *  //同步调用,返回Response,会抛出IO异常：Response<WxPaySyncResponse> response = call.execute();
 *  //异步调用,并设置回调函数：call.enqueue(new Callback() {...});
 * 
 */

