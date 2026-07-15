package com.everypicfound.imageasset.domain.generator;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.CommonErrorCode;
import com.everypicfound.common.exception.SystemException;

/*
 雪花ID 是将几个信息拼成一个Long:| 时间戳差值 | workerId | sequence |
 自增ID在分库的时候会出现相同ID的问题，使用雪花ID则可以保证全局唯一性
*/
@Component
public class SnowflakeImageIdGenerator implements ImageIdGenerator {

    /**
    * 自定义起始时间戳：2026-05-20 00:00:00 Asia/Shanghai
    */

    private static final long EPOCH = 1779206400000L;

    //这俩BITS 的意思是 WORKER_ID和SEQUENCE 序列的长度
    private static final long WORKER_ID_BITS = 10L;

    private static final long SEQUENCE_BITS = 12L;

    //不能等于或超过 1111....1000000000000 (12个0) 
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    //偏移
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    private static final long TIMESTAMP_SHIFT = WORKER_ID_BITS + WORKER_ID_SHIFT;

    private static final long WORKER_ID = 1L; //单机环境下先固定

    private long lastTimeStamp = -1L;

    private long sequence = 0L;

    //这里用的是一个long 64位来存放，所以需要通过移位操作来控制WORKER_ID和SEQUENCE的实际有效值都在对应位上

    @Override
    public synchronized Long nextId() {
        long currentTimeStamp = currentTimeMillis();

        if (currentTimeStamp < lastTimeStamp) {
            throw new SystemException(CommonErrorCode.SYSTEM_ERROR);
        }


        /*
        同一TimeStamp则sequence + 1, 如果Sequence达到最大则要等到下一个timeStamp然后开始新sequence
        不同timeStamp则重置sequence开始新计数
        */
        if (currentTimeStamp == lastTimeStamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                currentTimeStamp = waitNextMillis(lastTimeStamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimeStamp = currentTimeStamp;

        return ((currentTimeStamp - EPOCH) << TIMESTAMP_SHIFT)
                | (WORKER_ID << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long lastTimeStamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimeStamp) {
            timestamp = currentTimeMillis();
        }
        
        return timestamp;
    }
    
    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
