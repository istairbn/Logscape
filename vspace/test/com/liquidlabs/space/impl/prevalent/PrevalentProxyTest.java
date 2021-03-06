package com.liquidlabs.space.impl.prevalent;

import com.liquidlabs.common.concurrent.NamingThreadFactory;
import com.liquidlabs.space.map.Map;
import com.liquidlabs.space.map.MapImpl;
import com.liquidlabs.space.map.Put;
import org.junit.Test;
import org.prevayler.Prevayler;
import org.prevayler.PrevaylerFactory;
import org.prevayler.Transaction;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

public class PrevalentProxyTest {

    @Test
    public void shouldNotBeDodgy() {

    }
//    @Test DodgyTest
    public void should() throws ClassNotFoundException, IOException {
        PrevaylerFactory factory = new PrevaylerFactory();
        factory.configurePrevalenceDirectory("build/prevalent");
        factory.configureSnapshotSerializer(new MyXStreamSerializer());
        factory.configureJournalSerializer(new MyXStreamSerializer());
        final Map prevalent = new MapImpl();
        factory.configurePrevalentSystem(prevalent);
        final Prevayler spy = spy(factory.create());
        ExecutorService runner = Executors.newSingleThreadExecutor(new NamingThreadFactory("prev-space"));

        final PrevaylerProxy proxy = new PrevaylerProxy(spy, factory, runner);

        proxy.execute(new Put("abc", "def", false));

        doThrow(new RuntimeException("FUCK!")).when(spy).execute(any(Transaction.class));

        proxy.execute(new Put("key", "value", false));

        proxy.execute(new Put("key2", "value2", false));


        final Map map = (Map) proxy.prevalentSystem();

        assertThat(map.get("abc"), is("def"));
        assertThat(map.get("key"), is("value"));
        assertThat(map.get("key2"), is("value2"));

    }


}
