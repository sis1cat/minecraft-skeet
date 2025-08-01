package net.optifine.shaders;

import com.google.common.base.Joiner;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.optifine.Config;
import net.optifine.util.StrUtils;

public class ShaderPackZip implements IShaderPack {
    protected File packFile;
    protected ZipFile packZipFile;
    protected String baseFolder;
    private static final Pattern PATTERN_FOLDER_SHADERS = Pattern.compile("([^/]+/)shaders/");

    public ShaderPackZip(String name, File file) {
        this.packFile = file;
        this.packZipFile = null;
        this.baseFolder = "";
    }

    @Override
    public void close() {
        if (this.packZipFile != null) {
            try {
                this.packZipFile.close();
            } catch (Exception exception) {
            }

            this.packZipFile = null;
        }
    }

    @Override
    public InputStream getResourceAsStream(String resName) {
        try {
            if (this.packZipFile == null) {
                this.packZipFile = new ZipFile(this.packFile);
                this.baseFolder = this.detectBaseFolder(this.packZipFile);
            }

            String s = StrUtils.removePrefix(resName, "/");
            if (s.contains("..")) {
                s = this.resolveRelative(s);
            }

            ZipEntry zipentry = this.packZipFile.getEntry(this.baseFolder + s);
            return zipentry == null ? null : this.packZipFile.getInputStream(zipentry);
        } catch (Exception exception) {
            return null;
        }
    }

    private String resolveRelative(String name) {
        Deque<String> deque = new ArrayDeque<>();
        String[] astring = Config.tokenize(name, "/");

        for (int i = 0; i < astring.length; i++) {
            String s = astring[i];
            if (s.equals("..")) {
                if (deque.isEmpty()) {
                    return "";
                }

                deque.removeLast();
            } else {
                deque.add(s);
            }
        }

        return Joiner.on('/').join(deque);
    }

    private String detectBaseFolder(ZipFile zip) {
        ZipEntry zipentry = zip.getEntry("shaders/");
        if (zipentry != null && zipentry.isDirectory()) {
            return "";
        } else {
            Enumeration<? extends ZipEntry> enumeration = zip.entries();

            while (enumeration.hasMoreElements()) {
                ZipEntry zipentry1 = enumeration.nextElement();
                String s = zipentry1.getName();
                Matcher matcher = PATTERN_FOLDER_SHADERS.matcher(s);
                if (matcher.matches()) {
                    String s1 = matcher.group(1);
                    if (s1 != null) {
                        if (s1.equals("shaders/")) {
                            return "";
                        }

                        return s1;
                    }
                }
            }

            return "";
        }
    }

    @Override
    public boolean hasDirectory(String resName) {
        try {
            if (this.packZipFile == null) {
                this.packZipFile = new ZipFile(this.packFile);
                this.baseFolder = this.detectBaseFolder(this.packZipFile);
            }

            String s = StrUtils.removePrefix(resName, "/");
            ZipEntry zipentry = this.packZipFile.getEntry(this.baseFolder + s);
            return zipentry != null;
        } catch (IOException ioexception) {
            return false;
        }
    }

    @Override
    public String getName() {
        return this.packFile.getName();
    }
}