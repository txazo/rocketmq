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
package org.apache.rocketmq.remoting.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.apache.rocketmq.debug.CommandRequestCode;
import org.apache.rocketmq.debug.NodeNameHolder;
import org.apache.rocketmq.remoting.ChannelEventListener;
import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.common.Pair;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.common.SemaphoreReleaseOnlyOnce;
import org.apache.rocketmq.remoting.common.ServiceThread;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSysResponseCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.*;

public abstract class NettyRemotingAbstract {

    /**
     * Remoting logger instance.
     */
    private static final Logger PLOG = LoggerFactory.getLogger(RemotingHelper.ROCKETMQ_REMOTING);

    private static final Logger CLOG = LoggerFactory.getLogger("RocketmqCommand");

    protected final String nodeName = NodeNameHolder.getNodeName();

    /**
     * Semaphore to limit maximum number of on-going one-way requests, which protects system memory footprint.
     */
    protected final Semaphore semaphoreOneway;

    /**
     * Semaphore to limit maximum number of on-going asynchronous requests, which protects system memory footprint.
     */
    protected final Semaphore semaphoreAsync;

    /**
     * This map caches all on-going requests.
     */
    protected final ConcurrentMap<Integer /* opaque */, ResponseFuture> responseTable =
        new ConcurrentHashMap<Integer, ResponseFuture>(256);

    /**
     * This container holds all processors per request code, aka, for each incoming request, we may look up the
     * responding processor in this map to handle the request.
     */
    protected final HashMap<Integer/* request code */, Pair<NettyRequestProcessor, ExecutorService>> processorTable =
        new HashMap<Integer, Pair<NettyRequestProcessor, ExecutorService>>(64);

    /**
     * Executor to feed netty events to user defined {@link ChannelEventListener}.
     */
    protected final NettyEventExecutor nettyEventExecutor = new NettyEventExecutor();

    /**
     * The default request processor to use in case there is no exact match in {@link #processorTable} per request code.
     */
    protected Pair<NettyRequestProcessor, ExecutorService> defaultRequestProcessor;

    /**
     * Constructor, specifying capacity of one-way and asynchronous semaphores.
     * @param permitsOneway Number of permits for one-way requests.
     * @param permitsAsync Number of permits for asynchronous requests.
     */
    public NettyRemotingAbstract(final int permitsOneway, final int permitsAsync) {
        // 单向请求的最大限制数, 保护内存占用
        this.semaphoreOneway = new Semaphore(permitsOneway, true);
        // 异步请求的最大限制数, 保护内存占用
        this.semaphoreAsync = new Semaphore(permitsAsync, true);
    }

    /**
     * Custom channel event listener.
     * @return custom channel event listener if defined; null otherwise.
     */
    public abstract ChannelEventListener getChannelEventListener();

    /**
     * Put a netty event to the executor.
     * @param event Netty event instance.
     */
    public void putNettyEvent(final NettyEvent event) {
        this.nettyEventExecutor.putNettyEvent(event);
    }

