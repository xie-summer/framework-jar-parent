package com.njq.common.util.grab;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtmlGrabUtil {
    private static final Logger logger = LoggerFactory.getLogger(HtmlGrabUtil.class);
    public static Map<String, HtmlGrabUtil> customMap = new HashMap<>();
    private CookieStore store;
    private RequestConfig requestConfig;

    public HtmlGrabUtil() {
        requestConfig = RequestConfig.custom().setConnectionRequestTimeout(10 * 1000).setConnectTimeout(10 * 1000)
                .setSocketTimeout(10 * 1000).build();
    }

    public CookieStore getCookieStore() {
        return store;
    }

    public String getCookieStr() {
        String str = "";
        if (this.store != null) {
            List<Cookie> cookies = store.getCookies();
            for (int i = 0; i < cookies.size(); i++) {
                str += cookies.get(i).getName() + "=" + cookies.get(i).getValue();
            }
        }
        return str;
    }

    public static HtmlGrabUtil build(String key) {
        if (customMap.get(key) == null) {
            synchronized (customMap) {
                if (customMap.get(key) == null) {
                    customMap.put(key, new HtmlGrabUtil());
                }
            }
        }
        return customMap.get(key);
    }

    public void login(String url, String type, Map<String, String> paramsMap) {
        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        paramsMap.entrySet().forEach(n -> {
            formParams.add(new BasicNameValuePair(n.getKey(), n.getValue()));
        });
        login(url, type, formParams);
    }

    public void login(String url, String type, List<NameValuePair> formParams) {
        store = new BasicCookieStore();
        HttpEntity entity = sendPostFromUrl(url, formParams);
        if (entity == null) {
            return;
        }
        // 转化为文本信息, 设置爬取网页的字符集，防止乱码
        try {
            EntityUtils.toString(entity, SendConstants.ENCODE);
        } catch (ParseException | IOException e) {
            logger.info("登录信息出错", e);
        }
    }

    public HttpEntity sendPostFromUrl(String url, List<NameValuePair> formParams) {
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultCookieStore(store).build();
        HttpResponse response = null;
        try {
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(formParams, SendConstants.ENCODE);
            HttpPost postHttp = new HttpPost(url);
            postHttp.setConfig(requestConfig);
            // 设置HTTP Header
            setPostHeader(postHttp);
            postHttp.setEntity(formEntity);
            response = httpClient.execute(postHttp);
            return response.getEntity();
        } catch (Exception e) {
            logger.info("发送post信息出错", e);
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                logger.info("关闭流出错", e);
            }
        }
        return null;
    }

    public Document getDoc(String url) {
        String doc = getContext(url);
        if (doc == null) {
            return null;
        } else {
            return Jsoup.parse(doc);
        }
    }

    public String getContext(String url) {
        try {
            HttpEntity entity = sendGetFromUrl(url);
            if (entity == null) {
                return "";
            }
            String content = EntityUtils.toString(entity, SendConstants.ENCODE);
            return content;
        } catch (ParseException | IOException e) {
            logger.info("获取信息出错", e);
        }
        return "";
    }

    public HttpEntity sendGetFromUrl(String url) {
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultCookieStore(store).build();
        HttpResponse response = null;
        try {
            HttpGet getHttp = new HttpGet(url);
            getHttp.setConfig(requestConfig);
            // 设置HTTP Header
            setGetHeader(getHttp);
            response = httpClient.execute(getHttp);
            return response.getEntity();
        } catch (Exception e) {
            logger.info("发送get信息出错", e);
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                logger.info("关闭流出错", e);
            }
        }
        return null;
    }

    private void setPostHeader(HttpPost postHttp) {
        postHttp.setHeader(SendConstants.USER_AGENT_NAME, SendConstants.USER_AGENT_VALUE);
        postHttp.addHeader(SendConstants.CONTENT_TYPE_NAME, SendConstants.CONTENT_TYPE_VALUE);
        postHttp.addHeader(SendConstants.X_REQUESTED_WITH_NAME, SendConstants.X_REQUESTED_WITH_VALUE);

        // postHttp.addHeader("Referer","http://wiki.yonghuivip.com/");
        // postHttp.addHeader("Host","wiki.yonghuivip.com");
        // postHttp.addHeader("Origin","http://wiki.yonghuivip.com");
    }

    private void setGetHeader(HttpGet getHttp) {
        getHttp.setHeader(SendConstants.USER_AGENT_NAME, SendConstants.USER_AGENT_VALUE);
        getHttp.addHeader(SendConstants.CONTENT_TYPE_NAME, SendConstants.CONTENT_TYPE_VALUE);
        getHttp.addHeader(SendConstants.X_REQUESTED_WITH_NAME, SendConstants.X_REQUESTED_WITH_VALUE);
    }

}