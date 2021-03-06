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
package com.github.kaklakariada.aws.lambda.inject;

import java.util.Map;
import java.util.function.Supplier;

import com.amazonaws.services.lambda.runtime.Context;
import com.github.kaklakariada.aws.lambda.listener.RequestProcessingListener;
import com.github.kaklakariada.aws.lambda.model.request.ApiGatewayRequest;
import com.github.kaklakariada.aws.lambda.model.response.ApiGatewayResponse;
import com.github.kaklakariada.aws.lambda.service.ServiceFactory;
import com.github.kaklakariada.aws.lambda.service.ServiceParams;

public class CurrentRequestParamsSupplier<P extends ServiceParams> implements Supplier<P>, RequestProcessingListener {

	private final ServiceFactory<P> serviceFactory;
	private Map<String, String> env;
	private P serviceParams;

	public CurrentRequestParamsSupplier(ServiceFactory<P> serviceFactory) {
		this(serviceFactory, System.getenv());
	}

	CurrentRequestParamsSupplier(ServiceFactory<P> serviceFactory, Map<String, String> env) {
		this.serviceFactory = serviceFactory;
		this.env = env;
	}

	@Override
	public P get() {
		if (serviceParams == null) {
			throw new IllegalStateException("No service params");
		}
		return serviceParams;
	}

	@Override
	public void beforeRequest(ApiGatewayRequest request, Context context) {
		serviceParams = serviceFactory.extractServiceParams(request, env);
	}

	@Override
	public void afterRequest(ApiGatewayRequest request, ApiGatewayResponse response, Context context) {
		serviceParams = null;
	}
}
