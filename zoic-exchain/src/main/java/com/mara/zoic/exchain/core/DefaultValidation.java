package com.mara.zoic.exchain.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 默认验证实现。
 * <p>注意：此实现不是线程安全的。</p>
 * @author Mara.X.Ma & Jieyi Chen
 */
public class DefaultValidation implements Validation {

    private ValidatorNode root;

    /**
     * 始终指向验证器链的最后一个节点
     */
    private ValidatorNode cur;

    /**
     * 并行容器计数器，用于给并行容器命名（编号）
     */
    private final AtomicInteger parallelContainerCounter = new AtomicInteger(0);
    private final ConcurrentHashMap<String, ValidatorNode> validators = new ConcurrentHashMap<>(16);
    
    private static final String PARALLEL_NODE_NAME = "parallelContainerNode";

    /**
     * 创建一个以串行节点开头的Validation对象
     * @param validatorName 验证器名称
     * @param validatorClass 验证器类
     */
    DefaultValidation(String validatorName, Class<? extends AbstractValidator<?>> validatorClass) {
        DefaultValidatorNode node = new DefaultValidatorNode();
        node.validator = validatorClass;
        node.name = validatorName;
        root = node;
        cur = node;
        validators.put(validatorName, node);
    }
    
    /**
     * 创建一个以并行节点开头的Validation对象
     * @param validations Validation对象
     */
    DefaultValidation(Validation... validations) {
        final DefaultValidatorNode node = createParallelNode(validations);
        root = node;
        cur = node;
    }

    /**
     * 创建一个包含串行节点开头的Validation对象。
     * @param validatorName 验证器名称
     * @param validatorClass 验证器类
     * @return Validation对象
     */
    public static DefaultValidation create(String validatorName, Class<? extends AbstractValidator<?>> validatorClass) {
        return new DefaultValidation(validatorName, validatorClass);
    }
    
    /**
     * 创建一个包含并行节点开头的Validation对象。
     * @param validations Validation对象
     * @return Validation对象
     */
    public static DefaultValidation createParallel(Validation... validations) {
        return new DefaultValidation(validations);
    }

    @Override
    public Validation nextSerial(Validation validation) {
        combineValidatorMap(validation);
        final DefaultValidatorNode curNode = (DefaultValidatorNode) cur;
        ValidatorNode incomingValidatorNode = validation.getRootValidatorNode();
        DefaultValidatorNode newNode = ((DefaultValidatorNode) incomingValidatorNode);
        newNode.prev = cur;
        curNode.next = incomingValidatorNode;
        setContainerForValidation(validation, curNode.container);
        // cur永远是最后一个节点
        cur = findLastNode(incomingValidatorNode);
        return this;
    }

    private void combineValidatorMap(Validation validation) {
        Map<String, ValidatorNode> incoming = validation.getValidators();
        incoming.forEach((k, v) -> validators.compute(k, (key, value) -> {
            if (value != null) {
                throw new IllegalArgumentException("Duplicate validator name: " + key);
            }
            return v;
        }));
    }

    @Override
    public Validation nextParallel(Validation... validations) {
        if (validations.length == 1) {
            // 如果添加的是一个，那么“并行不成立”，此时需要将roadmap退化为串行
            nextSerial(validations[0]);
            return this;
        }
        final DefaultValidatorNode node = createParallelNode(validations);
        DefaultValidatorNode curNode = ((DefaultValidatorNode) cur);
        curNode.next = node;
        node.container = curNode.container;
        for (Validation validation : validations) {
            setContainerForValidation(validation, node);
        }
        node.prev = cur;
        node.name = PARALLEL_NODE_NAME + parallelContainerCounter.getAndIncrement();
        cur = node;
        return this;
    }

    @Override
    public void disableValidator(String validatorName) {
        ValidatorNode node = validators.get(validatorName);
        if (node != null) {
            ((DefaultValidatorNode) node).enabled = false;
            // 同时将已经存在的实例移除
            ValidatorManager.remove(node.getValidator());
        }
    }

    @Override
    public void enableValidator(String validatorName) {
        ValidatorNode node = validators.get(validatorName);
        if (node != null) {
            ((DefaultValidatorNode) node).enabled = true;
        }
    }

    @Override
    public void removeValidator(String validatorName) {
        ValidatorNode validatorNode = validators.get(validatorName);
        remove(validatorNode);
        // 同时将已经存在的实例移除
        ValidatorManager.remove(validatorNode.getValidator());
    }

