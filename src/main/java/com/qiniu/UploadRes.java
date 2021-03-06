package com.qiniu;

import com.google.gson.Gson;
import com.model.UptokenRet;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.StringMap;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by qiniu.
 * 点播云资源上传
 * 上传类型参考  用户文档
 * 上传直接调用 本类的  uploadResource  ，属于直传文件方法，不支持分片
 */
public class UploadRes {

    private static HttpClient httpClient;

    public UploadRes() {
        httpClient = HttpClient.getHttpClient();
    }


    private UptokenRet getVodUptokenV2(String hub, int deadline, String type) {
        Gson gson = new Gson();
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("hub", hub);
        map.put("type", "video");  //默认值
        map.put("deadline", 6 * 3600);  //默认值

        if (deadline > 0) {
            map.put("deadline", deadline);
        }

        if (type != null && type.length() > 0) {
            map.put("type", type);
        }

        String bodyStr = gson.toJson(map);

        String rawUrl = Const.HOST + "/v1/uptokens";

        String auth = httpClient.getHttpRequestSign("POST", rawUrl, bodyStr, true);
        Map<String, Object> ret = httpClient.doRequest("POST", rawUrl, bodyStr, true, auth);

        UptokenRet uptokenRet = gson.fromJson(ret.get("msg").toString(), UptokenRet.class);
        return uptokenRet;
    }

    /**
     * POST /v1/uptokens
     * 上传资源的最主要方法
     * <p>
     * 参数依次的含义： 点播云空间名(对应portal-->点播空间左上角的名字)  ,失效时间，上传文件类型（参考文档），要上传的文件路径，最好用绝对路径
     */
    public Map<String, Object> uploadResource(String hub, int deadline, String type, String key, String path) {
        if (type == null || type.length() <= 0) {
            type = "video";
        }

        UptokenRet uptokenRet = getVodUptokenV2(hub, deadline, type);
        if (uptokenRet!=null){
            System.out.println("print");
            System.out.println(uptokenRet.getKey());
            System.out.println(uptokenRet.getUptoken());
            System.out.println("print end");
        }

        //创建上传对象
        UploadManager uploadManager = new UploadManager();

        Map<String, Object> ret = new HashMap<String, Object>();
        try {
            //调用put方法上传
            Response res = uploadManager.put(path, type.equals("video") && key != null && key.length() > 0 ? key : uptokenRet.getKey(), uptokenRet.getUptoken());
            //打印返回的信息
            System.out.println(res.bodyString());


            StringMap retMap = res.jsonToMap();

            ret.put("code",res.statusCode);
            ret.put("hash", retMap.get("hash"));
            ret.put("key", retMap.get("key"));

        } catch (QiniuException e) {
            Response r = e.response;
            // 请求失败时,返回错误码，由客户自己根据错误码，在前端显示相应的提示信息
            ret.put("code",r.statusCode);
            ret.put("msg",r.toString());  // 错误信息
        }

        return ret;
    }


}
