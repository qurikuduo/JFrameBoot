package com.jf.controller;

import com.jf.common.BaseController;
import com.jf.entity.ResMsg;
import com.jf.entity.enums.ResCode;
import com.jf.system.annotation.Token;
import com.jf.system.conf.SysConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by xujunfei on 2017/4/7.
 */
@RestController
@RequestMapping("/app")
public class IndexAppController extends BaseController {

    @GetMapping("/home")
    public ResMsg home() {
        return new ResMsg(ResCode.SUCCESS.code(), ResCode.SUCCESS.msg());
    }

    @GetMapping("/online")
    public ResMsg online() {
        return new ResMsg(ResCode.SUCCESS.code(), ResCode.SUCCESS.msg(), true);
    }

    @GetMapping("/test")
    public ResMsg test() {
        return new ResMsg(ResCode.SUCCESS.code(), ResCode.SUCCESS.msg());
    }

    @GetMapping("/get")
    public ResMsg get() {
        Long userId = 10001L;
        String token = bindToken(userId);
        return new ResMsg(ResCode.SUCCESS.code(), ResCode.SUCCESS.msg(), token);
    }

    /**
     * Token测试
     * *注意：第一个参数必须要是Long param_name
     *
     * @param userId
     * @param param
     * @return
     */
    @GetMapping("/token")
    @Token(type = SysConfig.TOKEN_HEADER)
    public ResMsg token(Long userId, String param) {
        System.out.println("/app/token param:" + param);
        System.out.println("/app/token userId:" + userId);
        return new ResMsg(ResCode.SUCCESS.code(), ResCode.SUCCESS.msg(), userId);
    }

    @GetMapping("/token2")
    @Token(type = SysConfig.TOKEN_COOKIE, need = false)
    public ResMsg token2(Long userId, String param) {
        System.out.println("/app/token param:" + param);
        System.out.println("/app/token userId:" + userId);
        return new ResMsg(ResCode.SUCCESS.code(), ResCode.SUCCESS.msg(), userId);
    }

}
