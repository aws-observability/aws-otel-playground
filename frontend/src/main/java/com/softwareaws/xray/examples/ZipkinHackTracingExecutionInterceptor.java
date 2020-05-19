/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.softwareaws.xray.examples;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Because of https://github.com/aws/aws-sdk-java-v2/issues/1850, it is not possible to use overrideConfiguration to set the
 * Zipkin interceptor, which needs to be initialized without a no-args constructor and can't be used with service loader. This
 * class can be used by the service loader and hackily and lazily sets up the interceptor.
 */
public class ZipkinHackTracingExecutionInterceptor implements ExecutionInterceptor {

    static void setInterceptor(ExecutionInterceptor delegate) {
        ZipkinHackTracingExecutionInterceptor.delegate = delegate;
    }

    private static ExecutionInterceptor delegate;

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        delegate.beforeExecution(context, executionAttributes);
    }

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        return delegate.modifyRequest(context, executionAttributes);
    }

    @Override
    public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
        delegate.beforeMarshalling(context, executionAttributes);
    }

    @Override
    public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {
        delegate.afterMarshalling(context, executionAttributes);
    }

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        return delegate.modifyHttpRequest(context, executionAttributes);
    }

    @Override
    public Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        return delegate.modifyHttpContent(context, executionAttributes);
    }

    @Override
    public Optional<AsyncRequestBody> modifyAsyncHttpContent(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        return delegate.modifyAsyncHttpContent(context, executionAttributes);
    }

    @Override
    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        delegate.beforeTransmission(context, executionAttributes);
    }

    @Override
    public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
        delegate.afterTransmission(context, executionAttributes);
    }

    @Override
    public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context, ExecutionAttributes executionAttributes) {
        return delegate.modifyHttpResponse(context, executionAttributes);
    }

    @Override
    public Optional<Publisher<ByteBuffer>> modifyAsyncHttpResponseContent(Context.ModifyHttpResponse context, ExecutionAttributes executionAttributes) {
        return delegate.modifyAsyncHttpResponseContent(context, executionAttributes);
    }

    @Override
    public Optional<InputStream> modifyHttpResponseContent(Context.ModifyHttpResponse context, ExecutionAttributes executionAttributes) {
        return delegate.modifyHttpResponseContent(context, executionAttributes);
    }

    @Override
    public void beforeUnmarshalling(Context.BeforeUnmarshalling context, ExecutionAttributes executionAttributes) {
        delegate.beforeUnmarshalling(context, executionAttributes);
    }

    @Override
    public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {
        delegate.afterUnmarshalling(context, executionAttributes);
    }

    @Override
    public SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {
        return delegate.modifyResponse(context, executionAttributes);
    }

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        delegate.afterExecution(context, executionAttributes);
    }

    @Override
    public Throwable modifyException(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        return delegate.modifyException(context, executionAttributes);
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        delegate.onExecutionFailure(context, executionAttributes);
    }
}
