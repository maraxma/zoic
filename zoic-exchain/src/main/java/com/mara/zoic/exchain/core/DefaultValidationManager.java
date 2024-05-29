package com.mara.zoic.exchain.core;

import com.mara.zoic.exchain.core.Validation.ValidatorNode;

import java.io.Serial;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * {@link ValidationManager} 的默认实现。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-03-26
 */
public class DefaultValidationManager implements ValidationManager {

    private final ConcurrentHashMap<String, Validation> registered = new ConcurrentHashMap<>(10);

    @Override
    public void register(String validationName, Validation validation) {
        registered.put(validationName, validation);
    }

    @Override
    public void deregister(String validationName) {
        registered.remove(validationName);
    }

    @Override
    public <T> Map<T, ValidationResult<T>> validate(String validationName, Set<T> data, ExecutorService executorService) {
        return innerValidate(validationName, data, executorService, false, ValidationMode.getDefault(), null);
    }

    @Override
    public <T> Map<T, ValidationResult<T>> validate(String validationName, Set<T> data, ExecutorService executorService, ValidationMode validationMode, ValidationMessage.MessageType stopMessageType) {
        return innerValidate(validationName, data, executorService, false, validationMode, stopMessageType);
    }

    @Override
    public <T> Map<T, ValidationResult<T>> validateForkJoin(String validationName, Set<T> data, ForkJoinPool forkJoinPool) {
        return innerValidate(validationName, data, forkJoinPool, true, ValidationMode.getDefault(), null);
    }

    @Override
    public <T> Map<T, ValidationResult<T>> validateForkJoin(String validationName, Set<T> data, ForkJoinPool forkJoinPool, ValidationMode validationMode, ValidationMessage.MessageType stopMessageType) {
        return innerValidate(validationName, data, forkJoinPool, true, validationMode, stopMessageType);
    }

    @Override
    public <T> Map<T, ValidationResult<T>> validate(String validationName, Set<T> data) {
        return innerValidate(validationName, data, null, false, ValidationMode.getDefault(), null);
    }

    @Override
    public <T> Map<T, ValidationResult<T>> validate(String validationName, Set<T> data, ValidationMode validationMode, ValidationMessage.MessageType stopMessageType) {
        return innerValidate(validationName, data, null, false, validationMode, stopMessageType);
    }

    private <T> Map<T, ValidationResult<T>> innerValidate(String validationName, Set<T> data, ExecutorService executorService, boolean forkJoin, ValidationMode validationMode,
                                                          ValidationMessage.MessageType stopMessageType) {
        Validation validation = registered.get(validationName);
        if (validation == null) {
            throw new IllegalStateException("Validation `" + validationName + "` is not exist or not register");
        }
        try (DefaultValidationContext validationContext = new DefaultValidationContext(this, validation, validationName, validationMode, stopMessageType)) {
            Set<DataWrapper<T>> dataWrappers = data.stream().map(d -> new DefaultDataWrapper<>(d, validationName)).collect(Collectors.toSet());
            ValidationContextHolder.HOLDER.set(validationContext);
            if (forkJoin) {
                return ((ForkJoinPool) executorService).invoke(new ValidatorTask<>(validationContext, validation.getRootValidatorNode(), dataWrappers, Collections.emptyList(), -1));
            } else {
                return visitAndExecuteNode(validationContext, validation.getRootValidatorNode(), dataWrappers, executorService, Collections.emptyList(), -1);
            }
        } finally {
            // 记得清理ThreadLocal
            ValidationContextHolder.HOLDER.remove();
        }
    }

