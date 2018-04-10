package com.tunion.chain.litecoin.controller;

import com.tunion.chain.litecoin.service.LitecoinService;
import com.tunion.cores.result.Results;
import com.tunion.cores.tools.HttpClientRequest;
import com.tunion.cores.tools.JavaReflectUtil;
import com.tunion.cores.utils.CommConstants;
import com.tunion.cores.utils.JacksonUtil;
import com.tunion.cores.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Think on 2018/2/24.
 */
@Controller
@RequestMapping(value = "/bitcoincash")
public class LitecoinController {
    private static Logger logger = LoggerFactory.getLogger(LitecoinController.class);

    @Autowired
    private LitecoinService bitcoinCashService;

    @RequestMapping(value = "createAddress")
    @ResponseBody
    public Results createAddress(HttpServletRequest request)
    {
        Results results=null;
        //业务逻辑
        try{
            // 读取请求内容
            String parmaJson = HttpClientRequest.parseRequestBody(request);

            logger.info("请求参数：" + parmaJson);

            Map<String, Object> searchParams = null;
            if(!StringUtil.isNullStr(parmaJson))
                searchParams = (Map) JacksonUtil.getJacksonObj(parmaJson, HashMap.class);

            results = bitcoinCashService.createAddress(JavaReflectUtil.obj2str(searchParams.get("accountName")));

        }catch(Exception e){

            e.printStackTrace();
            results = new Results(
                    CommConstants.API_RETURN_STATUS.SERVER_INTERNAL_ERROR.value(),
                    CommConstants.API_RETURN_STATUS.SERVER_INTERNAL_ERROR.desc());
        }

        return results;
    }

    @RequestMapping(value = "withdrawalCash")
    @ResponseBody
    public Results withdrawalCash(HttpServletRequest request)
    {
        Results results=null;
        //业务逻辑
        try{
            // 读取请求内容
            String parmaJson = HttpClientRequest.parseRequestBody(request);

            logger.info("请求参数：" + parmaJson);

            Map<String, Object> searchParams = null;
            if(!StringUtil.isNullStr(parmaJson))
                searchParams = (Map) JacksonUtil.getJacksonObj(parmaJson, HashMap.class);

            results = bitcoinCashService.withdrawalCash(JavaReflectUtil.obj2str(searchParams.get("accountName")),JavaReflectUtil.obj2str(searchParams.get("accountAddress")),
                    JavaReflectUtil.obj2str(searchParams.get("txAmount")),JavaReflectUtil.obj2str(searchParams.get("txFee")),
                    JavaReflectUtil.obj2str(searchParams.get("comment")),JavaReflectUtil.obj2str(searchParams.get("commentTo")));

        }catch(Exception e){

            e.printStackTrace();
            results = new Results(
                    CommConstants.API_RETURN_STATUS.SERVER_INTERNAL_ERROR.value(),
                    CommConstants.API_RETURN_STATUS.SERVER_INTERNAL_ERROR.desc());
        }

        return results;
    }

    @RequestMapping(value = "getBalance")
    @ResponseBody
    public Results getBalance(HttpServletRequest request)
    {
        Results results=null;
        //业务逻辑
        try{
            // 读取请求内容
            String parmaJson = HttpClientRequest.parseRequestBody(request);

            logger.info("请求参数：" + parmaJson);

            Map<String, Object> searchParams = null;
            if(!StringUtil.isNullStr(parmaJson))
                searchParams = (Map) JacksonUtil.getJacksonObj(parmaJson, HashMap.class);

            results = bitcoinCashService.getBalance(JavaReflectUtil.obj2str(searchParams.get("accountName")));

        }catch(Exception e){

            e.printStackTrace();
            results = new Results(
                    CommConstants.API_RETURN_STATUS.SERVER_INTERNAL_ERROR.value(),
                    CommConstants.API_RETURN_STATUS.SERVER_INTERNAL_ERROR.desc());
        }

        return results;
    }
}
