package com.everypicfound.imageasset.domain.generator;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.common.exception.CommonErrorCode;
import com.everypicfound.common.exception.SystemException;
import com.everypicfound.imageasset.error.ImageAssetErrorCode;

import java.io.InputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/*
Sha256哈希算法将任意长度的数据变成256位即32bytes的结果，然后我们将其转成16进制字符串, 即64位字符串
这里的接口计算流程就是以8k字节为一段，一段一段将文件字节读取并送进计算器，之后由计算器digest成64位16进制字符串
*/
@Component
public class Sha256FileHashCalculator implements FileHashCalculator {
    
    private static final int BUFFER_SIZE = 8 * 1024;

    @Override
    public String calculateHash(InputStream inputStream) {
        if (inputStream == null) {
            throw new BizException(ImageAssetErrorCode.IMAGE_EMPTY);
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // 每次读进 8192 字节
            byte[] buffer = new byte[BUFFER_SIZE];
            int readLength;

            while ((readLength = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, readLength);
            }

            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException exception) {
            throw new SystemException(CommonErrorCode.SYSTEM_ERROR, exception);
        }
    }
    
    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            String hex = Integer.toHexString(value & 0xff);

            // 保证每个bytes都变成两位字符串 0x5 -> '05'
            if (hex.length() == 1) {
                builder.append('0');
            }

            builder.append(hex);
        }

        return builder.toString();
    }


}
