package com.tunion.chain.litecoin.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tunion.chain.litecoin.BitCoinsReceivedListener;
import com.tunion.chainrouter.pojo.AddressGroup;
import com.tunion.chainrouter.pojo.Transactions;
import com.tunion.cores.result.Results;
import com.tunion.cores.tools.cache.JedisUtils;
import com.tunion.cores.utils.CommConstants;
import com.tunion.cores.utils.JsonSerializer;
import com.tunion.cores.utils.ShellUtil;
import com.tunion.cores.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Think on 2018/2/24.
 */
@Service
public class LitecoinService {
    private static Logger logger = LoggerFactory.getLogger(LitecoinService.class);

    @Autowired
    private BitCoinsReceivedListener bitCoinsReceivedListener;

    private long str2long(String str) {
        Double fee = StringUtil.str2double(str)* CommConstants.bit;
        return fee.longValue();
    }

    //判断钱包地址是否有效
    private Results validateAddress(String accountAddress)
    {
        Results results = null;

        String cmdStr = String.format("bitcoin-cli validateaddress %s",accountAddress);
        logger.debug(cmdStr);

        try {
            results = ShellUtil.callShell(cmdStr);

            if(CommConstants.API_RETURN_STATUS.NORMAL.value().equals(results.getStatus())) {
                JSONObject jsonObj=JSONObject.parseObject((String)results.getData());
                if(!jsonObj.getBooleanValue("isvalid"))
                {
                    results.setStatus(CommConstants.API_RETURN_STATUS.ACCOUNT_ADDRESS_ERROR.value());
                    results.setError(CommConstants.API_RETURN_STATUS.ACCOUNT_ADDRESS_ERROR.desc());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    //判断账号是否存在
    private boolean checkAccount(String accoutName)
    {
        boolean bRet = true;

        Results results = getAddressesbyAccount( accoutName);

        if(CommConstants.API_RETURN_STATUS.NORMAL.value().equals(results.getStatus())) {
            if("".equals(results.getData()))
            {
                bRet = false;
            }
        }

        return  bRet;
    }

    //解密钱包
    private Results decryptWallet(String password,int second)
    {
        Results results = null;

        String cmdStr = String.format("bitcoin-cli walletpassphrase %s %d",password, second);
        logger.debug(cmdStr);

        try {
            results = ShellUtil.callShell(cmdStr);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    //判断钱包的余额
    private Results checkAccoutBalance(String accoutName,String amount)
    {
        Results results = getBalance(accoutName);
        if(CommConstants.API_RETURN_STATUS.NORMAL.value().equals(results.getStatus())) {
            double balance=StringUtil.str2double((String)results.getData());
            double txbalance=StringUtil.str2double(amount);

            if(txbalance>balance)
            {
                results.setStatus(CommConstants.API_RETURN_STATUS.ACCOUNT_BALANCE_ERROR.value());
                results.setError(CommConstants.API_RETURN_STATUS.ACCOUNT_BALANCE_ERROR.desc());
            }
        }

        return results;
    }

    public Results createAddress(String accoutName) {
        Results results = null;

        logger.info("createAddress accountName:"+accoutName);

        if(StringUtil.isNullStr(accoutName)) {
            return new Results(CommConstants.API_RETURN_STATUS.PARAMETER_ERROR.value(), CommConstants.API_RETURN_STATUS.PARAMETER_ERROR.desc());
        }

        try {

            String address = JedisUtils.getObjectByRawkey(accoutName+BitCoinsReceivedListener.COIN_NAME);
            //检查账号是否存在，如果已经创建过，提示存在，报错
            if(!StringUtil.isNullStr(address))
            {
                logger.error("账号已存在！address:"+address);
                return new Results(CommConstants.API_RETURN_STATUS.ACCOUNT_EXIST_ERROR.value(),CommConstants.API_RETURN_STATUS.ACCOUNT_EXIST_ERROR.desc(),address);
            }

            AddressGroup addressGroup = bitCoinsReceivedListener.getNewaddress();

            //放到缓存中，通知时需要获取
            JedisUtils.setObjectByRawkey(BitCoinsReceivedListener.COIN_NAME+addressGroup.getAddress(),accoutName);

            //存放到缓存中，保证只有一个钱包ID
            JedisUtils.setObjectByRawkey(accoutName+BitCoinsReceivedListener.COIN_NAME,addressGroup.getAddress());

            //key需要放到缓存中
            JedisUtils.setObjectByRawkey(addressGroup.getAddress(),addressGroup.getAddressKey());

            results = new Results(CommConstants.API_RETURN_STATUS.NORMAL.value(),CommConstants.API_RETURN_STATUS.NORMAL.desc(),addressGroup.getAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    public Results getAddressesbyAccount(String accoutName) {
        Results results = null;

        if(StringUtil.isNullStr(accoutName)) {
            return new Results(CommConstants.API_RETURN_STATUS.PARAMETER_ERROR.value(), CommConstants.API_RETURN_STATUS.PARAMETER_ERROR.desc());
        }

        String cmdStr = "bitcoin-cli getaddressesbyaccount "+accoutName;
        logger.debug(cmdStr);

        try {
            results = ShellUtil.callShell(cmdStr);

            if(CommConstants.API_RETURN_STATUS.NORMAL.value().equals(results.getStatus())) {
                List<String> strList = JsonSerializer.readListBean((String) results.getData(), List.class, String.class);

                if(!strList.isEmpty()) {
                    results.setData(strList.get(0));
                }else{
                    results.setData("");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    public Results listAddress() {
        Results results = null;

        String cmdStr = "bitcoin-cli listaddressgroupings";
        logger.debug(cmdStr);

        try {
            results = ShellUtil.callShell(cmdStr);

            if(CommConstants.API_RETURN_STATUS.NORMAL.value().equals(results.getStatus())) {

                JSONArray jsonArray = JSONObject.parseArray((String)results.getData());

                System.out.println(jsonArray.size());

                List<AddressGroup> lstAddress = new ArrayList<AddressGroup>();
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONArray arrayItem = jsonArray.getJSONArray(i);

                    for (int j = 0; j < arrayItem.size(); j++) {
                        JSONArray jsonObject = arrayItem.getJSONArray(j);

                        AddressGroup addressGroup = new AddressGroup();
                        addressGroup.setAddress(jsonObject.getString(0));
                        if (jsonObject.size() > 1) {
                            DecimalFormat df = new DecimalFormat("0.00000000");
                            addressGroup.setBalance(df.format(jsonObject.getDoubleValue(1)));
                        }
                        if (jsonObject.size() > 2) {
                            addressGroup.setAccout(jsonObject.getString(2));
                        }

                        lstAddress.add(addressGroup);

                    }
                }//end for

                results.setData(lstAddress);

            }//end if
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    public Results getBalance(String accoutName) {
        Results results = null;

        if(StringUtil.isNullStr(accoutName)) {
            return new Results(CommConstants.API_RETURN_STATUS.PARAMETER_ERROR.value(), CommConstants.API_RETURN_STATUS.PARAMETER_ERROR.desc());
        }

        try {
            results =new Results(CommConstants.API_RETURN_STATUS.NORMAL.value(),CommConstants.API_RETURN_STATUS.NORMAL.desc(),bitCoinsReceivedListener.getBalance());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    public Results withdrawalCash(String accoutName, String accountAddress, String txAmount, String txFee, String comment, String commentTo) {
        Results results = new Results(CommConstants.API_RETURN_STATUS.NORMAL.value(),CommConstants.API_RETURN_STATUS.NORMAL.desc());

        if(StringUtil.isNullStr(accoutName)||StringUtil.isNullStr(accountAddress)||StringUtil.isNullStr(txAmount)) {
            return new Results(CommConstants.API_RETURN_STATUS.PARAMETER_ERROR.value(), CommConstants.API_RETURN_STATUS.PARAMETER_ERROR.desc());
        }

        if(accountAddress.length()<34)
        {
            return new Results(CommConstants.API_RETURN_STATUS.ACCOUNT_ADDRESS_ERROR.value(), CommConstants.API_RETURN_STATUS.ACCOUNT_ADDRESS_ERROR.desc());
        }

        try {
            boolean bRet = false;
            //转换交易手续费的数据类型
            long fee = str2long(txFee);
            if (fee>0) {
                bRet = bitCoinsReceivedListener.sendCoins(accountAddress, txAmount, txFee);
            }else {
                bRet =  bitCoinsReceivedListener.sendCoins(accountAddress, txAmount);
            }
            if(!bRet)
            {
                results = new Results(CommConstants.API_RETURN_STATUS.ACCOUNT_BALANCE_ERROR.value(), CommConstants.API_RETURN_STATUS.ACCOUNT_BALANCE_ERROR.desc());
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            results = new Results(CommConstants.API_RETURN_STATUS.SERVER_INTERNAL_ERROR.value(),CommConstants.API_RETURN_STATUS.SERVER_INTERNAL_ERROR.desc(),e.getMessage());
        }

        return results;
    }

    public Results internalTransfer(String accoutName, String accountNameTo, String txAmount, String comment) {
        Results results = null;

        if(StringUtil.isNullStr(accoutName)||StringUtil.isNullStr(accountNameTo)||StringUtil.isNullStr(txAmount)) {
            return new Results(CommConstants.API_RETURN_STATUS.PARAMETER_ERROR.value(), CommConstants.API_RETURN_STATUS.PARAMETER_ERROR.desc());
        }

        //获取转出账号的余额，如果余额不足不能完成交易
        results = checkAccoutBalance(accoutName,txAmount);
        if(!CommConstants.API_RETURN_STATUS.NORMAL.value().equals(results.getStatus()))
        {
            return results;
        }

        //查询交易
        String cmdStr = String.format("bitcoin-cli move %s %s %s 0 %s",accoutName,accountNameTo,txAmount,comment);
        logger.debug(cmdStr);

        try {
            results = ShellUtil.callShell(cmdStr);

            if(CommConstants.API_RETURN_STATUS.NORMAL.value().equals(results.getStatus())) {

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    public Results queryTransactions(String accoutName, int transCount) {
        Results results = null;

        if(StringUtil.isNullStr(accoutName)) {
            return new Results(CommConstants.API_RETURN_STATUS.PARAMETER_ERROR.value(), CommConstants.API_RETURN_STATUS.PARAMETER_ERROR.desc());
        }

        //查询交易
        String cmdStr = String.format("bitcoin-cli listtransactions %s %d 0 true",accoutName,transCount);
        logger.debug(cmdStr);

        try {
            results = ShellUtil.callShell(cmdStr);

            if(CommConstants.API_RETURN_STATUS.NORMAL.value().equals(results.getStatus())) {

                String retStr=(String)results.getData() ;
                retStr=retStr.replaceAll("bip125-replaceable", "bip125Replaceable");

                List<Transactions> lstTrans = JsonSerializer.readListBean(retStr, List.class, Transactions.class);

                results.setData(lstTrans);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }
}
