package com.lmm.fcoin;

import java.math.RoundingMode;
import java.text.NumberFormat;

public class NumberFormatUtils {

    public  static  String up(Double d){

        NumberFormat nf = NumberFormat.getNumberInstance();


        // 保留两位小数
        nf.setMaximumFractionDigits(6);


        // 如果不需要四舍五入，可以使用RoundingMode.DOWN
        nf.setRoundingMode(RoundingMode.UP);
        return nf.format(d);
    }
    public  static  String down(Double d){

        NumberFormat nf = NumberFormat.getNumberInstance();

        // 保留两位小数
        nf.setMaximumFractionDigits(6);
        nf.setRoundingMode(RoundingMode.DOWN);
        return nf.format(d);
    }
}
