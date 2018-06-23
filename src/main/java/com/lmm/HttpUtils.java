package com.lmm;

import com.alibaba.fastjson.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class HttpUtils {


    public static Long getServiceTime(){
        ResponseEntity<String> fcoinServiceTime = new RestTemplate().getForEntity("https://api.fcoin.com/v2/public/server-time", String.class);
        JSONObject jsonObject = JSONObject.parseObject(fcoinServiceTime.getBody());
        if (jsonObject != null){
           return jsonObject.getLong("data");
        }
        return System.currentTimeMillis();

    }

}