    private <T> Map<T, ValidationResult<T>> visitAndExecuteNode(DefaultValidationContext validationContext, ValidatorNode rootNode, Set<DataWrapper<T>> dataWrappers, ExecutorService executorService, List<AtomicBoolean> parallelValidatorsCancellationFlag, int cancellationFlagIndex) throws IllegalStateException {
        // ValidationContext里面包含了ThreadLocal，必须在用完之后予以清理
        Map<T, ValidationResult<T>> combinedResult = new ConcurrentHashMap<>(16);
        // 开始遍历执行验证
        // 使用堆栈来避免递归调用
        Stack<ValidatorNode> stack = new Stack<>();
        stack.add(rootNode);
        while (!stack.isEmpty()) {
            ValidatorNode node = stack.pop();
            if (node.getNext() != null) {
                stack.push(node.getNext());
            }
            try {
                if (!node.isParallelNode()) {
                    // 串行节点
                    // 设定验证器名称
                    if (node.isEnabled()) {
                        Map<T, ValidationResult<T>> validationResult = executeNode(node, validationContext, dataWrappers, parallelValidatorsCancellationFlag, cancellationFlagIndex);
                        combineResult(combinedResult, validationResult);
                    }
                } else {
                    // 并行节点
                    if (executorService == null) {
                        throw new IllegalArgumentException("Validation `" + validationContext.getValidationName() + "` has parallel nodes but the executor service is null, " +
                                "please use other functions which support parallel validation");
                    }
                    List<ValidatorNode> pnodes = node.getParallelNodes();
                    CompletionService<Map<T, ValidationResult<T>>> completionService = new ExecutorCompletionService<>(executorService);
                    boolean parentCancellationFlag = validationContext.MY_CANCELLATION_FLAG.get() != null && validationContext.MY_CANCELLATION_FLAG.get().get();
                    List<AtomicBoolean> thisParallelValidatorsCancellationFlag = new ArrayList<>(pnodes.size());
                    for (int i = 0; i < pnodes.size(); i++) {
                        thisParallelValidatorsCancellationFlag.add(new AtomicBoolean(parentCancellationFlag));
                    }
                    for (int i = 0; i < pnodes.size(); i++) {
                        // 扫描出的并行节点，在开辟线程运作之前需要存储其他并行节点的YIELD_FLAG
                        final ValidatorNode thisNode = pnodes.get(i);
                        final int thisYieldIndex = i;
                        completionService.submit(() -> visitAndExecuteNode(validationContext, thisNode, dataWrappers, executorService, thisParallelValidatorsCancellationFlag, thisYieldIndex));
                    }
                    for (int i = 0; i < pnodes.size(); i++) {
                        Map<T, ValidationResult<T>> r = completionService.take().get();
                        combineResult(combinedResult, r);
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("Something wrong with user's validator `" + (node.getName() == null ? "Unknown" : node.getName()) + "`(" + (node.getValidator() == null ? "Unknown" : node.getValidator().getName()) + ")", e);
            }
        }
        return combinedResult;
    }

    private <T> Map<T, ValidationResult<T>> executeNode(ValidatorNode node, DefaultValidationContext validationContext, Set<DataWrapper<T>> dataWrappers, List<AtomicBoolean> parallelValidatorsCancellationFlag, int cancellationFlagIndex) {
        try {
            validationContext.VALIDATOR_NAME.set(node.getName());
            AbstractValidator<T> validator = ValidatorManager.getOrCreate(node.getValidator());
            // 设定验证器实例
            validationContext.VALIDATOR_INSTANCE.set(validator);
            // 重新设定validationContextHolder
            // 为什么要重新设定？因为在线程池等可重复利用线程的环境下InheritableThreadLocal可能会保留上次设定的值
            ValidationContextHolder.HOLDER.set(validationContext);
            // 设定cancellationFlag相关信息
            validationContext.MY_CANCELLATION_FLAG.set(cancellationFlagIndex == -1 ? new AtomicBoolean() : parallelValidatorsCancellationFlag.get(cancellationFlagIndex));
            List<AtomicBoolean> newParallelValidatorsCancellationFlag = new ArrayList<>();
            for (int i = 0; i < parallelValidatorsCancellationFlag.size(); i++) {
                if (i != cancellationFlagIndex) {
                    newParallelValidatorsCancellationFlag.add(parallelValidatorsCancellationFlag.get(i));
                }
            }
            validationContext.PARALLEL_VALIDATORS_CANCELLATION_FLAG.set(Collections.unmodifiableList(newParallelValidatorsCancellationFlag));
            validationContext.CANCELLATION_CHECKED.set(false);

            // 检测该验证器是否被全局（ValidationContext）忽略
            // 如果检测到忽略，则直接返回空Map，不再调用innerValidate
            if (validationContext.isIgnoreAllFollowingValidators()) {
                return Collections.emptyMap();
            }
            String validatorName = validationContext.getValidatorName();
            Set<String> globalIgnoredValidators = validationContext.getIgnoredValidators();
            for (String globalIgnoredValidator : globalIgnoredValidators) {
                if (equalsOrMatchRegexp(validatorName, globalIgnoredValidator)) {
                    return Collections.emptyMap();
                }
            }

            // 移除掉已设定的需要忽略的的DataWrapper
            for (Iterator<DataWrapper<T>> iterator = dataWrappers.iterator(); iterator.hasNext();) {
                DataWrapper<T> dataWrapper = iterator.next();
                if (dataWrapper.isIgnoreAllFollowingValidators()) {
                    // 如果设定了忽略接下来的所有验证器，那么直接将该数据移除
                    iterator.remove();
                }
                Set<String> ignoredValidators = dataWrapper.getIgnoredValidators();
                // 如果设定了忽略指定的验证器，那么也将该数据移除
                if (ignoredValidators != null) {
                    for (String ig : ignoredValidators) {
                        if (equalsOrMatchRegexp(validatorName, ig)) {
                            iterator.remove();
                            break;
                        }
                    }
                }
            }
            // 开始校验
            ValidationRecord<T> validationRecord = validator.innerValidate(dataWrappers, validationContext);
            // 返回结果
            return validationRecord.getValidationResults();
        } finally {
            // 记得清理ThreadLocal
            validationContext.VALIDATOR_NAME.remove();
            validationContext.VALIDATOR_INSTANCE.remove();
            validationContext.MY_CANCELLATION_FLAG.remove();
            validationContext.PARALLEL_VALIDATORS_CANCELLATION_FLAG.remove();
            validationContext.CANCELLATION_CHECKED.remove();
            // 遇到InheritableThreadLocal处理比较复杂，应当在主线程和所有继承了主线程的子线程中remove才可以达到清理效果
            // 每次在线程中使用均重新继承下，然后用完马上清理
            ValidationContextHolder.HOLDER.remove();
        }
    }

    private boolean equalsOrMatchRegexp(String validatorName, String exp) {
        return (
                // 正则表达式匹配
                // 要求正则表达式是包含在两个"/"之间的
                exp.startsWith("/")
                        && exp.endsWith("/")
                        && validatorName.matches(exp.substring(1, exp.length() - 1))
        )
        ||
        (
                // 直接匹配
                validatorName.equals(exp)
        );
    }

    private <T> void combineResult(Map<T, ValidationResult<T>> to, Map<T, ValidationResult<T>> from) {
        for (Map.Entry<T, ValidationResult<T>> en : from.entrySet()) {
            T key = en.getKey();
            ValidationResult<T> res = en.getValue();
            to.compute(key,  (k, v) -> {
                if (v == null) {
                    DefaultValidationResult<T> dvr = new DefaultValidationResult<>();
                    dvr.originalData = key;
                    dvr.validationName = res.getValidationName();
                    dvr.messages = copyThreadSafeSet(res.getMessages());
                    v= dvr;
                } else {
                    ((DefaultValidationResult<T>) v).messages.addAll(res.getMessages());
                }
                return v;
            });
        }
    }

    private <T> Set<T> copyThreadSafeSet(Set<T> from) {
        Map<T, Boolean> cmap = new ConcurrentHashMap<>(from.size(), 1);
        Set<T> newSet = Collections.newSetFromMap(cmap);
        newSet.addAll(from);
        return newSet;
    }

    @Override
    public void disableValidator(String validationName, String validatorName) {
        getValidation(validationName).disableValidator(validatorName);
    }

    @Override
    public void enableValidator(String validationName, String validatorName) {
        getValidation(validationName).enableValidator(validatorName);
    }

    @Override
    public Validation getValidation(String validationName) {
        Validation validation = registered.get(validationName);
        if (validation == null) {
            throw new IllegalStateException("validation `" + validationName + "` is not exist or not register yet");
        }
        return validation;
    }

    class ValidatorTask<T> extends RecursiveTask<Map<T, ValidationResult<T>>> {

        DefaultValidationContext validationContext;
        ValidatorNode node;
        Set<DataWrapper<T>> dataWrappers;
        List<AtomicBoolean> parallelValidatorsCancellationFlag;
        int cancellationFlagIndex;

        ValidatorTask(DefaultValidationContext validationContext, ValidatorNode node, Set<DataWrapper<T>> dataWrappers, List<AtomicBoolean> parallelValidatorsCancellationFlag, int cancellationFlagIndex) {
            this.validationContext = validationContext;
            this.node = node;
            this.dataWrappers = dataWrappers;
            this.parallelValidatorsCancellationFlag = parallelValidatorsCancellationFlag;
            this.cancellationFlagIndex = cancellationFlagIndex;
        }

        @Serial
        private static final long serialVersionUID = 1880078467256228544L;

        @Override
        protected Map<T, ValidationResult<T>> compute() {
            Map<T, ValidationResult<T>> res = new HashMap<>(16);
            while (node != null) {
                if (!node.isParallelNode()) {
                    combineResult(res, executeNode(node, validationContext, dataWrappers, parallelValidatorsCancellationFlag, cancellationFlagIndex));
                } else {
                    List<ValidatorNode> pnodes = node.getParallelNodes();
                    List<AtomicBoolean> thisParallelValidatorsCancellationFlag = new ArrayList<>(pnodes.size());
                    boolean parentCancellationFlag = validationContext.MY_CANCELLATION_FLAG.get() != null && validationContext.MY_CANCELLATION_FLAG.get().get();
                    for (int i = 0; i < pnodes.size(); i++) {
                        thisParallelValidatorsCancellationFlag.add(new AtomicBoolean(parentCancellationFlag));
                    }
                    Set<ValidatorTask<T>> subTasks = new HashSet<>(pnodes.size(), 1);
                    for (int i = 0; i < pnodes.size(); i++) {
                        subTasks.add(new ValidatorTask<>(validationContext, pnodes.get(i), dataWrappers, thisParallelValidatorsCancellationFlag, i));
                    }
                    invokeAll(subTasks);
                    Map<T, ValidationResult<T>> combinedResult = new HashMap<>(16);
                    subTasks.forEach(t -> {
                        Map<T, ValidationResult<T>> r = t.join();
                        combineResult(combinedResult, r);
                    });
                    combineResult(res, combinedResult);
                }
                node = node.getNext();
            }
            return res;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ValidatorTask<?> that = (ValidatorTask<?>) o;

            if (!Objects.equals(validationContext, that.validationContext)) {
                return false;
            }
            if (!Objects.equals(node, that.node)) {
                return false;
            }
            return Objects.equals(dataWrappers, that.dataWrappers);
        }

        @Override
        public int hashCode() {
            int result = validationContext != null ? validationContext.hashCode() : 0;
            result = 31 * result + (node != null ? node.hashCode() : 0);
            result = 31 * result + (dataWrappers != null ? dataWrappers.hashCode() : 0);
            return result;
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(10);
        CompletionService<Integer> cs = new ExecutorCompletionService<>(es);
        cs.submit(() -> {
            Thread.sleep(1000);
            return 1;
        });
        cs.submit(() -> {
            Thread.sleep(2000);
            return 2;
        });
        cs.submit(() -> {
            Thread.sleep(3000);
            return 3;
        });
        cs.submit(() -> {
            Thread.sleep(4000);
            return 4;
        });
        System.out.println(cs.take().get());
        System.out.println(cs.take().get());
        System.out.println(cs.take().get());
        System.out.println(cs.take().get());
        System.out.println(cs.take().get());
    }
}
