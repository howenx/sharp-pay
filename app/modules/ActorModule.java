package modules;

import actor.*;
import com.google.inject.AbstractModule;
import play.libs.akka.AkkaGuiceSupport;

/**
 * Akka Actor Module
 * Created by howen on 15/12/14.
 */
public class ActorModule extends AbstractModule implements AkkaGuiceSupport {
    @Override
    protected void configure() {
        bindActor(PayRunActor.class, "payRunActor");
        bindActor(ClearCartActor.class,"clearCartActor");
        bindActor(PublicFreeShipActor.class,"publicFreeShipActor");
        bindActor(ReduceInvActor.class,"reduceInvActor");
        bindActor(CancelOrderActor.class,"cancelOrderActor");
        bindActor(PublicCouponActor.class,"publicCouponActor");
        bindActor(QueryCustomStatusActor.class,"queryCustomStatusActor");
        bindActor(PushCustomsActor.class,"pushCustomsActor");
        bindActor(SchedulerCancelOrderActor.class,"schedulerCancelOrderActor");
        bindActor(PinFailActor.class, "pinFailActor");
        bindActor(RefundActor.class,"refundActor");
        bindActor(SchedulerCleanMsgActor.class,"schedulerCleanMsgActor");
        bindActor(DelScheduleActor.class,"delScheduleActor");
        bindActor(ApplyRefundActor.class,"applyRefundActor");
        bindActor(ResumeInvActor.class,"resumeInvActor");
        bindActor(MnsActor.class,"mnsActor");
        bindActor(CouponBackActor.class,"couponBackActor");
    }
}