    private void remove(ValidatorNode validatorNode) {
        if (validatorNode != null) {
            ValidatorNode prev = validatorNode.getPrev();
            ValidatorNode next = validatorNode.getNext();
            if (next != null) {
                ((DefaultValidatorNode) next).prev = prev;
                if (next.isParallelNode()) {
                    // 如果next是并行节点，那么并行节点List中包含的节点的prev都应该被重新设定
                    for (ValidatorNode vn : next.getParallelNodes()) {
                        ((DefaultValidatorNode) vn).prev = prev;
                    }
                }
            }
            ValidatorNode container = validatorNode.getContainer();
            if (container != null) {
                // 无论如何都要remove
                int removedIndex = ((DefaultValidatorNode) container).parallelNodes.indexOf(validatorNode);
                ((DefaultValidatorNode) container).parallelNodes.remove(validatorNode);
                if (next != null) {
                    // 如果next存在，那么在remove后将next放入
                    ((DefaultValidatorNode) container).parallelNodes.add(removedIndex, next);
                    // 还需要更新next的container
                    ((DefaultValidatorNode) next).container = container;
                }
                // 如果在移除后本节点只包含一个节点，那么也应当移除本容器，退化为直接的串行连接
                if (((DefaultValidatorNode) container).parallelNodes.size() == 1) {
                    ValidatorNode left = ((DefaultValidatorNode) container).parallelNodes.get(0);
                    if (prev != null) {
                        ((DefaultValidatorNode) prev).next = left;
                    }
                    // container设定为null
                    ((DefaultValidatorNode) left).container = null;
                    // 还要连接next
                    ValidatorNode lastNode = findLastNode(left);
                    ((DefaultValidatorNode) lastNode).next = container.getNext();
                    // container.next.prev需要设定
                    if (container.getNext() != null) {
                        ((DefaultValidatorNode) container.getNext()).prev = lastNode;
                    }
                }
                // 如果在移除掉节点后本容器节点不存在任何节点了，那么本容器节点也应该一并移除
                else if (((DefaultValidatorNode) container).parallelNodes.isEmpty()) {
                    remove(container);
                }

                // 如果本身位于container中，那么prev.next不需要更改
            } else {
                if (prev != null) {
                    ((DefaultValidatorNode) prev).next = next;
                }
            }
        }
    }

    @Override
    public void updateValidator(String validatorName, Class<? extends AbstractValidator<?>> validatorClass) {
        ValidatorNode validatorNode = validators.get(validatorName);
        if (validatorNode != null) {
            ((DefaultValidatorNode) validatorNode).validator = validatorClass;
            // 同时将已经存在的实例移除
            ValidatorManager.remove(validatorNode.getValidator());
        }
    }

    @Override
    public void addSerialFirst(Validation validation) {
        combineValidatorMap(validation);
        ValidatorNode incomingValidatorNode = validation.getRootValidatorNode();
        ValidatorNode lastNode = findLastNode(incomingValidatorNode);
        ((DefaultValidatorNode) lastNode).next = root;
        ((DefaultValidatorNode) root).prev = lastNode;
        setContainerForValidation(validation, root.getContainer());
        root = incomingValidatorNode;
    }

    @Override
    public void addSerialLast(Validation validation) {
        nextSerial(validation);
    }

    @Override
    public void addSerialBefore(String beforeValidatorName, Validation validation) {
        ValidatorNode beforeNode = validators.get(beforeValidatorName);
        if (beforeNode == null) {
            throw new IllegalArgumentException("No such validator: " + beforeValidatorName);
        }
        combineValidatorMap(validation);
        DefaultValidatorNode incomingNode = (DefaultValidatorNode) validation.getRootValidatorNode();
        DefaultValidatorNode incomingLastNode = (DefaultValidatorNode) findLastNode(incomingNode);
        DefaultValidatorNode prev = (DefaultValidatorNode) beforeNode.getPrev();
        DefaultValidatorNode next = (DefaultValidatorNode) beforeNode;
        ValidatorNode container = beforeNode.getContainer();
        if (container != null) {
            // 如果是并行节点中的节点，那么直接附加到此节点之前
            int removeIndex = ((DefaultValidatorNode) container).parallelNodes.indexOf(beforeNode);
            ((DefaultValidatorNode) container).parallelNodes.remove(beforeNode);
            ((DefaultValidatorNode) container).parallelNodes.add(removeIndex, incomingNode);
        }
        incomingNode.prev = prev;
        if (prev != null) {
            prev.next = incomingNode;
        }
        incomingLastNode.next = next;
        next.prev = incomingLastNode;

        // 注意before类型的add可能需要改变root
        if (beforeNode == root) {
            root = incomingNode;
        }

        // 设定container
        setContainerForValidation(validation, next.container);
    }

