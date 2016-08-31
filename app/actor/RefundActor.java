package actor;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Throwables;
import common.WeiXinTradeType;
import controllers.JDPay;
import controllers.WeiXinCtrl;
import domain.Order;
import domain.Refund;
import play.Logger;
import play.libs.ws.WSClient;
import service.CartService;
import util.ComUtil;
import util.SysParCom;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 退款Actor
 * Created by howen on 16/2/24.
 */
public class RefundActor extends AbstractActor {

    @Inject
    private ComUtil comUtil;
    @Inject
    public RefundActor(CartService cartService, WSClient ws, WeiXinCtrl weiXinCtrl) {

        receive(ReceiveBuilder.match(Object.class, refund -> {

            if (refund instanceof Refund) {

                //必传,orderId,reason,refundType,userId,skuId
                Order order = new Order();
                order.setOrderId(((Refund) refund).getOrderId());
                List<Order> orderList = cartService.getOrder(order);
                if (orderList.size() > 0) {
                    order = orderList.get(0);

                    ((Refund) refund).setAmount(order.getOrderAmount());
                    ((Refund) refund).setPayBackFee(order.getPayTotal());
                    ((Refund) refund).setSplitOrderId(order.getOrderSplitId());
                    ((Refund) refund).setUserId(order.getUserId());
                    List<Refund> refunds =cartService.selectRefund(((Refund) refund));
                    if (refunds.size()>0){
                        if (cartService.updateRefund(((Refund) refund))) {
                            if (order.getPayMethod().equals("JD")) {
                                comUtil.jdPayRefund(cartService, ws, (Refund) refund);
                            } else if (order.getPayMethod().equals("WEIXIN")) {
                                comUtil.weixinPayRefund(cartService, (Refund) refund, weiXinCtrl);
                            }
                        }
                    }else{
                        if (cartService.insertRefund(((Refund) refund))) {
                            if (order.getPayMethod().equals("JD")) {
                                comUtil.jdPayRefund(cartService, ws, (Refund) refund);
                            } else if (order.getPayMethod().equals("WEIXIN")) {
                                comUtil.weixinPayRefund(cartService, (Refund) refund, weiXinCtrl);
                            }
                        }
                    }
                }
            }
        }).matchAny(s -> Logger.error("RefundActor received messages not matched: {}", s.toString())).build());
    }


}