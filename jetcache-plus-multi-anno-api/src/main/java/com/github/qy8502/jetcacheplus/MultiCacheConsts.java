package com.github.qy8502.jetcacheplus;

public interface MultiCacheConsts {

    String EACH_ELEMENT = "$$each$$";
    String RESULT = "result";
    boolean DEFAULT_MULTI = true;

    static boolean isMulti(String value) {
        return value.contains(EACH_ELEMENT);
    }


}
