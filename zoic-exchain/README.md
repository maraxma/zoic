# zoic-exchain

## 简介

zoic-exchain是一个zoic工具包下的链式处理编排框架，主要用于对批量化的数据进行链式处理或者校验。

在业务开发中，经常会遇到对数据的链式校验（或者处理），这不仅仅是对单个数据字段的规则校验（空、非空、长度、类型等），还包含一系列的业务校验，如业务限制、数据筛选、数据清洗等。
传统的做法是直接在业务代码中通过工具类或者调用一些服务的方法，然而在过程较多的情况下这些代码将无法得到有效编排和管理。

举个例子：现在某电商平台针对用户提交的商品上架请求需要做很多的校验以确认该商品是否可以正常上架。商品的校验过程需要查询很多服务，并且有可能针对不同国家地区采取不同的校验手段。
具体一点说就是整个校验流程前后相关，前面的流程结果可能影响后面的流程。还有就是这期间有可能需要查询缓存，还需要维护整个流程的上下文。我们得拿出有效的手段来处理这些棘手的问题。
此框架就是为此而生。

事实上Java在2009年就有了自己的Validation标准（JSR303/JSR349/JSR380），但是这些校验标准是针对Java Bean的，并且在第三方的实现中大都是通过注解来驱动的，并且对“流程化”的校验支持较弱。

zoic-exchain是对“业务”和“流程化”的校验的实现，在实际的项目中，仍然可以和Java Bean Validation结合使用。

如果你有如下的需求，那么exchain将会非常适合你：

- 项目需要对传入的复杂数据进行校验；
- 校验并不单纯是诸如 @NotNull/@Range/@Size 等校验，而是涉及到复杂的联合校验、查表校验等；
- 校验是一个完整的流程，程序需要输出所有有关该数据的校验结果，而不是遇到错误即停止校验；
- 校验器需要重复使用，同一个校验器需要在多个流程中使用，存在多个校验流程，每个校验流程包含不同的校验拓扑图；
- 各个校验之间需要相互制约，比如校验了name那么就必须校验address，校验了age就不必校验score等情况；
- 校验流程之间需要并行校验以提高效率，且，并行校验之间相互制约，任意校验通过即为通过；
- 校验过程中产生的数据可以传递，产生的查询的结果可以预定义和懒加载；
- 校验流程中需要查表或者通过IO等耗时操作获得数据，获得的数据需要缓存并在整个校验链中共享，校验完成后回收此数据；
- 校验流程在运行时需要添加、删除或者更新某个校验器。

#### 使用方式

1. 引入依赖

2. 构建Validator

Validator是校验的核心内容，只有通过Validator才可以校验数据，和Java Bean Validation不同的是，exchain并不提供默认的校验方式，所有校验的内容必须自己完成，exchain只负责帮助你组织和正确调用它们。

继承AbstractValidator即可实现自己的Validator。重写validate方法实现校验逻辑（validate方法是可选的，如果你想创建一个空操作的校验器，继承AbstractValidator即可）。

```java
public static class NameValidator extends AbstractValidator<Bean> {
    @Override
    protected void validate(Set<DataWrapper<Bean>> data, ValidationContext validationContext) {
        for (DataWrapper<Bean> dataWrapper : data) {
            if (dataWrapper.getOriginalData().name.length() > 10) {
                report(dataWrapper.getOriginalData(), MessageType.ERROR, "name's length > 10", "E0001", null, null);
            }
        }
    }
}
```

3. 构建Validation

Validation是一个或者多个Validator的组合，它们之间或按照顺序方式，或按照并行方式构建，一个Validation才是一个完整的校验流程。

Validator之间可以随意组合，并且必须命名，同一个类型的Validator可以反复使用。

```java
import static org.mosin.bocus.core.vd.DefaultValidation.*;

// 如下通过API创建一个完整的Validation
// 如同JSON结构一样，节点并不是单纯的Node，而是可以嵌套另一整条流程
// create方法帮助创建含有一个节点的Validation，Validation组织起来就是一整个大的Valiation
// create方法中的第一个参数是该节点包含的验证器在本流程中的名称，一个流程中不能出现相同名称的节点，但是节点所使用的的class是可以重复的
Validation validation1 = create("v1", NameValidator.class)
    .nextSerial(create("v2", AgeValidator.class))
    .nextSerial(create("v6", ScoreValidator4.class))
    .nextSerial(create("v3", ScoreValidator.class))
    .nextParallel(
            create("v4", ScoreValidator2.class),
            create("v5", ScoreValidator3.class)
    );
```

