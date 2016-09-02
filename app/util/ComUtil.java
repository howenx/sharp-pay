package util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;
import common.WeiXinTradeType;
import controllers.JDPay;
import controllers.WeiXinCtrl;
import domain.Order;
import domain.Refund;
import domain.Sku;
import domain.SkuVo;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;
import service.CartService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by sibyl.sun on 16/4/6.
 */
public class ComUtil {

    //是否超出限购次数
    public boolean isOutOfRestrictAmount(Integer curAmount, SkuVo sku){
//        //直邮不限个数
//        if("K".equals(sku.getInvArea())){
//            return false;
//        }
        if(sku.getRestrictAmount() != 0 && sku.getSkuTypeRestrictAmount() < curAmount){
            return true;
        }
        return false;

    }

    //是否超出限购次数
    public boolean isOutOfRestrictAmount(Integer curAmount, Sku sku){
//        //直邮不限个数
//        if("K".equals(sku.getInvArea())){
//            return false;
//        }
        if(sku.getRestrictAmount() != 0 && sku.getRestrictAmount() < curAmount){
            return true;
        }
        return false;

    }

    //是否超出总额限制
    public boolean isOutOfPostalLimit(String invArea,BigDecimal curValue){
        //直邮不限总额
        if("K".equals(invArea)){
            return false;
        }
        if(curValue.compareTo(new BigDecimal(SysParCom.POSTAL_LIMIT)) > 0){
            return true;
        }
        return false;

    }

    /**
     * 转换图片,拼接URL前缀
     *
     * @param invImg invImg
     * @return invImg
     */
    public String getInvImg(String invImg) {
        //SKU图片
        if (invImg.contains("url")) {
            JsonNode jsonNode_InvImg = Json.parse(invImg);
            if (jsonNode_InvImg.has("url")) {
                ((ObjectNode) jsonNode_InvImg).put("url", SysParCom.IMAGE_URL + jsonNode_InvImg.get("url").asText());
                return Json.stringify(jsonNode_InvImg);
            } else return SysParCom.IMAGE_URL + invImg;
        } else return SysParCom.IMAGE_URL + invImg;
    }

    /***
     * JD退款
     * @param cartService
     * @param ws
     * @param refund
     */
    public void jdPayRefund(CartService cartService, WSClient ws, Refund refund) {
        Map<String, String> params = JDPay.payBackParams(refund, null, null);
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
        ws.url(SysParCom.JD_REFUND_URL).setContentType("application/x-www-form-urlencoded").post(sb.toString()).map(wsResponse -> {
            JsonNode response = wsResponse.asJson();
            Logger.info("京东退款返回数据JSON: " + response.toString());
            Refund re = new Refund();
            re.setId(response.get("out_trade_no").asLong());
            re.setPgCode(response.get("response_code").asText());
            re.setPgMessage(response.get("response_message").asText());
            re.setPgTradeNo(response.get("trade_no").asText());
            re.setState(response.get("is_success").asText());

            if (cartService.updateRefund(re)) {
                if (re.getState().equals("Y")) {
                    Order order1 = new Order();
                    order1.setOrderId(refund.getOrderId());
                    order1.setOrderStatus("T");
                    cartService.updateOrder(order1);
                    Logger.info(refund.getUserId() + "退款成功");
                } else {
                    Logger.error(refund.getUserId() + "退款失败");
                }
            }
            return wsResponse.asJson();
        });
    }

    /**
     * 微信退款
     * @param cartService
     * @param refund
     * @param weiXinCtrl
     */
    public void weixinPayRefund(CartService cartService, Refund refund, WeiXinCtrl weiXinCtrl) {
        Logger.info("调用微信退款refund="+refund);

        Order order = new Order();
        order.setOrderId(refund.getOrderId());
        Optional<List<Order>> listOptional = null;
        try {
            listOptional = Optional.ofNullable(cartService.getOrder(order));
        } catch (Exception e) {
            Logger.error("微信退款查询订单异常" + Throwables.getStackTraceAsString(e));
        }
        if (listOptional.isPresent() && listOptional.get().size() == 1) {
            order = listOptional.get().get(0);
            String xmlContent = weiXinCtrl.getRefundParams(order);
            if (xmlContent != null) {
                String result = weiXinCtrl.refundConnect(SysParCom.WEIXIN_PAY_REFUND, xmlContent, WeiXinTradeType.getWeiXinTradeType(order.getPayMethodSub())); //接口提供所有微信支付订单的查询
                Logger.info("微信支付退款发送内容\n" + xmlContent + "\n返回内容" + result);

                if (Objects.equals("", result) || null == result) {
                    Logger.error(refund.getUserId() + "微信退款返回结果为空");
                }
                try {
                    Map<String, String> resultMap = weiXinCtrl.xmlToMap(result);

                    Refund re = new Refund();
                    //退款失败的情况下获取不到这些值,所以先赋值
                    re.setId(refund.getId());
                    re.setOrderId(refund.getOrderId());

                    if (null == resultMap || resultMap.size() <= 0) {
                        Logger.error(refund.getUserId() + "微信退款返回结果为空");
                    } else if (!"SUCCESS".equals(resultMap.get("return_code"))) { //返回状态码  SUCCESS/FAIL 此字段是通信标识，非交易标识，交易是否成功需要查看result_code来判断
                        Logger.error(refund.getUserId() + "微信退款失败,返回状态码:" + resultMap.get("return_code"));
                    } else {
                        re.setPgCode(resultMap.get("result_code"));
                        re.setPgMessage(resultMap.get("return_msg"));
                        re.setPgTradeNo(resultMap.get("refund_id"));//微信退款单号
                        if (resultMap.get("result_code").equals("SUCCESS")) {
                            String out_refund_no=resultMap.get("out_refund_no");
                            re.setOrderId(Long.valueOf(out_refund_no));
                            re.setState("Y");
                            Order order1 = new Order();
                            order1.setOrderId(refund.getOrderId());
                            order1.setOrderStatus("T");
                            cartService.updateOrder(order1);
                            Logger.error(refund.getUserId() + "微信退款成功,返回业务结果码:" + resultMap.get("result_code")+",refund="+re);
                        } else {
                            Logger.error(refund.getUserId() + "微信退款失败,返回业务结果码:" + resultMap.get("result_code")+",refund="+re);
                            re.setState("N");
                        }
                        cartService.updateRefund(re);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Logger.error("微信退款出现异常," + Throwables.getStackTraceAsString(ex));
                }
            }

        }else{
            Logger.error("微信退款订单不存在"+refund);
        }

    }

}