    @Override
    public void addSerialAfter(String afterValidatorName, Validation validation) {
        ValidatorNode afterNode = validators.get(afterValidatorName);
        if (afterNode == null) {
            throw new IllegalArgumentException("No such validator: " + afterValidatorName);
        }
        combineValidatorMap(validation);
        DefaultValidatorNode incomingNode = (DefaultValidatorNode) validation.getRootValidatorNode();
        DefaultValidatorNode incomingLastNode = (DefaultValidatorNode) findLastNode(incomingNode);
        DefaultValidatorNode prev = (DefaultValidatorNode) afterNode;
        DefaultValidatorNode next = (DefaultValidatorNode) ((DefaultValidatorNode) afterNode).next;

        incomingNode.prev = prev;
        incomingLastNode.next = next;
        prev.next = incomingNode;
        if (next != null) {
            next.prev = incomingLastNode;
            if (next.isParallelNode()) {
                next.parallelNodes.forEach(e -> ((DefaultValidatorNode) e).prev = incomingLastNode);
            }
        }
        // 注意，after类型的add方法可能会影响cur
        if (afterNode == cur) {
            // 如果被选定的节点就是尾节点，那么cur要改变为添加到的节点
            cur = incomingLastNode;
        }

        setContainerForValidation(validation, prev.container);
    }

    @Override
    public void addParallelFirst(Validation... validations) {
        if (validations.length == 1) {
            // 如果添加的是一个，那么“并行不成立”，此时需要将roadmap退化为串行
            addSerialFirst(validations[0]);
            return;
        }
        final DefaultValidatorNode node = createParallelNode(validations);
        node.next = root;
        ((DefaultValidatorNode ) root).prev = node;
        node.container = ((DefaultValidatorNode) root).container;
        for (Validation validation : validations) {
            setContainerForValidation(validation, node);
        }
        root = node;
    }

    private DefaultValidatorNode createParallelNode(Validation... validations) {
        final DefaultValidatorNode node = new DefaultValidatorNode();
        node.name = PARALLEL_NODE_NAME + parallelContainerCounter.getAndIncrement();
        node.parallelNodes =
                Arrays.stream(validations).peek(e -> {
                    combineValidatorMap(e);
                    DefaultValidatorNode rootValidatorNode = (DefaultValidatorNode) e.getRootValidatorNode();
                    rootValidatorNode.prev = cur;
                    rootValidatorNode.container = node;
                    rootValidatorNode.isParallelFirstNode = true;
                }).map(Validation::getRootValidatorNode).collect(Collectors.toList());
        return node;
    }

    @Override
    public void addParallelLast(Validation... validations) {
        nextParallel(validations);
    }

    @Override
    public void addParallelBefore(String beforeValidatorName, Validation... validations) {
        DefaultValidatorNode beforeNode = (DefaultValidatorNode) validators.get(beforeValidatorName);
        if (beforeNode == null) {
            throw new IllegalArgumentException("No such validator: " + beforeValidatorName);
        }
        if (validations.length == 1) {
            // 如果添加的是一个，那么“并行不成立”，此时需要将roadmap退化为串行
            addSerialBefore(beforeValidatorName, validations[0]);
            return;
        }
        final DefaultValidatorNode node = createParallelNode(validations);
        DefaultValidatorNode next = beforeNode;
        DefaultValidatorNode prev = (DefaultValidatorNode) beforeNode.prev;
        ValidatorNode container = next.getContainer();
        if (container != null) {
            // 如果是并行节点中的节点，那么直接附加到此节点之前
            int removeIndex = ((DefaultValidatorNode) container).parallelNodes.indexOf(beforeNode);
            ((DefaultValidatorNode) container).parallelNodes.remove(beforeNode);
            ((DefaultValidatorNode) container).parallelNodes.add(removeIndex, node);
        }
        node.prev = prev;
        if (prev != null) {
            prev.next = node;
        }
        node.next = next;
        next.prev = node;

        // 注意before类型的add可能需要改变root
        if (beforeNode == root) {
            root = node;
        }

        node.container = next.container;
        for (Validation validation : validations) {
            setContainerForValidation(validation, node);
        }
    }

