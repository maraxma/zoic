package com.mara.zoic.exchain.core;

import com.mara.zoic.exchain.core.ValidationMessage.MessageType;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import static com.mara.zoic.exchain.core.DefaultValidation.create;
import static com.mara.zoic.exchain.core.DefaultValidation.createParallel;

public class ValidatorTest {

    public static void main(String[] args) {
        DefaultValidationManager defaultValidationManager = new DefaultValidationManager();
        Validation validation1 = create("v1", NameValidator.class)
                .nextSerial(create("v2", AgeValidator.class))
                .nextSerial(create("v6", ScoreValidator4.class))
                .nextSerial(create("v3", ScoreValidator.class))
                .nextParallel(
                        create("p1", ScoreValidator5.class)
                                .nextParallel(
                                        create("v4", ScoreValidator2.class)
                                                .nextSerial(create("v9", ScoreValidator5.class)),
                                        create("v5", ScoreValidator3.class).nextSerial(create("v10", ScoreValidator5.class)),
                                        create("v7", ScoreValidator5.class)
                        ),
                        create("p2", ScoreValidator5.class).nextSerial(create("p2s1", ScoreValidator5.class))
                )
                .nextSerial(create("v8", ScoreValidator5.class));
        Validation validation2 = createParallel(
                create("v1", NameValidator.class),
                create("v2", AgeValidator.class),
                create("v3", ScoreValidator.class),
                create("v6", ScoreValidator4.class),
                create("v4", ScoreValidator2.class),
                create("v5", ScoreValidator3.class)
        );
        defaultValidationManager.register("validation1", validation1);
        defaultValidationManager.register("validation2", validation2);
        Bean bean = new Bean();
        bean.name = "xxxxxxxxxxxxxxxxx";
        bean.age = 21;
        bean.score = 59;

//        long startTime = System.currentTimeMillis();
//        Map<Bean, ValidationResult<Bean>> map = defaultValidationManager.validate("validation2", Collections.singleton(bean), Executors.newFixedThreadPool(10));
//        long endTime = System.currentTimeMillis();
//        System.out.println("Cost: " + (endTime - startTime));
//        System.out.println(map);
//        
//        validation2.updateValidator("v6", ScoreValidator5.class);

        System.out.println(validation1.toRoadMapString());

        long startTime2 = System.currentTimeMillis();
        Map<Bean, ValidationResult<Bean>> map2 = defaultValidationManager.validate("validation1", Collections.singleton(bean), ForkJoinPool.commonPool());
        long endTime2 = System.currentTimeMillis();
        System.out.println("Cost: " + (endTime2 - startTime2));
        System.out.println(map2);
//        
//        long startTime3 = System.currentTimeMillis();
//        Map<Bean, ValidationResult<Bean>> map3 = defaultValidationManager.validateForkJoin("validation2", Collections.singleton(bean), ForkJoinPool.commonPool());
//        long endTime3 = System.currentTimeMillis();
//        System.out.println("Cost: " + (endTime3 - startTime3));
//        System.out.println(map3);

        long startTime3 = System.currentTimeMillis();
        Map<Bean, ValidationResult<Bean>> map3 = defaultValidationManager.validateForkJoin("validation1", Collections.singleton(bean), ForkJoinPool.commonPool());
        long endTime3 = System.currentTimeMillis();
        System.out.println("Cost: " + (endTime3 - startTime3));
        System.out.println(map3);

        long startTime4 = System.currentTimeMillis();
        Map<Bean, ValidationResult<Bean>> map4 = defaultValidationManager.validate("validation1", Collections.singleton(bean), ForkJoinPool.commonPool());
        long endTime4 = System.currentTimeMillis();
        System.out.println("Cost: " + (endTime4 - startTime4));
        System.out.println(map4);
    }

    public static class Bean {
        public String name;
        public int age;
        public int score;
    }

    public static class NewBean {
        public String name;
        public int age;
        public int score;
    }

    public static class NameValidator extends AbstractValidator<Bean> {

