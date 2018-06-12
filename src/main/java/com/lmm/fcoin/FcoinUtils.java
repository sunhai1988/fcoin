package com.lmm.fcoin;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FcoinUtils {

    private static final RetryTemplate retryTemplate = FcoinRetry.getRetryTemplate();

    private static final Logger logger = LoggerFactory.getLogger(FcoinUtils.class);
    //private static final String app_key = "42ffbdf4df994f1a8a181350e5b24541";
    //private static final String app_secret = "7ae3e81c0e8e47a4b604eeeca39be6ec";

    private static final String app_key = "c3d63dbd27714ca8a0887c938c4e8efe";
    private static final String app_secret = "b78eadff63b1414fbd05a449e383c92d";

    private static final double initUstd = 3000;
    private static final double maxUstd = 1000;
    private static final double minUstd = 50;

    public static BigDecimal getBigDecimal(double value, int scale) {
        return new BigDecimal(value).setScale(scale, BigDecimal.ROUND_HALF_UP);
    }

    public static String getSign(String data, String secret) throws Exception {

        String base64_1 = Base64.getEncoder().encodeToString(data.getBytes("utf-8"));
        SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes("utf-8"), "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(base64_1.getBytes("utf-8"));
        return Base64.getEncoder().encodeToString(rawHmac);
    }

    public static String getBalance() throws Exception {
        String url = "https://api.fcoin.com/v2/accounts/balance";
        Long timeStamp = System.currentTimeMillis();
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("FC-ACCESS-KEY", app_key);
        headers.add("FC-ACCESS-SIGNATURE", getSign("GET" + url + timeStamp, app_secret));
        headers.add("FC-ACCESS-TIMESTAMP", timeStamp.toString());

        HttpEntity requestEntity = new HttpEntity<>(headers);
        RestTemplate client = new RestTemplate();
        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.GET, requestEntity, String.class);
        logger.info(response.getBody());
        return response.getBody();
    }

    public static void subBuy(String symbol, String type, String amount) throws Exception {
        String side = "buy";
        String url = "https://api.fcoin.com/v2/orders";
        Long timeStamp = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.add("FC-ACCESS-KEY", app_key);
        headers.add("FC-ACCESS-SIGNATURE",
                getSign("POST" + url + timeStamp + "amount=" + amount + "&side=" + side + "&symbol=" + symbol + "&type=" + type, app_secret));
        headers.add("FC-ACCESS-TIMESTAMP", timeStamp.toString());
        MediaType t = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(t);
        headers.setAccept(Collections.singletonList(MediaType.ALL));

        //  封装参数，千万不要替换为Map与HashMap，否则参数无法传递
        Map<String, String> params = new HashMap<>();
        //  也支持中文
        params.put("amount", amount);
        params.put("side", side);
        params.put("symbol", symbol);
        params.put("type", type);
        String param = JSON.toJSONString(params);
        logger.info(param);
        HttpEntity<String> requestEntity = new HttpEntity<String>(param, headers);
        RestTemplate client = new RestTemplate();
        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.POST, requestEntity, String.class);
        logger.info(response.getBody());
    }

    public static void buy(String symbol, String type, BigDecimal amount) throws Exception {
        BigDecimal maxUsdtDecimal = getBigDecimal(maxUstd, 2);
        while (amount.doubleValue() > 0) {
            if (amount.compareTo(maxUsdtDecimal) > 0) {
                subBuy(symbol, type, maxUsdtDecimal.toString());
            } else {
                subBuy(symbol, type, amount.toString());
                break;
            }
            amount = amount.subtract(maxUsdtDecimal);

            Thread.sleep(5000);
        }

    }

    public static void sell(String symbol, String type, BigDecimal amount, double marketPrice) throws Exception {
        BigDecimal maxUsdtDecimal = getBigDecimal(maxUstd, 2);
        BigDecimal coinValue = getBigDecimal(amount.doubleValue() * marketPrice, 2);
        while (amount.doubleValue() > 0) {
            BigDecimal sellNum = getBigDecimal(maxUsdtDecimal.doubleValue() / marketPrice, 2);
            if (coinValue.compareTo(maxUsdtDecimal) > 0) {
                subSell(symbol, type, sellNum.toString());
            } else {
                subSell(symbol, type, amount.toString());
                break;
            }
            amount = amount.subtract(sellNum);

            Thread.sleep(5000);
        }
    }

    public static void subSell(String symbol, String type, String amount) throws Exception {
        String side = "sell";
        String url = "https://api.fcoin.com/v2/orders";
        Long timeStamp = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.add("FC-ACCESS-KEY", app_key);
        headers.add("FC-ACCESS-SIGNATURE",
                getSign("POST" + url + timeStamp + "amount=" + amount + "&side=" + side + "&symbol=" + symbol + "&type=" + type, app_secret));
        headers.add("FC-ACCESS-TIMESTAMP", timeStamp.toString());

        MediaType t = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(t);
        headers.setAccept(Collections.singletonList(MediaType.ALL));

        //  封装参数，千万不要替换为Map与HashMap，否则参数无法传递
        Map<String, String> params = new HashMap<>();
        //  也支持中文
        params.put("amount", amount);
        params.put("side", side);
        params.put("symbol", symbol);
        params.put("type", type);
        String param = JSON.toJSONString(params);
        logger.info(param);
        HttpEntity<String> requestEntity = new HttpEntity<String>(param, headers);
        RestTemplate client = new RestTemplate();
        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.POST, requestEntity, String.class);
        logger.info(response.getBody());
    }

    public static double getFtUsdtPrice() throws Exception {
        String url = "https://api.fcoin.com/v2/market/ticker/ftusdt";
        Long timeStamp = System.currentTimeMillis();
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("FC-ACCESS-KEY", app_key);
        headers.add("FC-ACCESS-SIGNATURE", getSign("GET" + url + timeStamp, app_secret));
        headers.add("FC-ACCESS-TIMESTAMP", timeStamp.toString());

        HttpEntity requestEntity = new HttpEntity<>(headers);
        RestTemplate client = new RestTemplate();
        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.GET, requestEntity, String.class);
        JSONObject jsonObject = JSON.parseObject(response.getBody());
        JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("ticker");
        BigDecimal b = new BigDecimal(jsonArray.get(0).toString()).setScale(3, BigDecimal.ROUND_HALF_UP);
        return b.doubleValue();
    }

    public static String getSymbols() throws Exception {
        String url = "https://api.fcoin.com/v2/public/symbols";
        Long timeStamp = System.currentTimeMillis();
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("FC-ACCESS-KEY", app_key);
        headers.add("FC-ACCESS-SIGNATURE", getSign("GET" + url + timeStamp, app_secret));
        headers.add("FC-ACCESS-TIMESTAMP", timeStamp.toString());

        HttpEntity requestEntity = new HttpEntity<>(headers);
        RestTemplate client = new RestTemplate();
        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.GET, requestEntity, String.class);
        logger.info(response.getBody());
        return response.getBody();
    }

    //ftusdt
    public void ftusdt() throws Exception {
        double marketPrice = getFtUsdtPrice();

        while (true) {
            //查询余额
            String balance = null;
            try {
                balance = retryTemplate.execute(retryContext ->
                        getBalance()
                );
            } catch (Exception e) {
                logger.error("==========fcoinUtils.getBalance重试后还是异常============", e);
                continue;
            }

            Map<String, Balance> balances = buildBalance(balance);
            Balance ftBalance = balances.get("ft");
            Balance usdtBalance = balances.get("usdt");

            double ft = ftBalance.getBalance();
            double usdt = usdtBalance.getBalance();
            //判断是否有冻结的，如果冻结太多冻结就休眠，进行下次挖矿
            if (ftBalance.getFrozen() > 0.1 * ft || usdtBalance.getFrozen() > 0.1 * usdt) {
                Thread.sleep(3000);
                continue;
            }

            logger.info("===============balance: usdt:{},ft:{}========================", usdt, ft);

            //usdt小于51并且ft的价值小于51
            if ((usdt < minUstd + 1 && ft < (minUstd + 1 / marketPrice))
                    || (usdt < minUstd + 1 && Math.abs(ft * marketPrice - usdt) < 11)
                    || (ft < (minUstd + 1 / marketPrice) && Math.abs(ft * marketPrice - usdt) < 11)) {
                logger.info("跳出循环，ustd:{}, marketPrice:{}", usdt, marketPrice);
                break;
            }

            //ft:usdt=1:0.6
            double ftValue = ft * marketPrice;
            if (ftValue < initUstd || usdt < initUstd) {
                //需要去初始化了
                if (isHaveInitBuyAndSell(ft, usdt, marketPrice, "ftusdt", "market")) {
                    //进行了两个币种的均衡，去进行余额查询，并判断是否成交完
                    logger.info("================有进行初始化均衡操作=================");
                    continue;
                }
            }

            //买单 卖单
            double half = (ft * marketPrice + usdt) / 2;
            double price = Math.min(Math.max(half * 0.9, minUstd), maxUstd);

            BigDecimal ustdAmount = getBigDecimal(price, 2);
            BigDecimal ftAmount = getBigDecimal(price / marketPrice, 2);
            logger.info("=============================交易对开始=========================");

            try {
                retryTemplate.execute(retryContext -> {
                    buy("ftusdt", "market", ustdAmount);
                    return null;
                });
            } catch (Exception e) {
                logger.error("==========fcoinUtils.buy 重试后还是异常============", e);
            }

            try {
                retryTemplate.execute(retryContext -> {
                    sell("ftusdt", "market", ftAmount, marketPrice);
                    return null;
                });
            } catch (Exception e) {
                logger.error("==========fcoinUtils.sell 重试后还是异常============", e);
            }
            logger.info("=============================交易对结束=========================");

            Thread.sleep(1000);
        }
    }

    private boolean isHaveInitBuyAndSell(double ft, double usdt, double marketPrice, String symbol, String type) throws Exception {
        //对半计算
        double half = (ft * marketPrice + usdt) / 2;
        //初始化小的
        if (ft * marketPrice < half && Math.abs(ft * marketPrice - usdt) > 10) {
            //买ft
            double num = Math.min(half - ft * marketPrice, initUstd);
            BigDecimal b = getBigDecimal(num, 2);
            try {
                initBuy(symbol, type, b);
            } catch (Exception e) {
                logger.error("初始化买有异常发生", e);
            }

        } else if (usdt < half && Math.abs(ft * marketPrice - usdt) > 10) {
            //卖ft
            double num = Math.min(half - usdt, initUstd);
            BigDecimal b = getBigDecimal(num / marketPrice, 2);
            try {
                initSell(symbol, type, b, marketPrice);
            } catch (Exception e) {
                logger.error("初始化卖有异常发生", e);
            }
        } else {
            return false;
        }

        Thread.sleep(3000);
        return true;
    }

    private void initBuy(String symbol, String type, BigDecimal usdt) throws Exception {
        //不需要重试
        buy(symbol, type, usdt);
    }

    private void initSell(String symbol, String type, BigDecimal coin, double marketPrice) throws Exception {
        //不需要重试
        sell(symbol, type, coin, marketPrice);
    }

    private Map<String, Balance> buildBalance(String balance) {
        Map<String, Balance> map = new HashMap<>();

        JSONObject jsonObject = JSON.parseObject(balance);
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        jsonArray.stream().forEach(jsonObj -> {
            JSONObject obj = (JSONObject) jsonObj;
            Balance balanceVo = new Balance();
            balanceVo.setAvailable(Double.valueOf(obj.getString("available")));
            balanceVo.setBalance(Double.valueOf(obj.getString("balance")));
            balanceVo.setFrozen(Double.valueOf(obj.getString("frozen")));
            map.put(obj.getString("currency"), balanceVo);
        });

        return map;
    }
}
