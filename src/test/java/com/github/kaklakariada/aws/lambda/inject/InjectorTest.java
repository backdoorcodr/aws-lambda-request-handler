package com.github.kaklakariada.aws.lambda.inject;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.github.kaklakariada.aws.lambda.example.MyLambdaController;
import com.github.kaklakariada.aws.lambda.example.MyServiceB;
import com.github.kaklakariada.aws.lambda.example.MyServiceParams;
import com.github.kaklakariada.aws.lambda.service.ServiceCache;

public class InjectorTest {

	private static final String VALUE1 = "val1";
	private static final String VALUE2 = "val2";

	@Mock
	private ServiceCache<MyServiceParams> cacheMock;
	@Mock
	private Supplier<MyServiceParams> serviceParamsSupplierMock;

	@Mock
	private MyServiceB serviceBMock1;
	@Mock
	private MyServiceB serviceBMock2;

	@Mock
	private MyServiceParams param1Mock;
	@Mock
	private MyServiceParams param2Mock;

	private Injector<MyServiceParams> injector;
	private MyLambdaController controller;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		injector = new Injector<>(cacheMock, serviceParamsSupplierMock);
		controller = new MyLambdaController();
		when(serviceBMock1.getValue()).thenReturn(VALUE1);
		when(serviceBMock2.getValue()).thenReturn(VALUE2);
		when(cacheMock.getService(MyServiceB.class, param1Mock)).thenReturn(serviceBMock1);
		when(cacheMock.getService(MyServiceB.class, param2Mock)).thenReturn(serviceBMock2);
	}

	@Test
	public void testServiceInjectedIntoController() {
		injector.injectServices(controller);

		when(serviceParamsSupplierMock.get()).thenReturn(param1Mock);
		assertThat(controller.handleRequest(), equalTo("Service B: " + VALUE1));

		when(serviceParamsSupplierMock.get()).thenReturn(param2Mock);
		assertThat(controller.handleRequest(), equalTo("Service B: " + VALUE2));
	}

	@Test(expected = IllegalStateException.class)
	public void testServiceParamsSupplierReturnsNull() {
		injector.injectServices(controller);

		when(serviceParamsSupplierMock.get()).thenReturn(null);
		controller.handleRequest();
	}
}