如上的代码创建了一个Validation，它的RoadMap如下：

```
                                     |--> v4 -->|
Start --> v1 --> v2 --> v6 --> v3 -->|          |--> End
                                     |--> v5 -->|
上图中的v4和v5是可以并行校验的。其他则是串行节点。串行节点只有当上一个节点中的校验代码运行结束才可以进入下一个节点；
并行节点上的节点（节点链）是可以在不同的线程中执行的，当他们都执行完毕后即可进入下一环节。
```

其中的v1/v2/v6/v3是顺序执行的（意即只有前者执行完成才会执行后者），v4和v5是并行执行的。

Validation创建后是可以后期修改的，通过其addValidator、removeValidator、updateValidator来实现。如果需要在运行时改变，要注意线程安全问题。

Validation上的每个验证器是可以在运行时单独关闭的，被关闭的验证器将不会再被执行。默认情况下所有新建立好的验证器都是打开的。要关闭或者开启Validator，
可以通过Validation.disableValidator()方法来实现，需要传入validator的名称作为依据。

5. 构建ValidationManager

ValidationManager统管Validation，要执行也必须靠它。一个Validation必须要注册到ValidationManager中才可以实施正式的校验。

```java
ValidationManager validationManager = new DafaultValidationManager();
validationManager.register("validation1", validation1);
```

6. 开始校验

使用创建好的validationManager开始校验。

```java
class Bean {
    private String name;
    private int age;
    private int score;
    
    // ... Getters and Setters
}

Bean bean = new Bean();
// bean.setXX();
Map<Bean, ValidationResult<Bean>> result = validationManager.validate("validation1", Collections.singleton
        (bean), ForkJoinPool.commonPool());
// result即为校验结果，取用即可，里面包含了每条数据及其所对应的校验结果
```

在校验过程中可以根据需要向Bocus汇报校验消息：

```java
// xxValidator.java
// validate方法
if (dataWrapper.getOriginalData().name.length() > 10) {
    // report方法位于AbstractValidator中，直接在子类中调用即可。
    report(dataWrapper.getOriginalData(), MessageType.ERROR, "name's length > 10", "E0001", null, null);
    
    // 还可以通过方法签名上的validationContext实现汇报。
    // validationContext.report(...)
        
    // 亦可以通过DataWrapper接口上的方法汇报，这种方式更直接，可以不用指定对应的数据（DataWrapper即是数据）
    // dataWrapper.report(...)
}
```

如果需要在一个任意一个validator检查到ERROR消息的时候便停止整个校验链，使用`dataWrapper.setIgnoreAllFollowingValidators(true)
`。需要注意的是，这个方法是针对单条数据的，意即，在单个数据上，接下来的所有验证器都不再会校验它（接下来的校验器收到的数据集中不再含有此数据）。

如果要一次性杜绝后续所有校验，那么需要使用```ValidationContext.cancelAllFollowingValidators()```。值得注意的是，此方法一经调用，便不能再反悔。

如果在并行校验中，需要在校验器中对其他的校验器做控制，那么需要在被控制的验证代码中加入检查点，Bocus会在收到通知的时候尽力通知到该验证器（类似于Kotlin协程中的cancel功能）。此功能仅在两个同级并行的验证器之间才会生效。

```java
// 被控制的验证器
public static class ScoreValidator2 extends AbstractValidator<Bean> {
    @Override
    protected void validate(Set<DataWrapper<Bean>> data, ValidationContext validationContext) {
        // 检查点，如果此前有收到取消指令，那么此方法后面的代码将不会再执行（通过抛出异常来实现的，用户切勿捕获该运行时异常）
        checkCancellation();
        
        for (DataWrapper<Bean> dataWrapper : data) {
            if (dataWrapper.getOriginalData().score < 80) {
                cancelOtherParallelValidators();
                report(dataWrapper.getOriginalData(), MessageType.WARNING, "score < 80", "E0004", null, null);
            }
        }
    }
}

// 发出消息的验证器
protected void validate(Set<DataWrapper<Bean>> data, ValidationContext validationContext) {
    for (DataWrapper<Bean> dataWrapper : data) {
        if (dataWrapper.getOriginalData().score < 90) {
            // 发出消息给其他的并行验证器，要求它们尽可能取消校验
            cancelOtherParallelValidators();
            report(dataWrapper.getOriginalData(), MessageType.WARNING, "score < 90", "E0005", null, null);
        }
    }
}
```