    /**
     * Entry of incoming command processing.
     *
     * <p>
     *     <strong>Note:</strong>
     *     The incoming remoting command may be
     *     <ul>
     *         <li>An inquiry request from a remote peer component;</li>
     *         <li>A response to a previous request issued by this very participant.</li>
     *     </ul>
     * </p>
     * @param ctx Channel handler context.
     * @param msg incoming remoting command.
     * @throws Exception if there were any error while processing the incoming command.
     */
    public void processMessageReceived(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
        final RemotingCommand cmd = msg;
        if (cmd != null) {
            switch (cmd.getType()) {
                case REQUEST_COMMAND:
                    // 处理请求命令
                    CLOG.info("{}", String.format("%-18s %-18s %s %s", nodeName, msg.getNodeName(), CommandRequestCode.command(cmd.getCode()), cmd.extFieldsToString()));
                    processRequestCommand(ctx, cmd);
                    break;
                case RESPONSE_COMMAND:
                    // 处理响应命令
                    processResponseCommand(ctx, cmd);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Process incoming request command issued by remote peer.
     * @param ctx channel handler context.
     * @param cmd request command.
     */
    public void processRequestCommand(final ChannelHandlerContext ctx, final RemotingCommand cmd) {
        // 获取RemotingCommand对应的Pair(请求处理器和执行线程池)
        final Pair<NettyRequestProcessor, ExecutorService> matched = this.processorTable.get(cmd.getCode());
        // 未匹配到, 使用默认Pair(请求处理器和执行线程池)
        final Pair<NettyRequestProcessor, ExecutorService> pair = null == matched ? this.defaultRequestProcessor : matched;
        final int opaque = cmd.getOpaque();

        if (pair != null) {
            Runnable run = new Runnable() {
                @Override
                public void run() {
                    try {
                        // RPC钩子
                        RPCHook rpcHook = NettyRemotingAbstract.this.getRPCHook();
                        if (rpcHook != null) {
                            // RPC钩子请求前回调
                            rpcHook.doBeforeRequest(RemotingHelper.parseChannelRemoteAddr(ctx.channel()), cmd);
                        }

                        // 处理请求
                        final RemotingCommand response = pair.getObject1().processRequest(ctx, cmd);
                        if (rpcHook != null) {
                            // RPC钩子响应后回调
                            rpcHook.doAfterResponse(RemotingHelper.parseChannelRemoteAddr(ctx.channel()), cmd, response);
                        }

                        // 非单向请求
                        if (!cmd.isOnewayRPC()) {
                            if (response != null) {
                                // 请求id = request.opaque
                                response.setOpaque(opaque);
                                // 标记响应类型
                                response.markResponseType();
                                try {
                                    // 写回响应
                                    ctx.writeAndFlush(response);
                                } catch (Throwable e) {
                                    PLOG.error("process request over, but response failed", e);
                                    PLOG.error(cmd.toString());
                                    PLOG.error(response.toString());
                                }
                            } else {

                            }
                        }
                    } catch (Throwable e) {
                        PLOG.error("process request exception", e);
                        PLOG.error(cmd.toString());

                        if (!cmd.isOnewayRPC()) {
                            final RemotingCommand response = RemotingCommand.createResponseCommand(RemotingSysResponseCode.SYSTEM_ERROR, //
                                RemotingHelper.exceptionSimpleDesc(e));
                            response.setOpaque(opaque);
                            ctx.writeAndFlush(response);
                        }
                    }
                }
            };

            // 请求被拒绝
            if (pair.getObject1().rejectRequest()) {
                final RemotingCommand response = RemotingCommand.createResponseCommand(RemotingSysResponseCode.SYSTEM_BUSY,
                    "[REJECTREQUEST]system busy, start flow control for a while");
                response.setOpaque(opaque);
                ctx.writeAndFlush(response);
                return;
            }

            try {
                // 请求任务
                final RequestTask requestTask = new RequestTask(run, ctx.channel(), cmd);
                // 提交请求任务到线程池
                pair.getObject2().submit(requestTask);
            } catch (RejectedExecutionException e) {
                // 请求任务被线程池拒绝
                if ((System.currentTimeMillis() % 10000) == 0) {
                    PLOG.warn(RemotingHelper.parseChannelRemoteAddr(ctx.channel()) //
                        + ", too many requests and system thread pool busy, RejectedExecutionException " //
                        + pair.getObject2().toString() //
                        + " request code: " + cmd.getCode());
                }

                if (!cmd.isOnewayRPC()) {
                    final RemotingCommand response = RemotingCommand.createResponseCommand(RemotingSysResponseCode.SYSTEM_BUSY,
                        "[OVERLOAD]system busy, start flow control for a while");
                    response.setOpaque(opaque);
                    ctx.writeAndFlush(response);
                }
            }
        } else {
            // 未匹配到请求处理器
            String error = " request type " + cmd.getCode() + " not supported";
            final RemotingCommand response =
                RemotingCommand.createResponseCommand(RemotingSysResponseCode.REQUEST_CODE_NOT_SUPPORTED, error);
            response.setOpaque(opaque);
            ctx.writeAndFlush(response);
            PLOG.error(RemotingHelper.parseChannelRemoteAddr(ctx.channel()) + error);
        }
    }

    /**
     * Process response from remote peer to the previous issued requests.
     * @param ctx channel handler context.
     * @param cmd response command instance.
     */
    public void processResponseCommand(ChannelHandlerContext ctx, RemotingCommand cmd) {
        final int opaque = cmd.getOpaque();

        // 根据请求id从响应表中查询ResponseFuture
        final ResponseFuture responseFuture = responseTable.get(opaque);
        if (responseFuture != null) {
            // 响应命令
            responseFuture.setResponseCommand(cmd);

            responseFuture.release();

            responseTable.remove(opaque);

            if (responseFuture.getInvokeCallback() != null) {
                // 执行回调
                executeInvokeCallback(responseFuture);
            } else {
                // 设置response
                responseFuture.putResponse(cmd);
            }
        } else {
            PLOG.warn("receive response, but not matched any request, " + RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
            PLOG.warn(cmd.toString());
        }
    }

    /**
     * Execute callback in callback executor. If callback executor is null, run directly in current thread
     * @param responseFuture
     */
    private void executeInvokeCallback(final ResponseFuture responseFuture) {
        boolean runInThisThread = false;
        // 回调线程池
        ExecutorService executor = this.getCallbackExecutor();
        if (executor != null) {
            try {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // 执行回调
                            responseFuture.executeInvokeCallback();
                        } catch (Throwable e) {
                            PLOG.warn("execute callback in executor exception, and callback throw", e);
                        }
                    }
                });
            } catch (Exception e) {
                runInThisThread = true;
                PLOG.warn("execute callback in executor exception, maybe executor busy", e);
            }
        } else {
            runInThisThread = true;
        }

        if (runInThisThread) {
            try {
                responseFuture.executeInvokeCallback();
            } catch (Throwable e) {
                PLOG.warn("executeInvokeCallback Exception", e);
            }
        }
    }

