package modules;

import com.google.inject.AbstractModule;
import middle.JDPayMid;
import util.*;

/**
 * 启动leveldb
 * Created by howen on 16/2/19.
 */
public class LevelDBModule extends AbstractModule {

    protected void configure() {
        bind(JDPayMid.class);
        bind(LevelFactory.class).asEagerSingleton();
        bind(NewScheduler.class);
        bind(SysParCom.class).asEagerSingleton();
        bind(MnsInit.class).asEagerSingleton();
        bind(LogUtil.class).asEagerSingleton();
        bind(RedisPool.class).asEagerSingleton();
        bind(LeveldbLoad.class).asEagerSingleton();
    }
}
