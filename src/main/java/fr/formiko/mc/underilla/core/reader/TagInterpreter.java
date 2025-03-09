package fr.formiko.mc.underilla.core.reader;

import com.jkantrell.nbt.tag.ByteArrayTag;
import com.jkantrell.nbt.tag.ByteTag;
import com.jkantrell.nbt.tag.CompoundTag;
import com.jkantrell.nbt.tag.DoubleTag;
import com.jkantrell.nbt.tag.FloatTag;
import com.jkantrell.nbt.tag.IntArrayTag;
import com.jkantrell.nbt.tag.IntTag;
import com.jkantrell.nbt.tag.LongArrayTag;
import com.jkantrell.nbt.tag.LongTag;
import com.jkantrell.nbt.tag.ShortTag;
import com.jkantrell.nbt.tag.StringTag;
import com.jkantrell.nbt.tag.Tag;
import java.util.Map;
import java.util.StringJoiner;

public abstract class TagInterpreter<T extends Tag<?>> {

    // CONSTANTS
    public static final TagInterpreter<ByteTag> BYTE = new TagInterpreter<>() {
    };
    public static final TagInterpreter<ShortTag> SHORT = new TagInterpreter<>() {
    };
    public static final TagInterpreter<IntTag> INT = new TagInterpreter<>() {
    };
    public static final TagInterpreter<LongTag> LONG = new TagInterpreter<>() {
    };
    public static final TagInterpreter<FloatTag> FLOAT = new TagInterpreter<>() {
    };
    public static final TagInterpreter<DoubleTag> DOUBLE = new TagInterpreter<>() {
    };
    public static final TagInterpreter<StringTag> STRING = new TagInterpreter<>() {
        @Override
        public String interpretBlockDataString(StringTag tag) { return tag.getValue(); }
    };
    public static final TagInterpreter<ByteArrayTag> BYTE_ARRAY = new TagInterpreter<>() {
    };
    public static final TagInterpreter<IntArrayTag> INT_ARRAY = new TagInterpreter<>() {
    };
    public static final TagInterpreter<LongArrayTag> LONG_ARRAY = new TagInterpreter<>() {
    };
    public static final TagInterpreter<CompoundTag> COMPOUND = new TagInterpreter<>() {
        @Override
        public String interpretBlockDataString(CompoundTag tag) {
            StringJoiner string = new StringJoiner(",", "[", "]");
            tag.forEach((n, t) -> {
                TagInterpreter interpreter = TagInterpreter.fromClass(t.getClass());
                if (interpreter == null) {
                    return;
                }
                String s = interpreter.interpretBlockDataString(t);
                string.add(n + "=" + s);
            });
            return string.toString();
        }
    };


    // STATIC ASSETS
    private static final Map<Class<?>, TagInterpreter<?>> CLASS_INTERPRETER_MAP = Map.ofEntries(Map.entry(ByteTag.class, BYTE),
            Map.entry(ShortTag.class, SHORT), Map.entry(IntTag.class, INT), Map.entry(LongTag.class, LONG),
            Map.entry(FloatTag.class, FLOAT), Map.entry(DoubleTag.class, DOUBLE), Map.entry(StringTag.class, STRING),
            Map.entry(ByteArrayTag.class, BYTE_ARRAY), Map.entry(IntArrayTag.class, INT_ARRAY), Map.entry(LongArrayTag.class, LONG_ARRAY),
            Map.entry(CompoundTag.class, COMPOUND));


    // STATIC METHODS
    public static <T extends Tag<?>> TagInterpreter<T> fromClass(Class<T> type) {
        return (TagInterpreter<T>) CLASS_INTERPRETER_MAP.get(type);
    }


    // UTIL
    public String interpretBlockDataString(T tag) { return tag.valueToString(); }
}