        @Override
        protected void validate(Set<DataWrapper<Bean>> data, ValidationContext validationContext) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // System.out.println(Thread.currentThread().getName() + ": " + getClass() + ": " + ValidationContexts.autoDetect().getValidatorName());
            // validationContext.put("Mara", DataProviders.immediate(true));
            for (DataWrapper<Bean> dataWrapper : data) {
                if (dataWrapper.getOriginalData().name.length() > 10) {
                    report(dataWrapper.getOriginalData(), MessageType.ERROR, "name's length > 10", "E0001", null, null);
                    // dataWrapper.setIgnoreAllFollowingValidators(true);
                }
            }
        }
    }

    public static class AgeValidator extends AbstractValidator<Bean> {

        @Override
        protected void validate(Set<DataWrapper<Bean>> data, ValidationContext validationContext) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // System.out.println(Thread.currentThread().getName() + ": " + getClass() + ": " + ValidationContexts.autoDetect().getValidatorName());
            for (DataWrapper<Bean> dataWrapper : data) {
                if (dataWrapper.getOriginalData().age > 10) {
                    report(dataWrapper.getOriginalData(), MessageType.ERROR, "age > 10", "E0002", null, null);
                }
                // System.out.println(getClass() + " Future Validators: " + dataWrapper.getFutureValidators());
            }
        }

    }

    public static class ScoreValidator extends AbstractValidator<Bean> {

        @Override
        protected void validate(Set<DataWrapper<Bean>> data, ValidationContext validationContext) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // System.out.println(Thread.currentThread().getName() + ": " + getClass() + ": " + ValidationContexts.autoDetect().getValidatorName());
            for (DataWrapper<Bean> dataWrapper : data) {
                if (dataWrapper.getOriginalData().score < 60) {
                    report(dataWrapper.getOriginalData(), MessageType.ERROR, "score < 60", "E0003", null, null);
                }
            }
        }

    }

    public static class ScoreValidator2 extends AbstractValidator<Bean> {

        @Override
        protected void validate(Set<DataWrapper<Bean>> data, ValidationContext validationContext) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // System.out.println(Thread.currentThread().getName() + ": " + getClass() + ": " + ValidationContexts.autoDetect().getValidatorName());
            checkCancellation();
            for (DataWrapper<Bean> dataWrapper : data) {
                System.out.println(getClass() + " Future Validators: " + dataWrapper.getFutureValidators());
                if (dataWrapper.getOriginalData().score < 80) {
                    cancelOtherParallelValidators();
                    report(dataWrapper.getOriginalData(), MessageType.WARNING, "score < 80", "E0004", null, null);
                }
            }
        }
    }


    public static class ScoreValidator3 extends AbstractValidator<Bean> {

        @Override
        protected void validate(Set<DataWrapper<Bean>> data, ValidationContext validationContext) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // System.out.println(Thread.currentThread().getName() + ": " + getClass() + ": " + ValidationContexts.autoDetect().getValidatorName());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            checkCancellation();
            for (DataWrapper<Bean> dataWrapper : data) {
                // System.out.println(dataWrapper.getValidatedInfo());
                if (dataWrapper.getOriginalData().score < 90) {
                    cancelOtherParallelValidators();
                    report(dataWrapper.getOriginalData(), MessageType.WARNING, "score < 90", "E0005", null, null);
                }
            }
        }
    }

    public static class ScoreValidator4 extends AbstractValidator<Bean> {

        @Override
        protected void validate(Set<DataWrapper<Bean>> data, ValidationContext validationContext) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // System.out.println(Thread.currentThread().getName() + ": " + getClass() + ": " + ValidationContexts.autoDetect().getValidatorName());
            for (DataWrapper<Bean> dataWrapper : data) {
                if (dataWrapper.getOriginalData().score < 100) {
                    Thread t = new Thread(() -> {
                        dataWrapper.reportDebug("score < 100");
                        validationContext.reportInfo(dataWrapper.getOriginalData(), "score < 100(2)");
                        // ValidationContexts.autoDetect().reportInfo(dataWrapper.getOriginalData(), "score < 100(3)");
                    });
                    t.start();
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // report(dataWrapper.getOriginalData(), MessageType.WARNING, "score < 100", "E0006", null, null);
                }
            }
            // if (validationContext.get("Mara").get().equals(Boolean.TRUE)) {
            //     System.out.println("Passed");
            // }
        }
    }

    public static class ScoreValidator5 extends AbstractValidator<Bean> {

        @Override
        protected void validate(Set<DataWrapper<Bean>> data, ValidationContext validationContext) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // System.out.println(Thread.currentThread().getName() + ": " + getClass() + ": " + ValidationContexts.autoDetect().getValidatorName());
            for (DataWrapper<Bean> dataWrapper : data) {
                if (dataWrapper.getOriginalData().age < 110) {
                    dataWrapper.reportDebug("score < 110");
                    // report(dataWrapper.getOriginalData(), MessageType.WARNING, "score < 100", "E0006", null, null);
                }
            }
        }
    }
}
