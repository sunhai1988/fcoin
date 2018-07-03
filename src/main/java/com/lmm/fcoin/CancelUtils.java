package com.lmm.fcoin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CancelUtils {
    public static void main(String[] args) throws Exception {
        List<String> ftusdt = new FcoinUtils().getNotTradeOrders("ftusdt", "1500000000", "400");
        new FcoinUtils().cancelOrders(ftusdt);
    }

    public JSONObject maxPriceJSONobject(JSONArray jsonArray){

        double  priceMax = 0;
        JSONObject maxJson = null;
        if (!CollectionUtils.isEmpty(jsonArray)) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                double  price = jsonObject.getDouble("price");
                if (price > priceMax){
                    priceMax = price;
                    maxJson = jsonObject;
                }
            }
        }
        return maxJson;
    }

    public JSONArray filterBuyArray(JSONArray jsonArray){
        JSONArray buyArray = new JSONArray();
        if (!CollectionUtils.isEmpty(jsonArray)) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String  side = jsonObject.getString("side");
                if ("buy".equals(side)){
                    buyArray.add(jsonObject);
                }
            }
        }
        return buyArray;
    }

    public JSONArray filterSellArray(JSONArray jsonArray){
        JSONArray sellArray = new JSONArray();
        if (!CollectionUtils.isEmpty(jsonArray)) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String  side = jsonObject.getString("side");
                if ("sell".equals(side)){
                    sellArray.add(jsonObject);
                }
            }
        }
        return sellArray;
    }

    public  JSONObject minBuyPriceObject(JSONArray allOrder){
        JSONArray jsonArray = this.filterBuyArray(allOrder);
        return this.minPriceJSONobject(jsonArray);
    }

    public  JSONObject maxBuyPriceObject(JSONArray allOrder){
        JSONArray jsonArray = this.filterBuyArray(allOrder);
        return this.maxPriceJSONobject(jsonArray);
    }

    public  JSONObject maxSellPriceObject(JSONArray allOrder){
        JSONArray jsonArray = this.filterSellArray(allOrder);
        return this.maxPriceJSONobject(jsonArray);
    }

    public  JSONObject minSellPriceObject(JSONArray allOrder){
        JSONArray jsonArray = this.filterSellArray(allOrder);
        return this.minPriceJSONobject(jsonArray);
    }


    public JSONObject minPriceJSONobject(JSONArray jsonArray){

        double  priceMin = 1000000000f;
        JSONObject minJson = null;
        if (!CollectionUtils.isEmpty(jsonArray)) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                double  price = jsonObject.getDouble("price");
                if (price < priceMin){
                    priceMin = price;
                    minJson = jsonObject;
                }
            }
        }
        return minJson;
    }

    public void cancelOrder(JSONObject jsonObject){
        if (jsonObject != null){
            String id = jsonObject.getString("id");
            List<String> array = new ArrayList<>();
            array.add(id);
            try {
                FcoinUtils.cancelOrders(array);
            }catch (Exception e){

            }
        }
    }
}
