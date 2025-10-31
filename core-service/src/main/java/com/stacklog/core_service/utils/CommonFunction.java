package com.stacklog.core_service.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class CommonFunction {
    
    public static LocalDateTime getCurrentTime() {
        return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

}
