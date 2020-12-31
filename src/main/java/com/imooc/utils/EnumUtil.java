package com.imooc.utils;

import com.imooc.enums.CodeEnum;

/**
 * Created by 廖师兄
 * 2017-07-16 18:36
 */
public class EnumUtil {

    public static <T extends CodeEnum> T getByCode(Integer code, Class<T> enumClass) {
        for (T each: enumClass.getEnumConstants()) {  //返回枚举数组
            if (code.equals(each.getCode())) {
                return each; //返回对应code的枚举实例
            }
        }
        return null;
    }
}
