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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;

/**
 * We want most of our GWT-RPC calls to have the following characteristics:
 * <ol>
 * <li>CSRF safe</li>
 * <li>Restart after logging us back in if the server restarted and lost our
 * login session</li>
 * <li>Do all of this transparently to the calling client code and to the called
 * server code</li>
 * </ol>
 * This class helps us do all of the above.
 * @param <T> 
 */
public class WrappedRpcBuilder<T> extends RpcRequestBuilder
{
    private static final Logger logger = Logger.getLogger(WrappedRpcBuilder.class.getName());
    private final RpcWrapper<T> wrapper;

    public WrappedRpcBuilder(RpcWrapper<T> wrapper) {
        this.wrapper = wrapper;
        logger.finest("Creating new " + this.getClass().getName() + " " + this);
    }
    
    public class RepeatingCallback implements RequestCallback
    {

        private final RepeatingRequestBuilder requestBuilder;
        private final RequestCallback realCallback;
        private String rpcName;

        public RepeatingCallback (RepeatingRequestBuilder rb, RequestCallback rc)
        {
            logger.finest("Creating new RepeatingCallback");
            this.requestBuilder = rb;
            this.realCallback = rc;
        }
        
        public void setRpcName(String rpcName)
        {
            this.rpcName = rpcName;
        }

        //@Override
        public void onResponseReceived (final Request request,
                final Response response)
        {
            logger.finest("Got a response");
            wrapper.getListener().onBeforeResponseProcessing(rpcName, request, response, requestBuilder.getOpaqueRequestInfoHolder(), new ProceedCallback<ResponseDisposition>() {
                
                //@Override
                public void proceed(ResponseDisposition disposition) {
                    switch (disposition) {
                    case PROCESS:
                        logger.finest("Told to process response");
                        realCallback.onResponseReceived(request, response);
                        break;
                        
                    case REPEAT:
                        logger.finest("Told to repeat response");
                        try {
                            requestBuilder.reSend(RepeatingCallback.this);
                        } catch (RequestException e) {
                            logger.log(Level.SEVERE, "Got an error while re-sending request", e);
                            realCallback.onError(request, new InvocationException("Got an error while re-sending request"));
                        }
                        break;
                        
                    case RETURN_EXCEPTION:
                        logger.finest("Told to throw an error response");
                        Throwable exception = disposition.getException();
                        assert null!=exception : "For disposition " + disposition + " exception cannot be null";
                        realCallback.onError(request, exception);
                        break;
                    }
                    return;
                }
            });            
        }

        //@Override
        public void onError (final Request request, final Throwable exception)
        {
            logger.finest("Got an error response");
            wrapper.getListener().onBeforeErrorProcessing(request, exception, requestBuilder.getOpaqueRequestInfoHolder(), new ProceedCallback<ErrorDisposition>() {
                
                //@Override
                public void proceed(ErrorDisposition disposition) {
                    switch (disposition) {
                    case PROCESS:
                        logger.finest("Told to process error response");
                        realCallback.onError(request, exception);
                        break;
                        
                    case REPEAT:
                        try {
                            logger.finest("Told to repeat call on error response");
                            requestBuilder.reSend(RepeatingCallback.this);
                        } catch (RequestException e) {
                            logger.log(Level.SEVERE, "Got an error while re-sending request", e);
                            realCallback.onError(request, new InvocationException("Got an error while re-sending request"));
                        }
                        break;
                        
                    case REPLACE_ERROR:
                        logger.finest("Told to replace error response");
                        Throwable exception = disposition.getException();
                        assert null!=exception : "For disposition " + disposition + " exception cannot be null";
                        realCallback.onError(request, exception);
                        break;
                    }
                }
            });
        }
    }

    public class RepeatingRequestBuilder extends RequestBuilder
    {

        public RepeatingRequestBuilder (RequestBuilder rb)
        {
            super(rb.getHTTPMethod(), rb.getUrl());
            logger.finest("Creating new Request builder");
            this.opaqueRequestInfoHolder = wrapper.getListener().getNewOpaqueRequestInfoHolder();
        }

        private String requestData;
        private String rpcName = null;
        private T opaqueRequestInfoHolder;

        public void reSend (RequestCallback rc) throws RequestException
        {
            logger.finest("reSend()");
            wrapper.getListener().onBeforeRequestResend(opaqueRequestInfoHolder);
            sendRequest(requestData, rc);
        }

        @Override
        public Request send () throws RequestException
        {
            logger.finest("send()");
            wrapper.getListener().onBeforeRequestSend(this, opaqueRequestInfoHolder);
            return super.send();
        }

        @Override
        public Request sendRequest (String rd, RequestCallback rc) throws RequestException
        {
            logger.finest("sendRequest()");
            wrapper.getListener().onBeforeRequestSend(this, opaqueRequestInfoHolder);
            requestData = rd;
            @SuppressWarnings("unchecked")
            RepeatingCallback rrc = (WrappedRpcBuilder<T>.RepeatingCallback) rc;
            rrc.setRpcName(rpcName);
            return super.sendRequest(rd, rc);
        }

        @Override
        public void setRequestData (String s)
        {
            logger.finest("setRequestData()");
            requestData = s;
            super.setRequestData(s);

            this.rpcName = getRpcName(s);
            wrapper.getListener().onRequestDataSet(opaqueRequestInfoHolder, rpcName);
        }

        @Override
        public void setCallback(RequestCallback rc) {
            logger.finest("setCallback()");
            @SuppressWarnings("unchecked")
            RepeatingCallback rrc = (WrappedRpcBuilder<T>.RepeatingCallback) rc;
            rrc.setRpcName(rpcName);
            super.setCallback(rc);
        }
        /**
         * Given the serialized request data for an RPC, this routine constructs
         * the RPC name (RemoteServiceInterface.method).
         * <p/>
         * This is done in javascript because JS and GWT Java split functions
         * behave differently.
         * <p/>
         * This is sensitive to the actual _internal_ implementation of GWT and
         * must be updated if the GWT version changes.
         */
        private native String getRpcName (String requestData)
        /*-{
            parts = requestData.split("|", 7);
            return parts[5]+"."+parts[6];
        }-*/;

        public T getOpaqueRequestInfoHolder ()
        {
            return opaqueRequestInfoHolder;
        }
    }

    @Override
    public RequestBuilder doCreate (String s)
    {
        logger.finest("doCreate()");
        return new RepeatingRequestBuilder(super.doCreate(s));
    }

    @Override
    public void doFinish (RequestBuilder rb)
    {
        logger.finest("doFinish()");
        super.doFinish(rb);
    }

    @Override
    public void doSetCallback (RequestBuilder rb, RequestCallback rc)
    {
        logger.finest("doSetCallback()");
        @SuppressWarnings("unchecked")
        RepeatingRequestBuilder rrb = (WrappedRpcBuilder<T>.RepeatingRequestBuilder) rb;
        super.doSetCallback(rrb, new RepeatingCallback(rrb, rc));
    }
}
