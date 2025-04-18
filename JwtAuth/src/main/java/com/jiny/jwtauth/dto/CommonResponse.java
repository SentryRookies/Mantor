package com.jiny.jwtauth.dto;

import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Date;

@Data
public class CommonResponse {
    private String code;
    private String message;
    private String reponseTime;
    private Object response;

    public static CommonResponse ok(Object response) {
        CommonResponse res = new CommonResponse();
        res.code = "SUCCESS";
        res.message = "success";
        res.response = response;
        res.reponseTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        return res;
    }

    public static CommonResponse fail(Exception e) {
        CommonResponse res = new CommonResponse();
        res.code = "FAIL";
        res.message = e.getMessage();
        res.response = e.getClass();
        res.reponseTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        return res;
    }

    public  static CommonResponse fail(String message) {
        CommonResponse res = new CommonResponse();
        res.code = "FAIL";
        res.message = message;
        res.response = message;
        res.reponseTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        return res;
    }
}
