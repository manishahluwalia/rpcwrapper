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

import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

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
public class RpcWrapper<T>
{
    private static final Logger logger = Logger.getLogger(RpcWrapper.class.getName());

    private final WrappedRpcBuilder<T> builder;

    private RpcWrapperListener<T> listener = new DefaultRpcWrapperListener<T>();

    public RpcWrapper() {
        logger.finest("Creating new " + this.getClass().getName() + " " + this);
        builder = new WrappedRpcBuilder<T>(this);
    }
    
    public void setListener(RpcWrapperListener<T> listener) {
        logger.finest("Listener set");
        this.listener = listener;
    }
    
    public RpcWrapperListener<T> getListener() {
        return listener;
    }

    /**
     * Given a basic GWT-RPC proxy service, wrap it to have the qualities of
     * {@link RpcWrapper}
     *
     * @param service
     *            A GWT-RPC proxy. Usually, acquired through
     *            {@link GWT#create(Class)}
     * @return A wrapped GWT-RPC proxy
     */
    public ServiceDefTarget wrapService (ServiceDefTarget service)
    {
        logger.finest("Wrapping service: " + service.getClass().getName() + " at " + service.getServiceEntryPoint() + " with policy " + service.getSerializationPolicyName());
        service.setRpcRequestBuilder(builder);
        return service;
    }
}