    @Override
    public void addParallelAfter(String afterValidatorName, Validation... validations) {
        DefaultValidatorNode afterNode = (DefaultValidatorNode) validators.get(afterValidatorName);
        if (afterNode == null) {
            throw new IllegalArgumentException("No such validator: " + afterValidatorName);
        }
        if (validations.length == 1) {
            // 如果添加的是一个，那么“并行不成立”，此时需要将roadmap退化为串行
            addSerialAfter(afterValidatorName, validations[0]);
            return;
        }
        final DefaultValidatorNode node = createParallelNode(validations);
        DefaultValidatorNode prev = afterNode;
        DefaultValidatorNode next = (DefaultValidatorNode) afterNode.next;

        node.prev = prev;
        node.next = next;
        prev.next = node;
        if (next != null) {
            next.prev = node;
            if (next.isParallelNode()) {
                next.parallelNodes.forEach(e -> ((DefaultValidatorNode) e).prev = node);
            }
        }

        // 注意，after类型的add方法可能会影响cur
        if (afterNode == cur) {
            // 如果被选定的节点就是尾节点，那么cur要改变为添加到的节点
            cur = node;
        }

        node.container = afterNode.container;
        for (Validation validation : validations) {
            setContainerForValidation(validation, node);
        }
    }

    @Override
    public Map<String, ValidatorNode> getValidators() {
        return Collections.unmodifiableMap(validators);
    }

    @Override
    public String toRoadMapString() {
        ValidatorNode root = getRootValidatorNode();
        StringBuilder sb = new StringBuilder("START\n");
        appendRoadMapString("", root, sb);
        return sb.append("\n").append("END").toString();
    }

    private void appendRoadMapString(String prefix, ValidatorNode node, StringBuilder sb) {
        ValidatorNode n = node;
        while (n != null) {
            if (n.isParallelNode()) {
                List<ValidatorNode> parallelNodes = n.getParallelNodes();
                if (n.getPrev() != null && n.getPrev().isParallelNode()) {
                    sb.append("\n").append(" -->");
                }
                for (ValidatorNode parallelNode : parallelNodes) {
                    appendRoadMapString(prefix + "    ", parallelNode, sb);
                }
            } else {
                if (!n.isParallelFirstNode()) {
                    if (n.getPrev() != null && n.getPrev().isParallelNode()) {
                        sb.append("\n");
                    }
                    sb.append(" --> ").append(n);
                } else {
                    sb.append("\n").append(prefix).append("|--- ").append(n);
                }
                if (!n.isEnabled()) {
                    sb.append("[DISABLED]");
                }
            }
            n = n.getNext();
        }
    }

    @Override
    public ValidatorNode getRootValidatorNode() {
        return root;
    }

    private void setContainerForValidation(Validation validation, ValidatorNode container) {
        ValidatorNode currNode = validation.getRootValidatorNode();
        while (currNode != null) {
            ((DefaultValidatorNode) currNode).container = container;
            currNode = currNode.getNext();
        }
    }

    /**
     * 验证器节点。
     * <p>一个验证器节点可以是一个简单的串行节点，也可以是一个包含多个节点的并行节点。
     * @author Mara.X.Ma
     * @since 2022-03-27
     */
    static class DefaultValidatorNode implements ValidatorNode {
        /**
         * 此节点连接的下一个节点
         */
        ValidatorNode next;

        /**
         * 此节点的上一个节点
         */
        ValidatorNode prev;

        /**
         * 此节点的名称。
         * <p>一般来说，作为串行节点，此名称即是验证器的名称；作为并行节点，此名称代表的是整个并行组。
         */
        String name;

        /**
         * 此节点作为串行节点所包含的验证器
         */
        Class<? extends AbstractValidator<?>> validator;

        /**
         * 此节点作为并行节点所包含的一个或者多个其他节点
         */
        List<ValidatorNode> parallelNodes;

        /**
         * 如果此节点是并行节点中的节点，那么存在一个父容器
         */
        ValidatorNode container;

        /**
         * 此验证器节点是否是启用的，默认是启用的
         * <p>验证器节点被禁用只影响本节点的验证器，后续的验证器不受影响。
         */
        boolean enabled = true;

        boolean isParallelFirstNode = false;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<ValidatorNode> getParallelNodes() {
            return parallelNodes == null ? null : Collections.unmodifiableList(parallelNodes);
        }

