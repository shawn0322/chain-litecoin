package com.tunion.cores;

import com.tunion.SpringBootStartApplication;
import com.tunion.cores.utils.ISOUtil;
import org.junit.runner.RunWith;
import org.litecoinj.core.ECKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.math.ec.ECPoint;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigInteger;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SpringBootStartApplication.class})
@AutoConfigureMockMvc
public class BaseTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public static void main(String[] args)
    {
        String privKey = "000000002a936ca763904c3c35fce2f3556c559c0214345d31b1bcebf76acb70";
        BigInteger secret = new BigInteger(privKey,16);
        ECPoint point = ECKey.publicPointFromPrivate(secret).normalize();;
        BigInteger x = point.getXCoord().toBigInteger();
        BigInteger y = point.getYCoord().toBigInteger();

        System.out.println("public key:"+ ISOUtil.byte2hex(point.getEncoded()));
    }

}
