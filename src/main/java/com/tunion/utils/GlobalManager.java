package com.tunion.utils;

import com.tunion.chain.litecoin.BitCoinsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Created by Think on 2017/10/23.
 */
@Component
public class GlobalManager implements ApplicationListener<ContextRefreshedEvent> {

    private static Logger logger = LoggerFactory.getLogger(GlobalManager.class);

    private static boolean initFlag=false;								//是否已经初始化

    @Autowired
    private BitCoinsReceivedListener bitCoinsReceivedListener;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        logger.info("onApplicationEvent");
        //需要执行的逻辑代码，当spring容器初始化完成后就会执行该方法。
        //防止重复初始化操作
        if(!initFlag)
        {
            initGlobalCache();
            initFlag=true;
        }
    }

    /*
     * 初始化全局缓存
     */
    public void initGlobalCache()
    {
        bitCoinsReceivedListener.initWallet();
    }
}

