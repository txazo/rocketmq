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

import io.netty.channel.Channel;
import org.apache.rocketmq.remoting.common.Pair;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.remoting.netty.NettyRequestProcessor;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

import java.util.concurrent.ExecutorService;

/**
 * 远程Server
 */
public interface RemotingServer extends RemotingService {

    /**
     * 注册请求处理器
     *
     * @param requestCode   请求类型
     * @param processor     请求处理器
     * @param executor      执行线程池
     */
    void registerProcessor(final int requestCode, final NettyRequestProcessor processor,
                           final ExecutorService executor);

    /**
     * 注册默认请求处理器
     *
     * @param processor     请求处理器
     * @param executor      执行线程池
     */
    void registerDefaultProcessor(final NettyRequestProcessor processor, final ExecutorService executor);

    /**
     * 本地监听端口
     */
    int localListenPort();

    /**
     * 获取请求类型对应的请求处理器和执行线程池
     *
     * @param requestCode   请求类型
     */
    Pair<NettyRequestProcessor, ExecutorService> getProcessorPair(final int requestCode);

    /**
     * 同步调用
     *
     * @param channel           通道
     * @param request           请求命令
     * @param timeoutMillis     超时时间
     */
    RemotingCommand invokeSync(final Channel channel, final RemotingCommand request,
                               final long timeoutMillis) throws InterruptedException, RemotingSendRequestException,
        RemotingTimeoutException;

    /**
     * 异步调用
     *
     * @param channel           通道
     * @param request           请求命令
     * @param timeoutMillis     超时时间
     * @param invokeCallback    回调
     */
    void invokeAsync(final Channel channel, final RemotingCommand request, final long timeoutMillis,
                     final InvokeCallback invokeCallback) throws InterruptedException,
        RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    /**
     * 单向调用
     *
     * @param channel           通道
     * @param request           请求命令
     * @param timeoutMillis     超时时间
     */
    void invokeOneway(final Channel channel, final RemotingCommand request, final long timeoutMillis)
        throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException,
        RemotingSendRequestException;

}