如果需要在校验的整个流程中传递数据，需要使用到`validationContext.put()`和`validationContext.get()`，它们实在整个Validation中生效的。put()
方法接收的是DataProvider对象，此对象有助于延迟初始化数据，参见其注释可获得详细的信息。

7. 获得校验结果

校验结果即为validationManager.validate()的返回，里面包含了所有的校验信息，除此之外，系统还添加了一些信息，方便开发者调试或者使用。

```java
{
    "StudentBean(name=Maraxxxxxxxxxxxxxxxxxxxxxxx, age=21, score=100)": {
        "originalData": {
            "name": "Maraxxxxxxxxxxxxxxxxxxxxxxx",
            "age": 21,
            "score": 100
        },
        "messages": [
            {
                "messageType": "ERROR",
                "message": "name's length > 8",
                "code": null,
                "field": null,
                "cause": null,
                "extra": {},
                "validationName": "studentValidation",
                "validatorName": "NameValidator",
                "validatorClass": "org.mosin.newebkt.validator.NameValidator",
                "reportLineNumber": 12,
                "validatorAcceptableClass": "org.mosin.newebkt.model.StudentBean",
                "reportCallerTrace": {
                    "classLoaderName": "app",
                    "moduleName": null,
                    "moduleVersion": null,
                    "methodName": "validate",
                    "fileName": "NameValidator.kt",
                    "lineNumber": 12,
                    "nativeMethod": false,
                    "className": "org.mosin.newebkt.validator.NameValidator"
                },
                "validatorCallerTrace": {
                    "classLoaderName": "app",
                    "moduleName": null,
                    "moduleVersion": null,
                    "methodName": "testValidation",
                    "fileName": "TestController.kt",
                    "lineNumber": 40,
                    "nativeMethod": false,
                    "className": "org.mosin.newebkt.controller.TestController"
                },
                "reportThreadName": "http-nio-8080-exec-1",
                "reportDateTime": "2022-04-25T07:14:17.240616+08:00"
            }
        ],
        "validationName": "studentValidation"
    }
}
```

### 例子

该章节用于使你深刻了解exchain所能解决的实际问题

**总则：校验代码需要规范管理，必要的时候可以重用；校验环节可以增删；校验环节可以随时调整位置**。

某系统收到一批学生信息，需要经由程序给出扫描结果：

- 校验跳远成绩，不合格给出“跳远不合格”提示
  - 如果该学生是男性，跳远及格线为2.25m，女性则为1.90m
  - 如果该学生是体育生，那么跳过跳远成绩的校验
  - 如果该学生有特殊标记，则向其跳远成绩加上0.5m
- 如果跳远成绩已经经过检验且合格，那么有关体育的选修课合格分数线调整为50分
- 校验一些选修课的成绩，不合格的给出“选修课不及格”提示 
  - 选修课分为3组，每组3门课，任意一组都及格则认定选修课整体及格
  - 如果选修课整体及格，那么附加选修课的整体分数
- 检查选修课的整体分数（如果有），整体分数在270+时给出“选修课成绩优异”提示
- 校验一些必修课的成绩，不合格的给出“选修课不及格”提示
  - 必修课分为20门课，任意一门不及格则整体不及格
  - 如果必修课的平均成绩高于90分，那么跳过选修课的检查
- 如果没有经历过选修课的检查，那么标注“选修课免查”
- 如果经历了选修课的检查，无论及格与否，统计其整体分数
- 检查每一位学生的违规记录，违规记录存在于数据库中，且数据库只允许查询一次
- 如果前面已经累计了100位同学各项考试都是通过的，那么校验结束，不再检查后续的数据
- 给出校验的整体报告
- 变态需求1：张三同学的信息经历和那些检查步骤？
- 变态需求2：在检查张三同学跳远成绩的时候给出即将进行的所有的后续检查步骤

一般来说，如上的需求对于一个Service来说几乎不可能完成，即便能完成又该如何管理和组织代码？

exchain给出的答案是，分为多个校验类，每个校验类只关注自己的校验主体即可，在校验过程中使用ValidationContext来获得信息，决定自己的校验分支。对于多科成绩，使用并行校验。
对于信息记录，使用AbstractValidator中的report方法汇报结果即可。最后，得到的ValidationResult对象记录了所有的校验过程和信息。