package com.rickey.apiInterface.controller;

import cn.hutool.http.HttpRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/day")
public class DayController {

    static final String IMAGE_BASE_URL = "https://cn.bing.com";

    @GetMapping("/wallpaper")
    public String getDayWallpaperUrl() throws JsonProcessingException {
        // 直接构造GET请求，传递必要的参数
        String requestUrl = "https://cn.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=zh-CN";
        String body = HttpRequest.get(requestUrl)
                .execute().body();
        // 创建 ObjectMapper 对象
        ObjectMapper mapper = new ObjectMapper();
        // 解析 JSON 字符串为 JsonNode 对象
        JsonNode rootNode = mapper.readTree(body);
        // 获取 images 数组中的第一个对象的 url 属性
        String imageUrl = IMAGE_BASE_URL + rootNode.path("images").get(0).path("url").asText();
        // 返回图片 URL
        return imageUrl;
    }

}
