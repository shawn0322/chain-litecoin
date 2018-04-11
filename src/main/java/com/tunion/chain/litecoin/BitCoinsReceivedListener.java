package com.tunion.chain.litecoin;


import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.tunion.chainrouter.pojo.AddressGroup;
import com.tunion.cores.result.Results;
import com.tunion.cores.tools.cache.JedisUtils;
import com.tunion.cores.utils.CommConstants;
import com.tunion.cores.utils.DateUtil;
import com.tunion.cores.utils.StringUtil;
import com.tunion.dubbo.chainrouter.CoinReceivedNotifyService;
import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.OnTransactionBroadcastListener;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.utils.ContextPropagatingThreadFactory;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Created by Think on 2018/1/31.
 */

@Component("bitCoinsReceivedListener")
public class BitCoinsReceivedListener implements WalletCoinsReceivedEventListener,Runnable{

    private static Logger logger = LoggerFactory.getLogger(BitCoinsReceivedListener.class);

    @Autowired
    private CoinReceivedNotifyService coinReceivedNotifyService;

    public static String COIN_NAME = CommConstants.MANUFACTOR_TYPE.LTC.name();
    public static int COIN_VALUE = CommConstants.MANUFACTOR_TYPE.LTC.value();

    /*
   *初始化根钱包
    */
    public void initWallet()
    {
        try {
            String bitcoinAddr = JedisUtils.getObjectByRawkey(CommConstants.CHAINROUTER_+COIN_NAME);
            bitcoinAddr="cTUtVh1QYG2afSbzo4d1QUm1mdQcA8E3Qw1yhmoY8j6vdaaPbnuD"; //address mhwj3hmBNGMpQCtBZobLDKwrwM2tBiE6WZ
            //初始化文件地址，及钱包主地址
            init("bitcoin-blocks", bitcoinAddr);

            ContextPropagatingThreadFactory ctxPTF = new ContextPropagatingThreadFactory("bitCoin Server");
            Thread thread = ctxPTF.newThread(this);
            thread.start();

            importWallets();

        } catch (BlockStoreException e) {
            e.printStackTrace();
        }
    }

