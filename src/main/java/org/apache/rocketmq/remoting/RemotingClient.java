/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.remoting;

import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.remoting.netty.NettyRequestProcessor;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 远程Client
 */
public interface RemotingClient extends RemotingService {

    /**
     * 更新NameServer地址列表
     *
     * @param addrs 地址列表
     */
    public void updateNameServerAddressList(final List<String> addrs);

    /**
     * 获取NameServer地址列表
     */
    public List<String> getNameServerAddressList();

    /**
     * 同步调用
     *
     * @param addr              server地址
     * @param request           请求
     * @param timeoutMillis     超时时间
     */
    public RemotingCommand invokeSync(final String addr, final RemotingCommand request,
                                      final long timeoutMillis) throws InterruptedException, RemotingConnectException,
        RemotingSendRequestException, RemotingTimeoutException;

    /**
     * 异步调用
     *
     * @param addr              server地址
     * @param request           请求
     * @param timeoutMillis     超时时间
     * @param invokeCallback    回调
     */
    public void invokeAsync(final String addr, final RemotingCommand request, final long timeoutMillis,
                            final InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException,
        RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    /**
     * 单向调用
     *
     * @param addr              server地址
     * @param request           请求
     * @param timeoutMillis     超时时间
     */
    public void invokeOneway(final String addr, final RemotingCommand request, final long timeoutMillis)
        throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException,
        RemotingTimeoutException, RemotingSendRequestException;

    /**
     * 注册请求处理器
     *
     * @param requestCode       请求类型
     * @param processor         请求处理器
     * @param executor          执行线程池
     */
    public void registerProcessor(final int requestCode, final NettyRequestProcessor processor,
                                  final ExecutorService executor);

    /**
     * 通道是否可写
     *
     * @param addr      server地址
     */
    public boolean isChannelWriteable(final String addr);
}
