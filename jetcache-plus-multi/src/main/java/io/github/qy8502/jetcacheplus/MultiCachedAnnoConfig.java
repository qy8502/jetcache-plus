package io.github.qy8502.jetcacheplus;

import com.alicp.jetcache.anno.support.CachedAnnoConfig;

import java.util.function.Function;

public class MultiCachedAnnoConfig extends CachedAnnoConfig {

    public MultiCachedAnnoConfig(CachedAnnoConfig cac) {
        this.setArea(cac.getArea());
        this.setName(cac.getName());
        this.setCacheType(cac.getCacheType());
        this.setEnabled(cac.isEnabled());
        this.setTimeUnit(cac.getTimeUnit());
        this.setExpire(cac.getExpire());
        this.setLocalExpire(cac.getLocalExpire());
        this.setLocalLimit(cac.getLocalLimit());
        this.setCacheNullValue(cac.isCacheNullValue());
        this.setCondition(cac.getCondition());
        this.setPostCondition(cac.getPostCondition());
        this.setSerialPolicy(cac.getSerialPolicy());
        this.setKeyConvertor(cac.getKeyConvertor());
        this.setKey(cac.getKey());
        this.setKeyEvaluator(cac.getKeyEvaluator());
        this.setDefineMethod(cac.getDefineMethod());
        this.setRefreshPolicy(cac.getRefreshPolicy());
        this.setPenetrationProtectConfig(cac.getPenetrationProtectConfig());
        MultiCached anno = cac.getDefineMethod().getAnnotation(MultiCached.class);
        if (anno != null) {
            this.value = anno.value();
            this.postKey = anno.postKey();
            this.multi = anno.multi();
        }

    }


    private boolean multi;

    private String postKey;
    private Function<Object, Object> postKeyEvaluator;

    private Function<Object, Object> conditionMultiEvaluator;
    private Function<Object, Object> postConditionMultiEvaluator;


    private String value;
    private Function<Object, Object> valueEvaluator;

    public boolean isMulti() {
        return multi;
    }

    public void setMulti(boolean multi) {
        this.multi = multi;
    }

    public String getPostKey() {
        return postKey;
    }

    public void setPostKey(String key) {
        this.postKey = key;
    }

    public Function<Object, Object> getPostKeyEvaluator() {
        return postKeyEvaluator;
    }

    public void setPostKeyEvaluator(Function<Object, Object> keyEvaluator) {
        this.postKeyEvaluator = keyEvaluator;
    }


    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Function<Object, Object> getValueEvaluator() {
        return valueEvaluator;
    }

    public void setValueEvaluator(Function<Object, Object> valueEvaluator) {
        this.valueEvaluator = valueEvaluator;
    }


    public Function<Object, Object> getConditionMultiEvaluator() {
        return conditionMultiEvaluator;
    }

    public void setConditionMultiEvaluator(Function<Object, Object> conditionMultiEvaluator) {
        this.conditionMultiEvaluator = conditionMultiEvaluator;
    }


    public Function<Object, Object> getPostConditionMultiEvaluator() {
        return postConditionMultiEvaluator;
    }

    public void setPostConditionMultiEvaluator(Function<Object, Object> postConditionMultiEvaluator) {
        this.postConditionMultiEvaluator = postConditionMultiEvaluator;
    }

}