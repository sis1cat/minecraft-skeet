package net.minecraft.resources;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.GsonHelper;
import net.optifine.util.StrUtils;

public final class ResourceLocation implements Comparable<ResourceLocation> {
    public static final Codec<ResourceLocation> CODEC = Codec.STRING.comapFlatMap(ResourceLocation::read, ResourceLocation::toString).stable();
    public static final StreamCodec<ByteBuf, ResourceLocation> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
        .map(ResourceLocation::parse, ResourceLocation::toString);
    public static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType(Component.translatable("argument.id.invalid"));
    public static final char NAMESPACE_SEPARATOR = ':';
    public static final String DEFAULT_NAMESPACE = "minecraft";
    public static final String REALMS_NAMESPACE = "realms";
    private final String namespace;
    private final String path;
    private final boolean defaultNamespace;

    public ResourceLocation(String str) {
        this(getNamespace(str), getPath(str));
    }

    private static String getNamespace(String str) {
        int i = str.indexOf(58);
        return i >= 0 ? str.substring(0, i) : "minecraft";
    }

    private static String getPath(String str) {
        int i = str.indexOf(58);
        return i >= 0 ? str.substring(i + 1) : str;
    }

    public ResourceLocation(String pNamespace, String pPath) {
        this.namespace = pNamespace;
        this.path = pPath;
        this.defaultNamespace = "minecraft".equals(pNamespace);
    }

    private static ResourceLocation createUntrusted(String pNamespace, String pPath) {
        return new ResourceLocation(assertValidNamespace(pNamespace, pPath), assertValidPath(pNamespace, pPath));
    }

    public static ResourceLocation fromNamespaceAndPath(String pNamespace, String pPath) {
        return createUntrusted(pNamespace, pPath);
    }

    public static ResourceLocation parse(String pLocation) {
        return bySeparator(pLocation, ':');
    }

    public static ResourceLocation withDefaultNamespace(String pLocation) {
        return new ResourceLocation("minecraft", assertValidPath("minecraft", pLocation));
    }

    @Nullable
    public static ResourceLocation tryParse(String pLocation) {
        return tryBySeparator(pLocation, ':');
    }

    @Nullable
    public static ResourceLocation tryBuild(String pNamespace, String pPath) {
        return isValidNamespace(pNamespace) && isValidPath(pPath) ? new ResourceLocation(pNamespace, pPath) : null;
    }

    public static ResourceLocation bySeparator(String pLocation, char pSeperator) {
        int i = pLocation.indexOf(pSeperator);
        if (i >= 0) {
            String s = pLocation.substring(i + 1);
            if (i != 0) {
                String s1 = pLocation.substring(0, i);
                return createUntrusted(s1, s);
            } else {
                return withDefaultNamespace(s);
            }
        } else {
            return withDefaultNamespace(pLocation);
        }
    }

    @Nullable
    public static ResourceLocation tryBySeparator(String pLocation, char pSeperator) {
        int i = pLocation.indexOf(pSeperator);
        if (i >= 0) {
            String s = pLocation.substring(i + 1);
            if (!isValidPath(s)) {
                return null;
            } else if (i != 0) {
                String s1 = pLocation.substring(0, i);
                return isValidNamespace(s1) ? new ResourceLocation(s1, s) : null;
            } else {
                return new ResourceLocation("minecraft", s);
            }
        } else {
            return isValidPath(pLocation) ? new ResourceLocation("minecraft", pLocation) : null;
        }
    }

    public static DataResult<ResourceLocation> read(String pLocation) {
        try {
            return DataResult.success(parse(pLocation));
        } catch (ResourceLocationException resourcelocationexception) {
            return DataResult.error(() -> "Not a valid resource location: " + pLocation + " " + resourcelocationexception.getMessage());
        }
    }

    public String getPath() {
        return this.path;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public ResourceLocation withPath(String pPath) {
        return new ResourceLocation(this.namespace, assertValidPath(this.namespace, pPath));
    }

    public ResourceLocation withPath(UnaryOperator<String> pPathOperator) {
        return this.withPath(pPathOperator.apply(this.path));
    }

    public ResourceLocation withPrefix(String pPathPrefix) {
        return this.withPath(pPathPrefix + this.path);
    }

    public ResourceLocation withSuffix(String pPathSuffix) {
        return this.withPath(this.path + pPathSuffix);
    }

    @Override
    public String toString() {
        return this.namespace + ":" + this.path;
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return pOther instanceof ResourceLocation resourcelocation
                ? this.namespace.equals(resourcelocation.namespace) && this.path.equals(resourcelocation.path)
                : false;
        }
    }

    @Override
    public int hashCode() {
        return 31 * this.namespace.hashCode() + this.path.hashCode();
    }

    public int compareTo(ResourceLocation pOther) {
        int i = this.path.compareTo(pOther.path);
        if (i == 0) {
            i = this.namespace.compareTo(pOther.namespace);
        }

        return i;
    }

