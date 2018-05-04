package com.tunion.cores;

import com.tunion.SpringBootStartApplication;
import com.tunion.cores.tools.HttpClientRequest;
import com.tunion.cores.utils.ISOUtil;
import com.tunion.cores.utils.JacksonUtil;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.runner.RunWith;
import org.litecoinj.core.ECKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.math.ec.ECPoint;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SpringBootStartApplication.class})
@AutoConfigureMockMvc
public class BaseTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public static void main(String[] args)
    {
        String url="https://recaptcha.net/recaptcha/api/siteverify";
        try {
            List<NameValuePair> urlParameters = new ArrayList<>();
            urlParameters.add(new BasicNameValuePair("secret", "6Lc7dFQUAAAAAKNCW8kgvYWoImGDm0WeF7cqcm4V"));
            urlParameters.add(new BasicNameValuePair("response", "03AJIzXZ53niCJc55ACfD6OoihTX2tAwwngP6q1o3cCEo8McDcHoAuMfNq1obWPEC8ddIm6F1RAcwleVw3nFpxgUNPy6NVAxurDq__esjY8h5PZPj6qLzXt0X1tv5rOZmhiNXZvXLMNmGtIbqJn59fHMlvDDaosn6dJkgHBSl3o7ac2xZFfiPZ0yVrsKRZfdBXbvfZ6ur6QtoU_gqZ288XVMfGZauIOGeTb_RqDKEEN13Q7fZaQGSwx4L9Lxlis1rKGvijSu4LONExFYutlMFFwY6JcqUHrfJUdCucVtLBiqHprV-GPlNB9PTd9MW-g1W1b5W-3zg0T4Z5kZ-7IKxroi33vwSW6m2xRhRpJcFmpmoa1DZgn4bST76y_qdBSN3zdOLa4EREcs_r3UKCUJk5xzRE0nVM-AX40TQSxuDNGPO2Si_GQCsh_Awt0Pp8UnmRF6vOIg2U38nsZ4Pwv4g0CxBa087KP4DB03zH2bu6yRnEyLOtVXofBlQdmZvjEjSr-HqSIHlGTqHx0yVUUYZd0SxBZm6O-JZ7Rn9pr2RBY2AlKNvvBy4QRrg0SEGcpPy9i8com-_Jdfh-OtOyMcl_szHM2bbissF_SU4u9g5ww8IaiXj_jqbDErc"));

            String retStr=HttpClientRequest.httpPost(url, urlParameters);
            System.out.println(retStr);

            Map map=(Map)JacksonUtil.getJacksonObj(retStr, Map.class);
            System.out.println(map.get("success"));

        }catch (Exception e)
        {
            e.printStackTrace();
        }

        String email="lizf8@tuniondata.com";
        System.out.println(email.substring(0,email.indexOf('@')));

        String privKey = "000000002a936ca763904c3c35fce2f3556c559c0214345d31b1bcebf76acb70";
        BigInteger secret = new BigInteger(privKey,16);
        ECPoint point = ECKey.publicPointFromPrivate(secret).normalize();;
        BigInteger x = point.getXCoord().toBigInteger();
        BigInteger y = point.getYCoord().toBigInteger();

        System.out.println("public key:"+ ISOUtil.byte2hex(point.getEncoded()));
    }

}
