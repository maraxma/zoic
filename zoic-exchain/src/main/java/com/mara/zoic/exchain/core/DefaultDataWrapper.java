package com.mara.zoic.exchain.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据包装器默认实现。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-03-29
 * @see DataWrapper
 */
class DefaultDataWrapper<T> implements DataWrapper<T> {

    T data;
    boolean ignoreFollowingValidators = false;
    final Set<String> IGNORED_VALIDATORS = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

    ValidationResult<T> validatedInfo;

    DefaultDataWrapper(T data, String validationName) {
        this.data = data;
        DefaultValidationResult<T> validatedInfo = new DefaultValidationResult<>();
        validatedInfo.originalData = data;
        validatedInfo.validationName = validationName;
        this.validatedInfo = validatedInfo;
    }

    @Override
    public T getOriginalData() {
        return data;
    }

    @Override
    public void setIgnoreAllFollowingValidators(boolean ignore) {
        ignoreFollowingValidators = ignore;
    }

    @Override
    public boolean isIgnoreAllFollowingValidators() {
        return ignoreFollowingValidators;
    }

    @Override
    public void setIgnoreValidators(String... validatorNames) {
        synchronized (IGNORED_VALIDATORS) {
            IGNORED_VALIDATORS.clear();
            addIgnoreValidators(validatorNames);
        }
    }

    @Override
    public void addIgnoreValidators(String... validatorNames) {
        IGNORED_VALIDATORS.addAll(Arrays.stream(validatorNames).collect(Collectors.toList()));
    }

    @Override
    public void removeIgnoredValidators(String... validatorNames) {
        for (String validatorName : validatorNames) {
            IGNORED_VALIDATORS.remove(validatorName);
        }
    }

    @Override
    public void clearIgnoredValidators() {
        IGNORED_VALIDATORS.clear();
    }

    @Override
    public Set<String> getIgnoredValidators() {
        return Collections.unmodifiableSet(IGNORED_VALIDATORS);
    }

    /**
     * {@inheritDoc}
     * <p>注意：如果Validation流程较长，此方法执行效率会降低。
     */
    @Override
    public Set<String> getFutureValidators() {

        ValidationContext context = ValidationContexts.autoDetect();
        String validatorName = context.getValidatorName();
        AbstractValidator<?> validator = context.getValidator();
        DefaultValidation validation = (DefaultValidation) context.getValidation();
        Validation.ValidatorNode validatorNode = validation.getRootValidatorNode();
        // 先找到当前的Node
        Set<String> ret = new HashSet<>();
        Stack<Validation.ValidatorNode> stack = new Stack<>();
        stack.push(validatorNode);
        boolean found = false;
        while (!stack.isEmpty()) {
            Validation.ValidatorNode node = stack.pop();
            if (node.getNext() != null) {
                stack.push(node.getNext());
            }
            if (!found && !node.isParallelNode() && node.getValidator().getName().equals(validator.getClass().getName()) && node.getName().equals(validatorName)) {
                // 从这个节点开始
                found = true;
                Validation.ValidatorNode container;
                Validation.ValidatorNode cur = node;
                while ((container = cur.getContainer()) != null) {
                    con: for (Validation.ValidatorNode pn : container.getParallelNodes()) {
                        // 将兄弟节点中的另一只流程添加
                        Validation.ValidatorNode n = pn;
                        while (n != null) {
                            if (n.getName().equals(cur.getName())) {
                                // 找到属于本节点的一只流程，排除掉
                                continue con;
                            }
                            n = n.getNext();
                        }
                        stack.push(pn);
                    }
                    cur = container;
                }
                continue;
            }
            if (!node.isParallelNode()) {
                if (found) {
                    ret.add(node.getName());
                }
            } else {
                List<Validation.ValidatorNode> parallelNodes = node.getParallelNodes();
                for (Validation.ValidatorNode parallelNode : parallelNodes) {
                    stack.push(parallelNode);
                }
            }
        }
        // 在返回结果之前还要移除已经设定的被忽略的验证器
        Set<String> ignoredValidators = getIgnoredValidators();
        for (String iv : ignoredValidators) {
            if (!(iv.startsWith("/") && iv.endsWith("/"))) {
                ret.remove(iv);
            } else {
                for (Iterator<String> iterator = ret.iterator(); iterator.hasNext();) {;
                    String s = iterator.next();
                    if (s.matches(iv.substring(1, iv.length() - 1))) {
                        iterator.remove();
                    }
                }
            }
        }

        return ret;
    }

    @Override
    public ValidationResult<T> getValidatedInfo() {
        return validatedInfo;
    }
}