    /*
    从缓存中获取需要导入的钱包地址
     */
    private void importWallets()
    {
        try
        {
            Set<String> addressSet= JedisUtils.getSetByRawkey(COIN_NAME);

            for (String address : addressSet) {
                address=address.substring(COIN_NAME.length());
                logger.info("bitcoin wallet:"+address);

                String key = JedisUtils.getObjectByRawkey(address);
                if(!StringUtil.isNullStr(key)) {
                    this.addKeyWallet(key);
                }else{
                    logger.error("address:{} key is null!",address);
                }
            }
        }catch (Exception e)
        {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            logger.info("running......");

            //启动监听
            startPeerGroup();

            while (true) {
                Thread.sleep(20);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ECKey loadPrivate(String privateKey)
    {
        ECKey key = null;
        try {
            DumpedPrivateKey dpk = DumpedPrivateKey.fromBase58(params, privateKey);
            key = dpk.getKey();

            Address addressFromKey = LegacyAddress.fromKey(params,key);
            logger.info("Public Address:" + addressFromKey);
        }catch (Exception e)
        {
            logger.error(e.getMessage());
        }

        return key;
    }

    private Wallet loadBitcoinWallet(String privateKey)
    {
        Wallet wallet = null;
        try
        {
            ECKey key = loadPrivate(privateKey);

            wallet = new Wallet(params);
            wallet.importKey(key);
        }catch (Exception e)
        {
            logger.error(e.getMessage());
        }

        return  wallet;
    }

    /*
    增加钱包地址
     */
    public boolean addKeyWallet(String privateKey)
    {
        boolean bRet = false;
        try
        {
            ECKey key = loadPrivate(privateKey);

            bRet = wallet.importKey(key);
        }catch (Exception e)
        {
            logger.error(e.getMessage());
        }


        return bRet;
    }

    private void startPeerGroup()
    {
        logger.info("Start peer group!");
        peerGroup.start();
        peerGroup.downloadBlockChain();
    }

    private void stopPeerGroup()
    {
        logger.info("Stop peer group!");
        peerGroup.stop();
    }

    /*
    NetworkParameters params;  // 网络参数声明
    params = TestNet3Params.get(); // 公共测试网络
    params = RegTestParams.get(); // 私有测试网络
    params = MainNetParams.get(); // 生产网络
     */

    private void init(String filePath,String privateKey) throws BlockStoreException {
        logger.info("init......");
        params  = TestNet3Params.get();

        blockFile = new File(filePath);
        blockStore = new SPVBlockStore(params, blockFile);

        mainAddress = LegacyAddress.fromKey(params,loadPrivate(privateKey));

        wallet = loadBitcoinWallet(privateKey);

        BlockChain blockChain = new BlockChain(params, wallet, blockStore);
        peerGroup = new PeerGroup(params, blockChain);
        peerGroup.addPeerDiscovery(new DnsDiscovery(params));

        peerGroup.addWallet(wallet);
        wallet.addCoinsReceivedEventListener(this);

        peerGroup.addOnTransactionBroadcastListener(new OnTransactionBroadcastListener() {

            @Override
            public void onTransaction(Peer peer, final Transaction tx) {
                String toAddress="";
                List<String> lstAddress=new ArrayList<String>();
                try {

                    for (TransactionOutput output : tx.getOutputs()) {
                        String outAddress = output.getScriptPubKey().getToAddress(params).toString();
                        if (output.isMine(wallet)) {
                            toAddress = outAddress;
                        }else{
                            lstAddress.add(outAddress);
                            Coin value = output.getValue();
                            logger.info("Send to address:{},tx:{}, txAmout:{}", outAddress, tx.getHashAsString(), value.toFriendlyString());
                        }
                    }

                    if (!StringUtil.isNullStr(toAddress))
                    {
                        logger.info("Received Address:{}", toAddress);
                    }else{
                        //发送业务处理
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        });
    }

    @Override
    public void onCoinsReceived(final Wallet wallet, final Transaction transaction, Coin prevBalance, Coin newBalance) {
        Context.propagate(wallet.getContext());
        final Coin value = transaction.getValueSentToMe(wallet);
        String toAddress = transaction.getOutput(0).getScriptPubKey().getToAddress(params).toString();

        //在交易信息中，接收方的地址不一定就是第一个，有放在第二个的情况
        if(StringUtil.isNullStr(JedisUtils.getObjectByRawkey(toAddress)))
        {
            if(transaction.getOutputs().size()>1)
            {
                toAddress = transaction.getOutput(1).getScriptPubKey().getToAddress(params).toString();
            }
        }

        logger.info("Received address:{},tx:{}, txAmout:{}",toAddress,transaction.getHashAsString(),value.toFriendlyString());
        logger.info("prevBalance:{},newBalance:{},walletBalance:{}",prevBalance.toFriendlyString(),newBalance.toFriendlyString(),wallet.getBalance().toFriendlyString());

        logger.info("交易时间:"+ DateUtil.dateToTimeString(transaction.getUpdateTime()));

        if(transaction.getUpdateTime().getTime()>DateUtil.addDay(new Date(),-1).getTime()) {
            coinReceivedNotifyService.notifyCoinRecevied(toAddress, COIN_VALUE, transaction.getHashAsString(), value.toPlainString());
        }

        Futures.addCallback(transaction.getConfidence().getDepthFuture(1), new FutureCallback<TransactionConfidence>() {
            public void onSuccess(TransactionConfidence result) {
                logger.debug("Transaction confirmed, wallet balance is :" + wallet.getBalance().toFriendlyString());
            }

            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });

    }

    public AddressGroup getNewaddress()
    {
        AddressGroup addressGroup= new AddressGroup();
        try {
            ECKey key = new ECKey();
            Address newAddress = LegacyAddress.fromKey(params, key);
            //key.toAddress(params);

            logger.info("getNewaddress:{}",newAddress.toString());
            addressGroup.setAddress(newAddress.toString());
            addressGroup.setAddressKey(key.getPrivateKeyEncoded(params).toString());

            //添加到钱包地址
            this.addKeyWallet(addressGroup.getAddressKey());
        } catch (Exception e) {
            logger.error("getNewaddress error:"+e.getMessage());
        }

        return addressGroup;
    }

    public String getBalance()
    {
        String balance = wallet.getBalance().toPlainString();
        logger.info("wallet balance is:{}",balance);
        return balance;
    }

    public boolean sendCoins(String address,String amount)
    {
        logger.info("sendCoins to address:{} with {}",address,amount);
        boolean bRet = false;
        try {
            Address toAddress = LegacyAddress.fromBase58(params, address);//Address.fromBase58(params, address);

            Coin amountToSend = Coin.parseCoin(amount);

            SendRequest request = SendRequest.to( toAddress, amountToSend );
            request.changeAddress = mainAddress;

            final Wallet.SendResult sendResult = wallet.sendCoins(request);

            bRet = true;

            //并设置交易完成后的事件响应
            sendResult.broadcastComplete.addListener(new Runnable() {
                @Override
                public void run() {
                    logger.info("Coins Sent! Transaction hash is:" + sendResult.tx.getHashAsString());
                }
            }, MoreExecutors.newDirectExecutorService());
        } catch (InsufficientMoneyException e) {
            logger.error("sendCoins"+e.getMessage());
        }

        return bRet;
    }

    public boolean sendCoins(String address,String amount,String fee)
    {
        logger.info("sendCoins to address:{} with {} and fee {}",address,amount,fee);
        boolean bRet = false;
        try {
            Address toAddress = LegacyAddress.fromBase58(params, address);

            Coin amountToSend = Coin.parseCoin(amount);
            Coin feeToSend  =  Coin.parseCoin(fee);

            //Generate the request
            SendRequest request = SendRequest.to( toAddress, amountToSend );
            request.changeAddress = mainAddress;
            //Update the fee.
            request.feePerKb = feeToSend;

            final Wallet.SendResult sendResult = wallet.sendCoins(request);

            bRet = true;

            //并设置交易完成后的事件响应
            sendResult.broadcastComplete.addListener(new Runnable() {
                @Override
                public void run() {
                    logger.info("Coins Sent! Transaction hash is:" + sendResult.tx.getHashAsString());
                }
            }, MoreExecutors.newDirectExecutorService());
        } catch (InsufficientMoneyException e) {
            logger.error("sendCoins with fee"+e.getMessage());
        }

        return bRet;
    }

    public Results queryTransaction(String txid)
    {
        Results results = new Results(CommConstants.API_RETURN_STATUS.NORMAL.value(),CommConstants.API_RETURN_STATUS.NORMAL.desc());

        try{
            Sha256Hash sha256Hash = Sha256Hash.wrap(txid);
            Transaction transaction = wallet.getTransaction(sha256Hash);

            results.setData(transaction);
        }catch (Exception e){
            logger.error("查询失败!{}",e.getMessage());
        }

        return results;
    }

    private File blockFile;              //文件
    private SPVBlockStore blockStore;   //区块存储
    private NetworkParameters params;   //网络配置参数
    private BlockChain blockChain;      //区块链
    private PeerGroup peerGroup;        //
    private Wallet wallet;              //钱包
    private Address mainAddress;       //主钱包地址

}