    /**
     * Custom RPC hook.
     * @return RPC hook if specified; null otherwise.
     */
    public abstract RPCHook getRPCHook();

    /**
     * This method specifies thread pool to use while invoking callback methods.
     * @return Dedicated thread pool instance if specified; or null if the callback is supposed to be executed in the
     * netty event-loop thread.
     */
    public abstract ExecutorService getCallbackExecutor();

    /**
     * <p>
     *    This method is periodically invoked to scan and expire deprecated request.
     * </p>
     */
    public void scanResponseTable() {
        final List<ResponseFuture> rfList = new LinkedList<ResponseFuture>();
        Iterator<Entry<Integer, ResponseFuture>> it = this.responseTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, ResponseFuture> next = it.next();
            ResponseFuture rep = next.getValue();

            // response超时清理
            if ((rep.getBeginTimestamp() + rep.getTimeoutMillis() + 1000) <= System.currentTimeMillis()) {
                rep.release();
                it.remove();
                rfList.add(rep);
                PLOG.warn("remove timeout request, " + rep);
            }
        }

        // 超时response回调
        for (ResponseFuture rf : rfList) {
            try {
                executeInvokeCallback(rf);
            } catch (Throwable e) {
                PLOG.warn("scanResponseTable, operationComplete Exception", e);
            }
        }
    }

    public RemotingCommand invokeSyncImpl(final Channel channel, final RemotingCommand request, final long timeoutMillis)
        throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException {
        // 请求id
        final int opaque = request.getOpaque();

        try {
            // ResponseFuture
            final ResponseFuture responseFuture = new ResponseFuture(opaque, timeoutMillis, null, null);
            // ResponseFuture添加到response表
            this.responseTable.put(opaque, responseFuture);
            final SocketAddress addr = channel.remoteAddress();
            // 写请求并注册操作成功事件
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    if (f.isSuccess()) {
                        // 发送成功
                        responseFuture.setSendRequestOK(true);
                        // 返回
                        return;
                    } else {
                        // 发送失败
                        responseFuture.setSendRequestOK(false);
                    }

                    // response表中清除当前请求
                    responseTable.remove(opaque);
                    // 记录异常
                    responseFuture.setCause(f.cause());
                    responseFuture.putResponse(null);
                    PLOG.warn("send a request command to channel <" + addr + "> failed.");
                }
            });

            // 等待响应
            RemotingCommand responseCommand = responseFuture.waitResponse(timeoutMillis);
            if (null == responseCommand) {
                if (responseFuture.isSendRequestOK()) {
                    // 响应超时
                    throw new RemotingTimeoutException(RemotingHelper.parseSocketAddressAddr(addr), timeoutMillis,
                        responseFuture.getCause());
                } else {
                    // 发送失败
                    throw new RemotingSendRequestException(RemotingHelper.parseSocketAddressAddr(addr), responseFuture.getCause());
                }
            }

            return responseCommand;
        } finally {
            // response表中清除当前请求
            this.responseTable.remove(opaque);
        }
    }

    public void invokeAsyncImpl(final Channel channel, final RemotingCommand request, final long timeoutMillis,
        final InvokeCallback invokeCallback)
        throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        // 请求id
        final int opaque = request.getOpaque();
        // 获取异步请求许可
        boolean acquired = this.semaphoreAsync.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
        if (acquired) {
            final SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreAsync);

            final ResponseFuture responseFuture = new ResponseFuture(opaque, timeoutMillis, invokeCallback, once);
            this.responseTable.put(opaque, responseFuture);
            try {
                channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture f) throws Exception {
                        if (f.isSuccess()) {
                            responseFuture.setSendRequestOK(true);
                            return;
                        } else {
                            responseFuture.setSendRequestOK(false);
                        }

                        responseFuture.putResponse(null);
                        responseTable.remove(opaque);
                        try {
                            // 执行回调
                            executeInvokeCallback(responseFuture);
                        } catch (Throwable e) {
                            PLOG.warn("excute callback in writeAndFlush addListener, and callback throw", e);
                        } finally {
                            responseFuture.release();
                        }

                        PLOG.warn("send a request command to channel <{}> failed.", RemotingHelper.parseChannelRemoteAddr(channel));
                    }
                });
            } catch (Exception e) {
                responseFuture.release();
                PLOG.warn("send a request command to channel <" + RemotingHelper.parseChannelRemoteAddr(channel) + "> Exception", e);
                throw new RemotingSendRequestException(RemotingHelper.parseChannelRemoteAddr(channel), e);
            }
        } else {
            if (timeoutMillis <= 0) {
                throw new RemotingTooMuchRequestException("invokeAsyncImpl invoke too fast");
            } else {
                String info =
                    String.format("invokeAsyncImpl tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreAsyncValue: %d", //
                        timeoutMillis, //
                        this.semaphoreAsync.getQueueLength(), //
                        this.semaphoreAsync.availablePermits()//
                    );
                PLOG.warn(info);
                throw new RemotingTimeoutException(info);
            }
        }
    }

    public void invokeOnewayImpl(final Channel channel, final RemotingCommand request, final long timeoutMillis)
        throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        // 标记单向RPC
        request.markOnewayRPC();
        // 获取单向请求许可
        boolean acquired = this.semaphoreOneway.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
        if (acquired) {
            final SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreOneway);
            try {
                channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture f) throws Exception {
                        once.release();
                        if (!f.isSuccess()) {
                            PLOG.warn("send a request command to channel <" + channel.remoteAddress() + "> failed.");
                        }
                    }
                });
            } catch (Exception e) {
                once.release();
                PLOG.warn("write send a request command to channel <" + channel.remoteAddress() + "> failed.");
                throw new RemotingSendRequestException(RemotingHelper.parseChannelRemoteAddr(channel), e);
            }
        } else {
            if (timeoutMillis <= 0) {
                throw new RemotingTooMuchRequestException("invokeOnewayImpl invoke too fast");
            } else {
                String info = String.format(
                    "invokeOnewayImpl tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreAsyncValue: %d", //
                    timeoutMillis, //
                    this.semaphoreOneway.getQueueLength(), //
                    this.semaphoreOneway.availablePermits()//
                );
                PLOG.warn(info);
                throw new RemotingTimeoutException(info);
            }
        }
    }

    /**
     * Netty事件执行线程
     */
    class NettyEventExecutor extends ServiceThread {

        // 事件队列
        private final LinkedBlockingQueue<NettyEvent> eventQueue = new LinkedBlockingQueue<NettyEvent>();
        // 队列大小限制
        private final int maxSize = 10000;

        /**
         * 添加Netty事件
         */
        public void putNettyEvent(final NettyEvent event) {
            if (this.eventQueue.size() <= maxSize) {
                this.eventQueue.add(event);
            } else {
                PLOG.warn("event queue size[{}] enough, so drop this event {}", this.eventQueue.size(), event.toString());
            }
        }

        @Override
        public void run() {
            PLOG.info(this.getServiceName() + " service started");

            // 通道事件监听器
            final ChannelEventListener listener = NettyRemotingAbstract.this.getChannelEventListener();

            while (!this.isStopped()) {
                try {
                    NettyEvent event = this.eventQueue.poll(3000, TimeUnit.MILLISECONDS);
                    if (event != null && listener != null) {
                        // 事件回调
                        switch (event.getType()) {
                            case IDLE:
                                listener.onChannelIdle(event.getRemoteAddr(), event.getChannel());
                                break;
                            case CLOSE:
                                listener.onChannelClose(event.getRemoteAddr(), event.getChannel());
                                break;
                            case CONNECT:
                                listener.onChannelConnect(event.getRemoteAddr(), event.getChannel());
                                break;
                            case EXCEPTION:
                                listener.onChannelException(event.getRemoteAddr(), event.getChannel());
                                break;
                            default:
                                break;

                        }
                    }
                } catch (Exception e) {
                    PLOG.warn(this.getServiceName() + " service has exception. ", e);
                }
            }

            PLOG.info(this.getServiceName() + " service end");
        }

        @Override
        public String getServiceName() {
            return NettyEventExecutor.class.getSimpleName();
        }
    }
}
