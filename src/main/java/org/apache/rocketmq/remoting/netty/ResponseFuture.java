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

import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.common.SemaphoreReleaseOnlyOnce;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 响应Future
 */
public class ResponseFuture {

    // 请求id
    private final int opaque;
    // 超时时间
    private final long timeoutMillis;
    // 响应回调
    private final InvokeCallback invokeCallback;
    // 请求开始时间
    private final long beginTimestamp = System.currentTimeMillis();
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    private final SemaphoreReleaseOnlyOnce once;

    private final AtomicBoolean executeCallbackOnlyOnce = new AtomicBoolean(false);

    // 响应命令
    private volatile RemotingCommand responseCommand;
    // 请求是否发送成功
    private volatile boolean sendRequestOK = true;
    // 异常
    private volatile Throwable cause;

    public ResponseFuture(int opaque, long timeoutMillis, InvokeCallback invokeCallback,
        SemaphoreReleaseOnlyOnce once) {
        this.opaque = opaque;
        this.timeoutMillis = timeoutMillis;
        this.invokeCallback = invokeCallback;
        this.once = once;
    }

    public void executeInvokeCallback() {
        // 回调不为null
        if (invokeCallback != null) {
            // 控制只回调一次
            if (this.executeCallbackOnlyOnce.compareAndSet(false, true)) {
                // 回调
                invokeCallback.operationComplete(this);
            }
        }
    }

    public void release() {
        if (this.once != null) {
            this.once.release();
        }
    }

    public boolean isTimeout() {
        long diff = System.currentTimeMillis() - this.beginTimestamp;
        return diff > this.timeoutMillis;
    }

    /**
     * 等待response
     */
    public RemotingCommand waitResponse(final long timeoutMillis) throws InterruptedException {
        // 等待直到超时
        this.countDownLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        return this.responseCommand;
    }

    /**
     * 设置response
     */
    public void putResponse(final RemotingCommand responseCommand) {
        this.responseCommand = responseCommand;
        // 唤醒等待
        this.countDownLatch.countDown();
    }

    public long getBeginTimestamp() {
        return beginTimestamp;
    }

    public boolean isSendRequestOK() {
        return sendRequestOK;
    }

    public void setSendRequestOK(boolean sendRequestOK) {
        this.sendRequestOK = sendRequestOK;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public InvokeCallback getInvokeCallback() {
        return invokeCallback;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public RemotingCommand getResponseCommand() {
        return responseCommand;
    }

    public void setResponseCommand(RemotingCommand responseCommand) {
        this.responseCommand = responseCommand;
    }

    public int getOpaque() {
        return opaque;
    }

    @Override
    public String toString() {
        return "ResponseFuture [responseCommand=" + responseCommand + ", sendRequestOK=" + sendRequestOK
            + ", cause=" + cause + ", opaque=" + opaque + ", timeoutMillis=" + timeoutMillis
            + ", invokeCallback=" + invokeCallback + ", beginTimestamp=" + beginTimestamp
            + ", countDownLatch=" + countDownLatch + "]";
    }
}
