package com.imooc.utils;

import com.imooc.enums.CodeEnum;

/**
 * 通用的枚举类，通过code返回枚举实例
 * Created by 廖师兄
 * 2017-07-16 18:36
 */
public class EnumUtil {
	
	//<T extends CodeEnum>是对枚举范型T的说明，说明枚举类T是继承了CodeEnum
	//Class<T> enumClass输入enum的class类文件
    public static <T extends CodeEnum> T getByCode(Integer code, Class<T> enumClass) {
        for (T each: enumClass.getEnumConstants()) {  //返回枚举数组
            if (code.equals(each.getCode())) {
                return each; //返回对应code的枚举实例
            }
        }
        return null;
    }
}
