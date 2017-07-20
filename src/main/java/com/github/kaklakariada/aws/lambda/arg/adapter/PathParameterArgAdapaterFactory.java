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
package com.github.kaklakariada.aws.lambda.arg.adapter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

import com.amazonaws.services.lambda.runtime.Context;
import com.github.kaklakariada.aws.lambda.arg.SingleArgValueAdapter;
import com.github.kaklakariada.aws.lambda.controller.PathParameter;
import com.github.kaklakariada.aws.lambda.model.request.ApiGatewayRequest;

public class PathParameterArgAdapaterFactory extends ArgAdapterFactory {

	@Override
	public Class<?> getSupportedArgType() {
		return String.class;
	}

	@Override
	public Class<? extends Annotation> getSupportedArgAnnotation() {
		return PathParameter.class;
	}

	@Override
	public SingleArgValueAdapter createAdapter(Parameter param) {
		final String parameterName = param.getAnnotation(PathParameter.class).value();
		return (ApiGatewayRequest request, Context context) -> request.getPathParameters().get(parameterName);
	}
}
