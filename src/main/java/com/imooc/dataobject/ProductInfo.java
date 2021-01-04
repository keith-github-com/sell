package com.imooc.dataobject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.imooc.enums.ProductStatusEnum;
import com.imooc.utils.EnumUtil;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;



/**
 * 商品信息
 * Created by 廖师兄
 * 2017-05-09 11:30
 */
@Entity
@Data
@DynamicUpdate  //表示update对象的时候，生成动态的update语句，如果这个字段的值是null就不会被加入到update语句中
@DynamicInsert  //表示insert对象的时候，生成动态的insert语句，如果这个字段的值是null就不会加入到insert语句中
public class ProductInfo {

    @Id
    private String productId;

    /** 名字. */
    private String productName;

    /** 单价. */
    private BigDecimal productPrice;

    /** 库存. */
    private Integer productStock;

    /** 描述. */
    private String productDescription;

    /** 小图. */
    private String productIcon;

    /** 状态, 0正常1下架. */
    private Integer productStatus = ProductStatusEnum.UP.getCode();

    /** 类目编号. */
    private Integer categoryType;

    private Date createTime;

    private Date updateTime;

    //JsonIgnore向前端返回的Json不包含以下的方法或属性
    /** 在枚举类ProductStatusEnum中找出所有的实例序列，根据状态码code找到对应的实例 */
    @JsonIgnore
    public ProductStatusEnum getProductStatusEnum() {
        return EnumUtil.getByCode(productStatus, ProductStatusEnum.class);
    }
}