        @Override
        public Class<? extends AbstractValidator<?>> getValidator() {
            return validator;
        }

        @Override
        public ValidatorNode getNext() {
            return next;
        }

        @Override
        public ValidatorNode getPrev() {
            return prev;
        }

        @Override
        public ValidatorNode getContainer() {
            return container;
        }

        @Override
        public boolean isParallelFirstNode() {
            return isParallelFirstNode;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public String toString() {
            return (name == null ? "--" : name) + ": " + (validator == null ? "--" : validator.getName());
        }
    }

    private ValidatorNode findLastNode(ValidatorNode node) {
        ValidatorNode n = node;
        while (n.getNext() != null) {
            n = n.getNext();
        }
        return n;
    }

    public static void main(String[] args) {
        Validation validation = create("v1", Validator1.class)
                .nextSerial(create("v2", Validator2.class))
                .nextSerial(create("v3", Validator3.class))
                .nextParallel(
                        create("v4", Validator4.class)
                                .nextSerial(create("v5", Validator5.class)),
                        create("v6", Validator6.class)
                )
                .nextSerial(create("v7", Validator7.class))
                .nextSerial(create("v8", Validator8.class))
                .nextParallel(
                        create("v9", Validator9.class)
                                .nextParallel(
                                        create("v10", Validator10.class)));
        System.out.println(validation.toRoadMapString());
        validation.removeValidator("v2");
        System.out.println(validation.toRoadMapString());
        validation.removeValidator("v3");
        System.out.println(validation.toRoadMapString());
        validation.removeValidator("v4");
        System.out.println(validation.toRoadMapString());
        validation.removeValidator("v5");
        System.out.println(validation.toRoadMapString());
        validation.removeValidator("v6");
        System.out.println(validation.toRoadMapString());
        validation.removeValidator("v9");
        System.out.println(validation.toRoadMapString());

        validation.addSerialFirst(create("v0", Validator1.class));
        System.out.println(validation.toRoadMapString());

        validation.addSerialBefore("v1", create("v-0", Validator1.class).nextSerial(create("v-1", Validator1.class)));
        System.out.println(validation.toRoadMapString());

        validation.addSerialBefore("v1", create("vd", Validator1.class));
        System.out.println(validation.toRoadMapString());

        validation.addSerialAfter("v0", create("v-2", Validator1.class).nextSerial(create("v-3", Validator1.class)));
        System.out.println(validation.toRoadMapString());

        validation.addSerialAfter("v-2", create("vx", Validator1.class));
        System.out.println(validation.toRoadMapString());

        validation.addParallelFirst(
                create("p1", Validator1.class)
                // create("p2", Validator1.class)
        );
        System.out.println(validation.toRoadMapString());

        validation.addSerialAfter("v10", create("vdd", Validator1.class).nextSerial(create("vdd2", Validator1.class)));
        System.out.println(validation.toRoadMapString());

        validation.nextSerial(create("vdd3", Validator1.class));
        System.out.println(validation.toRoadMapString());

        validation.addParallelAfter("vdd3", create("vdd4", Validator1.class), create("vdd5", Validator1.class));
        System.out.println(validation.toRoadMapString());

        validation.addParallelAfter("vdd2", create("vdd6", Validator1.class), create("vdd7", Validator1.class));
        System.out.println(validation.toRoadMapString());

        validation.nextParallel(create("vdd9", Validator1.class), create("vdd10", Validator1.class));
        System.out.println(validation.toRoadMapString());

        validation.addParallelBefore("v0", create("vdd11", Validator1.class), create("vdd12", Validator1.class));
        System.out.println(validation.toRoadMapString());

        validation.addParallelBefore("p1", create("vdd13", Validator1.class), create("vdd14", Validator1.class));
        System.out.println(validation.toRoadMapString());
    }

    static class Validator1 extends AbstractValidator<Object> {}
    static class Validator2 extends AbstractValidator<Object> {}
    static class Validator3 extends AbstractValidator<Object> {}
    static class Validator4 extends AbstractValidator<Object> {}
    static class Validator5 extends AbstractValidator<Object> {}
    static class Validator6 extends AbstractValidator<Object> {}
    static class Validator7 extends AbstractValidator<Object> {}
    static class Validator8 extends AbstractValidator<Object> {}
    static class Validator9 extends AbstractValidator<Object> {}
    static class Validator10 extends AbstractValidator<Object> {}

}
