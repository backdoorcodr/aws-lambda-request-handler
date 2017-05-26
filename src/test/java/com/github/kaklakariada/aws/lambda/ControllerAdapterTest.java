/**
 * aws-lambda-request-handler - Request handler for AWS Lambda Proxy model
 * Copyright (C) 2017 Christoph Pirkl <christoph at users.sourceforge.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.kaklakariada.aws.lambda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Parameter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.lambda.runtime.Context;
import com.github.kaklakariada.aws.lambda.controller.LambdaController;
import com.github.kaklakariada.aws.lambda.controller.RequestHandlerMethod;
import com.github.kaklakariada.aws.lambda.exception.ConfigurationErrorException;
import com.github.kaklakariada.aws.lambda.exception.InternalServerErrorException;
import com.github.kaklakariada.aws.lambda.request.ApiGatewayRequest;

@RunWith(JUnitPlatform.class)
public class ControllerAdapterTest {

	@Mock
	private ApiGatewayRequest apiGatewayRequestMock;
	@Mock
	private TestRequest requestBodyMock;
	@Mock
	private Context contextMock;
	@Mock
	private TestResponse responseMock;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testNoHandlerMethod() {
		assertHandlerMethodConfigError(new LambdaControllerMissingHandlerMethod());
	}

	@Test
	public void testHandlerMissingHandlerMethodAnnotation() {
		assertHandlerMethodConfigError(new LambdaControllerMissingHandlerMethodAnnotation());
	}

	@Test
	public void testTwoHandlerMethods() {
		assertHandlerMethodConfigError(new LambdaControllerTwoHandlerMethods());
	}

	@Test
	public void testHandlerMethodPackagePrivate() {
		assertHandlerMethodConfigError(new LambdaControllerWithPackagePrivateHandlerMethod());
	}

	@Test
	public void testHandlerMethodPrivate() {
		assertHandlerMethodConfigError(new LambdaControllerWithPrivateHandlerMethod());
	}

	@Test
	public void testHandlerMethodProtected() {
		assertHandlerMethodConfigError(new LambdaControllerWithProtectedHandlerMethod());
	}

	@Test
	public void testVoidHandlerMethod() {
		assertConfigError(new LambdaControllerVoidHandlerMethod(),
				"Return type 'void' of handler method 'public void com.github.kaklakariada.aws.lambda.ControllerAdapterTest$LambdaControllerVoidHandlerMethod.handler1()' is not compatible with response type "
						+ TestResponse.class.getName());
	}

	@Test
	public void testHandlerMethodWithWrongReturnType() {
		assertConfigError(new LambdaControllerStringReturningHandlerMethod(),
				"Return type 'java.lang.String' of handler method 'public java.lang.String com.github.kaklakariada.aws.lambda.ControllerAdapterTest$LambdaControllerStringReturningHandlerMethod.handler1()' is not compatible with response type "
						+ TestResponse.class.getName());
	}

	@Test
	public void testHandlerMethodWithSuperTypeReturnType() {
		assertConfigError(new LambdaControllerSuperTypeReturningHandlerMethod(),
				"Return type 'com.github.kaklakariada.aws.lambda.ControllerAdapterTest$BaseTestResponse' of handler method 'public com.github.kaklakariada.aws.lambda.ControllerAdapterTest$BaseTestResponse com.github.kaklakariada.aws.lambda.ControllerAdapterTest$LambdaControllerSuperTypeReturningHandlerMethod.handler1()' is not compatible with response type "
						+ TestResponse.class.getName());
	}

	@Test
	public void testHandlerMethodWithInvalidArguments() throws NoSuchMethodException, SecurityException {
		final Parameter param = LambdaControllerInvalidArgumentType.class.getMethod("handler1", String.class)
				.getParameters()[0];
		assertConfigError(new LambdaControllerInvalidArgumentType(),
				"Could not find adapter for parameter " + param + " of handler method");
	}

	@Test
	public void testHandlerMethodMatchingReturnReturnType() {
		final LambdaControllerValidReturnType controllerMock = mock(LambdaControllerValidReturnType.class);
		when(controllerMock.handler1()).thenReturn(responseMock);
		assertSame(responseMock, executeHandlerMethod(controllerMock));
		verify(controllerMock).handler1();
	}

	private TestResponse executeHandlerMethod(final LambdaController<TestRequest, TestResponse> controllerMock) {
		final ControllerAdapter<TestRequest, TestResponse> adapter = ControllerAdapter.create(controllerMock,
				TestRequest.class, TestResponse.class);
		return adapter.handleRequest(apiGatewayRequestMock, requestBodyMock, contextMock);
	}

	@Test
	public void testHandlerMethodThrowsException() {
		final LambdaControllerValidReturnType controllerMock = mock(LambdaControllerValidReturnType.class);
		final RuntimeException cause = new RuntimeException("expected");
		when(controllerMock.handler1()).thenThrow(cause);

		final ControllerAdapter<TestRequest, TestResponse> adapter = ControllerAdapter.create(controllerMock,
				TestRequest.class, TestResponse.class);

		final InternalServerErrorException exception = assertThrows(InternalServerErrorException.class,
				() -> adapter.handleRequest(apiGatewayRequestMock, requestBodyMock, contextMock));
		assertSame(cause, exception.getCause().getCause());
		assertEquals("Error invoking handler method", exception.getErrorMessage());
	}

	private void assertHandlerMethodConfigError(final LambdaController<TestRequest, TestResponse> controller) {
		final String expectedErrorMessage = "Class " + controller.getClass().getName()
				+ " must have exactly one public method annotated with " + RequestHandlerMethod.class.getName();
		assertConfigError(controller, expectedErrorMessage);
	}

	private void assertConfigError(final LambdaController<TestRequest, TestResponse> controller,
			String expectedErrorMessage) {
		final ConfigurationErrorException exception = assertThrows(ConfigurationErrorException.class,
				() -> ControllerAdapter.create(controller, TestRequest.class, TestResponse.class));

		assertEquals(expectedErrorMessage, exception.getErrorMessage());
	}

	private static class LambdaControllerMissingHandlerMethod implements LambdaController<TestRequest, TestResponse> {
	}

	private static class LambdaControllerMissingHandlerMethodAnnotation
			implements LambdaController<TestRequest, TestResponse> {
		public void handler() {
		}
	}

	private static class LambdaControllerTwoHandlerMethods implements LambdaController<TestRequest, TestResponse> {
		@RequestHandlerMethod
		public void handler1() {
		}

		@RequestHandlerMethod
		public void handler2() {
		}
	}

	private static class LambdaControllerStringReturningHandlerMethod
			implements LambdaController<TestRequest, TestResponse> {
		@RequestHandlerMethod
		public String handler1() {
			return null;
		}
	}

	private static class LambdaControllerVoidHandlerMethod implements LambdaController<TestRequest, TestResponse> {
		@RequestHandlerMethod
		public void handler1() {
		}
	}

	private static class LambdaControllerSuperTypeReturningHandlerMethod
			implements LambdaController<TestRequest, TestResponse> {
		@RequestHandlerMethod
		public BaseTestResponse handler1() {
			return null;
		}
	}

	private static class LambdaControllerValidReturnType implements LambdaController<TestRequest, TestResponse> {
		@RequestHandlerMethod
		public TestResponse handler1() {
			return null;
		}
	}

	private static class LambdaControllerInvalidArgumentType implements LambdaController<TestRequest, TestResponse> {
		@RequestHandlerMethod
		public TestResponse handler1(String arg) {
			return null;
		}
	}

	private static class LambdaControllerWithPrivateHandlerMethod
			implements LambdaController<TestRequest, TestResponse> {
		@RequestHandlerMethod
		private TestResponse handler1() {
			return null;
		}
	}

	private static class LambdaControllerWithPackagePrivateHandlerMethod
			implements LambdaController<TestRequest, TestResponse> {
		@RequestHandlerMethod
		TestResponse handler1() {
			return null;
		}
	}

	private static class LambdaControllerWithProtectedHandlerMethod
			implements LambdaController<TestRequest, TestResponse> {
		@RequestHandlerMethod
		protected TestResponse handler1() {
			return null;
		}
	}

	private static class BaseTestRequest {
	}

	private static class BaseTestResponse {
	}

	private static class TestRequest extends BaseTestRequest {
	}

	private static class TestResponse extends BaseTestResponse {
	}
}
