package com.svtu.util;

import com.alibaba.fastjson.JSON;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Web 通用工具类（传输 JSON 专用）
 */
public class WebUtils {

    /**
     * 向客户端响应 JSON 数据
     * @param response 响应对象
     * @param object 要转成 JSON 的对象
     * @throws IOException IO异常
     */
    public static void writeJson(HttpServletResponse response, Object object) throws IOException {
        // 设置响应类型为 JSON
        response.setContentType("application/json;charset=UTF-8");
        // 把对象转成 JSON 字符串
        String json = JSON.toJSONString(object);
        // 输出给前端
        response.getWriter().write(json);
    }
}
