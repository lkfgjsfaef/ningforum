package com.app.mq;

import lombok.Data;

import java.io.Serializable;

@Data
public class CacheInvalidationMessage implements Serializable {

    private String cacheKey;

    private String pattern;

    private String type;
}
