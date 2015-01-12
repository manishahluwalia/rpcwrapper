/*
* Copyright 2015 Manish Ahluwalia
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.github.manishahluwalia.gwt.rpcwrapper.client;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.Response;

public class DefaultRpcWrapperListener<T> implements RpcWrapperListener<T> {

    //@Override
    public void onBeforeResponseProcessing(String rpcName, Request request, Response response, T opaqueRequestInfoHolder,
            ProceedCallback<ResponseDisposition> callback) {
        callback.proceed(ResponseDisposition.PROCESS);
    }

    //@Override
    public void onBeforeErrorProcessing(Request request, Throwable error, T opaqueRequestInfoHolder, ProceedCallback<ErrorDisposition> callback) {
        callback.proceed(ErrorDisposition.PROCESS);
    }

    //@Override
    public T getNewOpaqueRequestInfoHolder() {
        return null;
    }

    //@Override
    public void onBeforeRequestSend(RequestBuilder repeatingRequestBuilder, T opaqueRequestInfoHolder) {
    }

    //@Override
    public void onBeforeRequestResend(T opaqueRequestInfoHolder) {
    }

    //@Override
    public void onRequestDataSet(T opaqueRequestInfoHolder, String rpcName) {
    }
}
