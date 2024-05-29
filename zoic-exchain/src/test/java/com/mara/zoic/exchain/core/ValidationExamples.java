package com.mara.zoic.exchain.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

public class ValidationExamples {

    @Test
    @DisplayName("演示一个最简单的校验")
    void simpleValidationTest() {
        // 定义验证链
        // 验证链代表了一个完整的校验，其是可以在运行时改变的
        DefaultValidation validation = DefaultValidation.create("validator1", MyValidator.class);

        // 新建验证管理器
        // 验证管理器负责管理所有的验证链，验证链必须注册到验证管理器才可以使用
        // 验证管理器还可以在运行时禁用或者移除某个验证链里的验证器
        DefaultValidationManager defaultValidationManager = new DefaultValidationManager();

        // 向验证管理器注册验证链
        defaultValidationManager.register("MyValidation", validation);

        // 开始校验，并获得结果
        // 必须由验证管理器开启一段校验
        // 验证链符合单一职责原则，只负责验证器的关系组织，具体实施校验需要验证管理器发起
        Map<User, ValidationResult<User>> resultMap = defaultValidationManager.validate("MyValidation", Set.of(new User(1, "Mara.X.Ma")));

        // 查看结果（如果没有在校验过程中汇报消息，那么这里不会存在结果）
        // 验证结果包含很多信息，包括错误信息和堆栈信息以及原始数据
        resultMap.forEach((user, result) -> {
            System.out.println(result.toString());
        });
    }

    // 定义实体
    record User(Integer id, String name) { }
    // 定义验证器
    public static class MyValidator extends AbstractValidator<User> {
        @Override
        protected void validate(Set<DataWrapper<ValidationExamples.User>> data, ValidationContext validationContext) {
            System.out.println("已校验");
        }
    }
}