    public String toDebugFileName() {
        return this.toString().replace('/', '_').replace(':', '_');
    }

    public String toLanguageKey() {
        return this.namespace + "." + this.path;
    }

    public String toShortLanguageKey() {
        return this.namespace.equals("minecraft") ? this.path : this.toLanguageKey();
    }

    public String toLanguageKey(String pType) {
        return pType + "." + this.toLanguageKey();
    }

    public String toLanguageKey(String pType, String pKey) {
        return pType + "." + this.toLanguageKey() + "." + pKey;
    }

    private static String readGreedy(StringReader pReader) {
        int i = pReader.getCursor();

        while (pReader.canRead() && isAllowedInResourceLocation(pReader.peek())) {
            pReader.skip();
        }

        return pReader.getString().substring(i, pReader.getCursor());
    }

    public static ResourceLocation read(StringReader pReader) throws CommandSyntaxException {
        int i = pReader.getCursor();
        String s = readGreedy(pReader);

        try {
            return parse(s);
        } catch (ResourceLocationException resourcelocationexception) {
            pReader.setCursor(i);
            throw ERROR_INVALID.createWithContext(pReader);
        }
    }

    public static ResourceLocation readNonEmpty(StringReader pReader) throws CommandSyntaxException {
        int i = pReader.getCursor();
        String s = readGreedy(pReader);
        if (s.isEmpty()) {
            throw ERROR_INVALID.createWithContext(pReader);
        } else {
            try {
                return parse(s);
            } catch (ResourceLocationException resourcelocationexception) {
                pReader.setCursor(i);
                throw ERROR_INVALID.createWithContext(pReader);
            }
        }
    }

    public static boolean isAllowedInResourceLocation(char pCharacter) {
        return pCharacter >= '0' && pCharacter <= '9'
            || pCharacter >= 'a' && pCharacter <= 'z'
            || pCharacter == '_'
            || pCharacter == ':'
            || pCharacter == '/'
            || pCharacter == '.'
            || pCharacter == '-';
    }

    public static boolean isValidPath(String pPath) {
        for (int i = 0; i < pPath.length(); i++) {
            if (!validPathChar(pPath.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static boolean isValidNamespace(String pNamespace) {
        for (int i = 0; i < pNamespace.length(); i++) {
            if (!validNamespaceChar(pNamespace.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private static String assertValidNamespace(String pNamespace, String pPath) {
        if (!isValidNamespace(pNamespace)) {
            throw new ResourceLocationException("Non [a-z0-9_.-] character in namespace of location: " + pNamespace + ":" + pPath);
        } else {
            return pNamespace;
        }
    }

    public static boolean validPathChar(char pPathChar) {
        return pPathChar == '_'
            || pPathChar == '-'
            || pPathChar >= 'a' && pPathChar <= 'z'
            || pPathChar >= '0' && pPathChar <= '9'
            || pPathChar == '/'
            || pPathChar == '.';
    }

    private static boolean validNamespaceChar(char pNamespaceChar) {
        return pNamespaceChar == '_' || pNamespaceChar == '-' || pNamespaceChar >= 'a' && pNamespaceChar <= 'z' || pNamespaceChar >= '0' && pNamespaceChar <= '9' || pNamespaceChar == '.';
    }

    private static String assertValidPath(String pNamespace, String pPath) {
        if (!isValidPath(pPath)) {
            throw new ResourceLocationException("Non [a-z0-9/._-] character in path of location: " + pNamespace + ":" + pPath);
        } else {
            return pPath;
        }
    }

    public boolean isDefaultNamespace() {
        return this.defaultNamespace;
    }

    public boolean isOptiFine() {
        return this.defaultNamespace && this.path.startsWith("optifine");
    }

    public ResourceLocation removePrefix(String prefix) {
        String s = StrUtils.removePrefix(this.path, prefix);
        return this.withPath(s);
    }

    public ResourceLocation removeSuffix(String suffix) {
        String s = StrUtils.removeSuffix(this.path, suffix);
        return this.withPath(s);
    }

    public ResourceLocation removePrefixSuffix(String prefix, String suffix) {
        String s = StrUtils.removePrefixSuffix(this.path, prefix, suffix);
        return this.withPath(s);
    }

    public int compareNamespaced(ResourceLocation o) {
        int i = this.namespace.compareTo(o.namespace);
        return i != 0 ? i : this.path.compareTo(o.path);
    }

    public static class Serializer implements JsonDeserializer<ResourceLocation>, JsonSerializer<ResourceLocation> {
        public ResourceLocation deserialize(JsonElement pJson, Type pTypeOfT, JsonDeserializationContext pContext) throws JsonParseException {
            return ResourceLocation.parse(GsonHelper.convertToString(pJson, "location"));
        }

        public JsonElement serialize(ResourceLocation pSrc, Type pTypeOfSrc, JsonSerializationContext pContext) {
            return new JsonPrimitive(pSrc.toString());
        }
    }
}