package com.mara.zoic.annohttp.http.serialization;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 拷贝自jackson-databind中的{@code com.fasterxml.jackson.core.type.TypeReference}类。
 * <br/>
 * 拷贝仅仅是为了避免对jackson-databind的强依赖。<br/>
 * Mara.X.Ma 注：{@link TypeRef} 用于对泛型类型的引用进行保持，避免被擦除。对于泛型中指定的类型，在编译期通过校验后会被擦除，
 * 但有个例外，那就是子类如果实现了泛型接口（父类），并且父接口（类）指定了具体的泛型类型，那么子类对父类的泛型类型将不会被擦除，
 * 可以在运行时通过反射获得。究其原因，是因为子类的实现强绑定了泛型类型，编译器不能将其擦除，否则会导致无法生成子类。
 *
 * @author jackson-databind, Mara.X.Ma copied
 * @since 1.0.0 2022-07-16
 */
public class TypeRef<T> implements Comparable<TypeRef<T>> {

    protected final Type _type;

    /**
     * Mara.X.Ma 注：为什么构造函数要设计为protected的？
     * 因为作者希望与用户的代码中new出来的TypeReference是其子类实例（匿名类）而非其直接实例。
     * 这样才可以对泛型类型进行封存。
     */
    protected TypeRef() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof Class<?>) { // sanity check, should never happen
            throw new IllegalArgumentException("Internal error: TypeReference constructed without actual type information");
        }
        /* 22-Dec-2008, tatu: Not sure if this case is safe -- I suspect
         *   it is possible to make it fail?
         *   But let's deal with specific
         *   case when we know an actual use case, and thereby suitable
         *   workarounds for valid case(s) and/or error to throw
         *   on invalid one(s).
         */
        _type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    protected TypeRef(Type type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        _type = type;
    }

    public Type getType() {
        return _type;
    }

    public static <T> TypeRef<T> fromType(Type type) {
        return new TypeRef<>(type) {
        };
    }

    /**
     * The only reason we define this method (and require implementation
     * of <code>Comparable</code>) is to prevent constructing a
     * reference without type information.
     */
    @Override
    public int compareTo(TypeRef<T> o) {
        return 0;
    }
    // just need an implementation, not a good one... hence ^^^
}
