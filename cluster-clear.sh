#!/bin/bash

function clearUserHome() {
    rm -rf $1/logs
    rm -rf $1/store
}

clearUserHome "cluster/broker-1-master"
clearUserHome "cluster/broker-1-slave"
clearUserHome "cluster/broker-2-master"
clearUserHome "cluster/broker-2-slave"
clearUserHome "cluster/namesrv-1"
clearUserHome "cluster/namesrv-2"
