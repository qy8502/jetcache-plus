package io.github.qy8502.jetcacheplus;

import java.util.*;

public class MultiCacheMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 4855332394673640836L;

    private boolean multiResult = false;
    private int multiArgIndex = -1;

    private MultiCacheMap values;
    private MultiCacheMap conditions;

    public boolean isMultiResult() {
        return multiResult;
    }

    public void setMultiResult(boolean multiResult) {
        this.multiResult = multiResult;
    }

    public int getMultiArgIndex() {
        return multiArgIndex;
    }

    public void setMultiArgIndex(int multiArgIndex) {
        this.multiArgIndex = multiArgIndex;
    }

    public MultiCacheMap getValues() {
        return values;
    }

    public void setValues(MultiCacheMap values) {
        this.values = values;
    }

    public MultiCacheMap getConditions() {
        return conditions;
    }

    public void setConditions(MultiCacheMap conditions) {
        this.conditions = conditions;
    }

    public boolean getCondition(K key) {
        if (this.conditions == null) {
            return true;
        }
        return !Boolean.FALSE.equals(this.conditions.get(key));
    }

    public Object getValue(K key) {
        if (this.values == null) {
            if (multiResult) {
                return key;
            } else {
                return null;
            }
        }
        return this.values.get(key);
    }

    private Map<V, K> element;

    public Object getElement(V value) {
        if (element == null) {
            element = new HashMap<>();
            forEach((k, v) -> element.put(v, k));
        }
        return element.get(value);
    }

    public static <V> DefaultValueMap<Object, V> defaultValueMap(V defalutValue) {
        return new DefaultValueMap<>(defalutValue);
    }

    private static class DefaultValueMap<K, V> extends AbstractMap<K, V> {

        private V defalutValue;

        public DefaultValueMap(V defalutValue) {
            this.defalutValue = defalutValue;
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }

        @Override
        public V get(Object key) {
            return defalutValue;
        }
    }
}
