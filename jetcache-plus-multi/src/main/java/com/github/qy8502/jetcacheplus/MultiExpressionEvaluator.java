package com.github.qy8502.jetcacheplus;

import com.alicp.jetcache.CacheConfigException;
import com.alicp.jetcache.anno.method.CacheInvokeContext;
import org.mvel2.MVEL;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiExpressionEvaluator implements Function<Object, Object> {
    private static final Pattern pattern = Pattern.compile("\\s*(\\w+)\\s*\\{(.+)\\}\\s*");
    private Function<Object, Object> target;

    private boolean multi = false;

    public boolean isMulti() {
        return multi;
    }

    public MultiExpressionEvaluator(String script, Method defineMethod) {
        Object rt[] = parseEL(script);
        EL el = (EL) rt[0];
        String realScript = (String) rt[1];
        if (el == EL.MVEL) {
            target = new MvelEvaluator(realScript);
        } else if (el == EL.SPRING_EL) {
            multi = MultiCacheConsts.isMulti(script);
            if (multi) {
                target = new MutliSpelEvaluator(realScript, defineMethod);
            } else {
                target = new SpelEvaluator(realScript, defineMethod);
            }
        }
    }

    private Object[] parseEL(String script) {
        if (script == null || script.trim().equals("")) {
            return null;
        }
        Object[] rt = new Object[2];
        Matcher matcher = pattern.matcher(script);
        if (!matcher.matches()) {
            rt[0] = EL.SPRING_EL; // default spel since 2.4
            rt[1] = script;
            return rt;
        } else {
            String s = matcher.group(1);
            if ("spel".equals(s)) {
                rt[0] = EL.SPRING_EL;
            } else if ("mvel".equals(s)) {
                rt[0] = EL.MVEL;
            }/* else if ("buildin".equals(s)) {
                rt[0] = EL.BUILD_IN;
            } */ else {
                throw new CacheConfigException("Can't parse \"" + script + "\"");
            }
            rt[1] = matcher.group(2);
            return rt;
        }
    }

    @Override
    public Object apply(Object o) {
        return target.apply(o);
    }

    Function<Object, Object> getTarget() {
        return target;
    }
}

class MvelEvaluator implements Function<Object, Object> {
    private String script;

    public MvelEvaluator(String script) {
        this.script = script;
    }

    @Override
    public Object apply(Object context) {
        return MVEL.eval(script, context);
    }
}


class SpelEvaluator implements Function<Object, Object> {

    private static ExpressionParser parser;
    private static ParameterNameDiscoverer parameterNameDiscoverer;

    static {
        parser = new SpelExpressionParser();
        parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    }

    private final Expression expression;
    private String[] parameterNames;

    public SpelEvaluator(String script, Method defineMethod) {
        expression = parser.parseExpression(script);
        if (defineMethod.getParameterCount() > 0) {
            parameterNames = parameterNameDiscoverer.getParameterNames(defineMethod);
        }
    }

    @Override
    public Object apply(Object rootObject) {
        EvaluationContext context = new StandardEvaluationContext(rootObject);
        CacheInvokeContext cic = (CacheInvokeContext) rootObject;
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], cic.getArgs()[i]);
            }
        }
        context.setVariable("result", cic.getResult());
        return expression.getValue(context);
    }
}

class MutliSpelEvaluator implements Function<Object, Object> {

    private static ExpressionParser parser;
    private static ParameterNameDiscoverer parameterNameDiscoverer;
    private static final Pattern patternMultiKey = Pattern.compile("#(.*?)\\[" + MultiCacheConsts.EACH_ELEMENT.replace("$", "\\$") + "\\]");

    static {
        parser = new SpelExpressionParser();
        parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    }

    private final Expression expression;
    private String[] parameterNames;
    private String multiArg;
    private boolean multiResult = false;
    private int multiArgIndex = -1;

    public MutliSpelEvaluator(String script, Method defineMethod) {
        //region Description
        if (defineMethod.getParameterCount() > 0) {
            parameterNames = parameterNameDiscoverer.getParameterNames(defineMethod);
        }
        Matcher multiKeyMatcher = patternMultiKey.matcher(script);
        Set<String> eachArgs = new HashSet<>();
        while (multiKeyMatcher.find()) {
            eachArgs.add(multiKeyMatcher.group(1));
        }
        if (eachArgs.isEmpty()) {
            throw new IllegalArgumentException("el contain $$each$$ but format isn't right");
        } else if (eachArgs.size() > 1) {
            throw new IllegalArgumentException("el can only contain one $$each$$ arg/result");
        }
        multiArg = eachArgs.iterator().next();
        if (MultiCacheConsts.RESULT.equals(multiArg)) {
            multiResult = true;
        } else {
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    if (parameterNames[i].contains(multiArg)) {
                        multiArgIndex = i;
                    }
                }
            }
            if (multiArgIndex < 0) {
                throw new IllegalArgumentException("el argument " + multiArg + " does not exist");
            }
        }


        script = script.replace(MultiCacheConsts.EACH_ELEMENT, "0");
        expression = parser.parseExpression(script);
        //endregion
    }

    @Override
    public Object apply(Object rootObject) {
        CacheInvokeContext cic = (CacheInvokeContext) rootObject;
        MultiCacheMap<Object, Object> values = new MultiCacheMap<>();
        values.setMultiArgIndex(multiArgIndex);
        values.setMultiResult(multiResult);
        Iterable elements;
        if (multiResult) {
            elements = MultiCacheHandler.toIterable(cic.getResult());
        } else {
            elements = MultiCacheHandler.toIterable(cic.getArgs()[multiArgIndex]);
        }
        if (elements == null) {
            throw new IllegalArgumentException(multiArg + " can not to be Iterable");
        }
        elements.forEach(element -> {
            EvaluationContext context = new StandardEvaluationContext(rootObject);
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    if (i == multiArgIndex) {
                        context.setVariable(parameterNames[i], new Object[]{element});
                    } else {
                        context.setVariable(parameterNames[i], cic.getArgs()[i]);
                    }
                }
            }
            if (multiResult) {
                context.setVariable(MultiCacheConsts.RESULT, new Object[]{element});
            }
            values.put(element, expression.getValue(context));
        });
        return values;

    }
}

enum EL {
    BUILD_IN, MVEL, SPRING_EL
}
