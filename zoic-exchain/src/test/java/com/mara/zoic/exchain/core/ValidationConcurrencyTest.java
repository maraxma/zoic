package com.mara.zoic.exchain.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.mara.zoic.exchain.core.ValidatorTest.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.*;

import static com.mara.zoic.exchain.core.DefaultValidation.create;

public class ValidationConcurrencyTest {

    static ValidationManager validationManager;
    static ExecutorService executorService;

    @BeforeAll
    static void setup() {
        validationManager = new DefaultValidationManager();
        Validation validation = create("v1", NameValidator.class)
                .nextSerial(create("v2", AgeValidator.class))
                .nextSerial(create("v3", ScoreValidator.class))
                .nextParallel(
                    create("v4", ScoreValidator2.class),
                    create("v5", ScoreValidator3.class)
                    )
                .nextSerial(create("v6", ScoreValidator4.class));
        validationManager.register("validation1", validation);

        executorService = Executors.newFixedThreadPool(100);
    }

    @Test
    @DisplayName("测试在并发环境下的数据的正确性")
    void testConcurrency() throws InterruptedException {
        Bean bean = new Bean();
        Bean bean2 = new Bean();
        bean.name = "Mara";
        bean2.name = "Mara.X.Ma Mosin XXXXX";
        bean.age = 21;
        bean2.age = 50;
        bean.score = 100;
        bean2.score = 59;
        for (int i = 0; i < 1000; i++) {
            new Thread(() -> {
                try {
                    Map<Bean, ValidationResult<Bean>> validationResultMap = validationManager.validate("validation1", new HashSet<>(Arrays.asList(bean, bean2)), executorService);
                    System.out.println(validationResultMap);
                    validationResultMap.entrySet().stream().findFirst().get().getValue().when(r -> !r.getMessages().isEmpty()).thenThrowRuntimeException("xxx");
                    Map<Bean, ValidationResult<Bean>> validationResultMap2 = validationManager.validateForkJoin("validation1", new HashSet<>(Arrays.asList(bean, bean2)), ForkJoinPool.commonPool());
                    System.out.println(validationResultMap2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        Thread.sleep(3000);
    }

    @Test
    @DisplayName("测试在线程池环境下的稳定性（测试数据的正确性，因为很多数据是通过ThreadLocal传输的）")
    void testThreadPool() throws InterruptedException {
        Bean bean = new Bean();
        Bean bean2 = new Bean();
        bean.name = "Mara";
        bean2.name = "Mara.X.Ma Mosin XXXXX";
        bean.age = 21;
        bean2.age = 50;
        bean.score = 100;
        bean2.score = 59;
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(100, 100, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>(3000));
        for (int i = 0; i < 2000; i++) {
            if (i == 500) {
                validationManager.disableValidator("validation1", "v2");
            }
            threadPoolExecutor.submit(() -> {
                try {
                    Map<Bean, ValidationResult<Bean>> validationResultMap = validationManager.validate("validation1", new HashSet<>(Arrays.asList(bean, bean2)), executorService);
                    System.out.println(validationResultMap);
                    //Map<Bean, ValidationResult<Bean>> validationResultMap2 = validationManager.validateForkJoin("validation1", new HashSet<>(Arrays.asList(bean, bean2)), ForkJoinPool.commonPool());
                    //System.out.println(validationResultMap2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        threadPoolExecutor.shutdown();
        threadPoolExecutor.awaitTermination(5000, TimeUnit.SECONDS);
    }
}
