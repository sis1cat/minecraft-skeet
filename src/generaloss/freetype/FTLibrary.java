package generaloss.freetype;

import generaloss.freetype.face.FTFace;
import generaloss.freetype.stroker.FTStroker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class FTLibrary {

    static {
        loadNative();
    }

    private static final String LIBRARY_NAME = "freetype_jni";

    @SuppressWarnings("UnsafeDynamicallyLoadedCode")
    private static void loadNative() {
        final String os = detectOS();

        if(os.equals("android")) {
            System.loadLibrary(LIBRARY_NAME);
            return;
        }

        final String arch = detectArch();
        final String libName = (os.equals("windows") ? LIBRARY_NAME + ".dll" : "lib" + LIBRARY_NAME + ".so");
        final String pathInJar = String.format("/jni/%s/%s/%s", os, arch, libName);

        try(InputStream in = FTLibrary.class.getResourceAsStream(pathInJar)) {
            if(in == null)
                throw new UnsatisfiedLinkError("Native library not found: " + pathInJar);

            final Path temp = Files.createTempFile(LIBRARY_NAME, libName);
            temp.toFile().deleteOnExit();

            try(OutputStream out = Files.newOutputStream(temp)) {
                final byte[] buffer = new byte[4096];
                int read;
                while((read = in.read(buffer)) != -1)
                    out.write(buffer, 0, read);
            }

            System.load(temp.toAbsolutePath().toString());
        }catch(IOException e) {
            throw new RuntimeException("Failed to load native library", e);
        }
    }

    private static String detectOS() {
        final String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("win"))
            return "windows";

        if(os.contains("linux")) {
            String vm = System.getProperty("java.vm.name").toLowerCase();
            if(vm.contains("dalvik") || vm.contains("art"))
                return "android";
            return "linux";
        }
        throw new UnsupportedOperationException("Unsupported OS: " + os);
    }

    private static String detectArch() {
        final String arch = System.getProperty("os.arch").toLowerCase();
        if(arch.contains("amd64") || arch.contains("x86_64")) return "x86_64";
        if(arch.contains("86")) return "i686";
        if(arch.contains("aarch64") || arch.contains("arm64")) return "aarch64";
        if(arch.contains("riscv")) return "riscv64";
        if(arch.contains("x86")) return "x86";
        throw new UnsupportedOperationException("Unsupported architecture: " + arch);
    }



    public static int encodeChars(char a, char b, char c, char d) {
        return (a << 24) | (b << 16) | (c << 8) | d;
    }

    public static int FTPos_toInt(int value) {
        return ((value + 63) & -64) >> 6;
    }


    private static native int getLastErrorCode();

    public static FTError getLastError() {
        return FTError.byCode(getLastErrorCode());
    }

    private static native long initFreeType();

    public static FTLibrary init() {
        final long address = initFreeType();
        if(address == 0)
            throw new RuntimeException("Couldn't initialize FreeType library: " + getLastError());

        return new FTLibrary(address);
    }


    private final long address;

    private FTLibrary(long address) {
        this.address = address;
    }

    public long getAddress() {
        return address;
    }


    private static native long newMemoryFace(long library, ByteBuffer data, int dataSize, int faceIndex);

    public FTFace newMemoryFace(ByteBuffer buffer, int faceIndex) {
        final long face = newMemoryFace(address, buffer, buffer.remaining(), faceIndex);
        if(face == 0)
            throw new RuntimeException("Couldn't load font: " + FTLibrary.getLastError());
        return new FTFace(face);
    }

    public FTFace newMemoryFace(byte[] data, int faceIndex) {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
        buffer.put(data);
        buffer.position(0);
        return newMemoryFace(buffer, faceIndex);
    }


    private static native long strokerNew(long library);

    public FTStroker strokerNew() {
        final long stroker = strokerNew(address);
        if(stroker == 0)
            throw new RuntimeException("Couldn't create FreeType stroker: " + FTLibrary.getLastError());
        return new FTStroker(stroker);
    }


    private static native void doneFreeType(long library);

    public void done() {
        doneFreeType(address);
    }

}
