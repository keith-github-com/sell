package com.imooc.enums;

/**
 * Created by 廖师兄
 * 2017-07-16 18:16
 */

//由于EnumUtil.getByCode要求输入code，所以让各个枚举类都包括一个code属性
public interface CodeEnum {
	//这是枚举类code属性的getter方法
    Integer getCode();
}
