package com.tunion.dubbo.chainrouter;

import com.alibaba.dubbo.config.annotation.Reference;
import com.tunion.cores.result.Results;
import com.tunion.cores.tools.cache.JedisUtils;
import com.tunion.cores.utils.CommConstants;
import com.tunion.cores.utils.StringUtil;
import com.tunion.dubbo.IService.chainweb.IDubboChainWeb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Created by Think on 2018/2/1.
 */
@Service
public class CoinReceivedNotifyService {
    private static Logger logger = LoggerFactory.getLogger(CoinReceivedNotifyService.class);

    @Reference
    private IDubboChainWeb dubboChainWeb;

    public Results notifyCoinRecevied(String accoutAddress, int accoutType, String txid, String amount) {
        Results results = null;
        try {
            //查询一下地址所对应的账号信息
            String accoutName = JedisUtils.getObjectByRawkey(CommConstants.MANUFACTOR_TYPE.index(accoutType).name()+accoutAddress);

            logger.info("accoutAddress:{},accoutName:{},amount:{}",accoutAddress,accoutName,amount);

            if(StringUtil.isNullStr(accoutName))
            {
                return results;
            }

            results = dubboChainWeb.notifyCoinRecevied(accoutName,accoutAddress,accoutType,txid,amount);

            logger.info("dubboChainWeb.notifyCoinRecevied!{}",results.getError());

        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return results;
    }
}
