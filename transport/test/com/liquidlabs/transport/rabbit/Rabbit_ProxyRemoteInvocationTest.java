package com.liquidlabs.transport.rabbit;

import com.liquidlabs.common.net.URI;
import com.liquidlabs.transport.TransportFactory;
import com.liquidlabs.transport.TransportFactoryImpl;
import com.liquidlabs.transport.proxy.DummyService;
import com.liquidlabs.transport.proxy.DummyServiceImpl;
import com.liquidlabs.transport.proxy.DummyServiceImpl.UserType;
import com.liquidlabs.transport.proxy.ProxyFactoryImpl;
import com.liquidlabs.transport.proxy.Remotable;
import com.liquidlabs.transport.serialization.Convertor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class Rabbit_ProxyRemoteInvocationTest {
	private ProxyFactoryImpl proxyFactoryA;
	boolean enableOutput = false;
	private ProxyFactoryImpl proxyFactoryB;
	private URI proxyBAddress;
	private DummyService remoteService;
	TransportFactory transportFactory ;
	ExecutorService executor = Executors.newFixedThreadPool(5);
	Convertor c = new Convertor();


	@Before
	public void setUp() throws Exception {

		System.setProperty("transport", TransportFactory.TRANSPORT.RABBIT.name());

		com.liquidlabs.common.concurrent.ExecutorService.setTestMode();

		transportFactory = new TransportFactoryImpl(Executors.newFixedThreadPool(5), "test");
		proxyFactoryA = new ProxyFactoryImpl(transportFactory,  TransportFactoryImpl.getDefaultProtocolURI("", "localhost", 11111, "testServiceA"), executor, "");
		proxyFactoryA.start();

		Thread.sleep(100);

		TransportFactoryImpl transportFactory2 = new TransportFactoryImpl(Executors.newFixedThreadPool(5), "test");
		proxyFactoryB = new ProxyFactoryImpl(transportFactory2,  TransportFactoryImpl.getDefaultProtocolURI("", "localhost", 22222, "testServiceB"), executor, "");
		proxyFactoryB.registerMethodReceiver("methodReceiver", new DummyServiceImpl());
		proxyFactoryB.start();

		Thread.sleep(100);
		proxyBAddress = proxyFactoryB.getAddress();
		DummyServiceImpl.callCount = 0;
		remoteService = proxyFactoryA.getRemoteService("methodReceiver", DummyService.class, new String[] { proxyBAddress.toString() } );

		DummyServiceImpl.callCount = 0;
	}
	@After
	public void tearDown() throws Exception {
		transportFactory.stop();
		proxyFactoryA.stop();
		proxyFactoryB.stop();
		Thread.sleep(50);
	}

	@Test
	public void testNumberParamShouldWork() throws Exception {

        for (int i = 0; i < 10000; i++) {
            Number passANumber = remoteService.passANumber(100 + i);
            assertNotNull(passANumber);
            assertEquals(100 + i, passANumber.intValue());
        }
	}

	@Test
	public void testShouldReturnRawByteArrayWithException() throws Exception {
		try {
			byte[] bytes = remoteService.getBytesWithException();
			fail("should have gone failed!");
		} catch (Throwable t){
			assertThat(t.getMessage(), is(notNullValue()));
		}
	}
	@Test
	public void testShouldReturnRawByteArray() throws Exception {
		byte[] bytes = remoteService.getBytes();
		assertEquals("GotBytes", new String(bytes));
	}

	@Test
	public void testShouldPassAMap() throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("stuff1", "stuff2");

		Map mapString = remoteService.passAMap(map);
		assertTrue(mapString.toString().contains("stuff1"));
		assertTrue(mapString.toString().contains("stuff2"));
	}

	@Test
	public void testShouldStopAProxy() throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("stuff1", "stuff2");

		remoteService.passAMap(map);


		assertTrue("Didnt find ProxyClient to stop!", proxyFactoryA.stopProxy(remoteService));
	}

	@Test
	public void testShouldStopAProxyUsingGetId() throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("stuff1", "stuff2");

		remoteService.passAMap(map);


		TestRemote testRemote = new TestRemote(remoteService.getId());

		assertTrue("Didnt find ProxyClient to stop!", proxyFactoryA.stopProxy(testRemote));
	}
	public static class TestRemote implements Remotable {
		private final String id;
		public TestRemote(String id) {
			this.id = id;
		}
		public String getId() {
			return id;
		}

	}


	@Test
	public void testShouldReturnZeroArray() throws Exception {
		int size = 0;
		List<String> results = remoteService.doListOfStrings(size);
		assertNotNull(results);
		assertEquals("Got Wrong size:" + results.size(), size, results.size());
	}
	@Test
	public void testShouldReturnArraysListOfString() throws Exception {
		int size = 100;
		List<String> results = remoteService.doListOfStrings(size);
		assertNotNull(results);
		assertEquals("Got Wrong size:" + results.size(), size, results.size());
	}
	@Test
	public void testEmptyCustomListObject() throws Exception {
        DummyServiceImpl.latch = new CountDownLatch(1);
		remoteService.doCustomUserTypeList(new ArrayList<UserType>(), 0);
		assertThat(DummyServiceImpl.latch.await(3, TimeUnit.SECONDS), is(true));

	}

	@Test
	public void testSingleURIStringArraysResult() throws Exception {
		String[] doEmptyStringArray = remoteService.twoWayWithURIAddressArray("blah");
		assertNotNull(doEmptyStringArray);
		assertEquals(1, doEmptyStringArray.length);

	}
	@Test
	public void testEmptyStringArrayisOkay() throws Exception {
		String[] doEmptyStringArray = remoteService.doEmptyStringArray();
		assertNotNull(doEmptyStringArray);
		assertTrue(doEmptyStringArray.length == 0);
	}
	@Test
	public void testCustomUserTypeWorksWithNulls() throws Exception {
		UserType userType = new UserType();
		userType.someInt = 99;
		userType.someValue = null;
		UserType userTypeResult = remoteService.doCustomUserType(userType);
		assertNotNull(userTypeResult);
		assertNull(userTypeResult.someValue);
		assertTrue(DummyServiceImpl.callCount == 1);				
	}
	@Test
	public void testNoArgs() throws Exception {
        DummyServiceImpl.latch = new CountDownLatch(1);
		remoteService.noArgs();
        assertThat(DummyServiceImpl.latch.await(3, TimeUnit.SECONDS), is(true));

	}
	@Test
	public void testTwoWayWithNullArgs() throws Exception {
		String result = remoteService.twoWay(null);
		assertNotNull(result);
		assertTrue(DummyServiceImpl.callCount == 1);		
	}
	@Test
	public void testTwoWay() throws Exception {
		String result = remoteService.twoWay("doStuff");
		assertNotNull(result);
		assertTrue(DummyServiceImpl.callCount == 1);		
	}
	

}
