package sisicat.main.utilities;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import com.darkmagician6.eventapi.types.Priority;
import com.mojang.blaze3d.systems.RenderSystem;
import sisicat.IDefault;
import sisicat.events.GraphicsEvent;
import sisicat.events.WindowResizeEvent;
import sisicat.main.gui.elements.Window;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class Render implements IDefault {

    private static int programID;

    private static int
            vao,
            vbo,
            ebo;

    public static float[] vertices = new float[26 * 4 * 2000];
    public static int[] indices = new int[6 * 2000];

    private static int
            v, i;

    public static final FrameBuffer frameBuffer = new FrameBuffer();

    public static void initialize() {

        initializeShaderProgram();
        initializeVAO();

    }

    private static void initializeShaderProgram() {

        programID = glCreateProgram();

        try {

            int vertexShaderID = glCreateShader(GL_VERTEX_SHADER);
            int fragmentShaderID = glCreateShader(GL_FRAGMENT_SHADER);

            glShaderSource(vertexShaderID, rectangleVertexShaderSource);
            glCompileShader(vertexShaderID);
            checkShaderCompilation(vertexShaderID);

            glShaderSource(fragmentShaderID, fragmentShaderSource);
            glCompileShader(fragmentShaderID);
            checkShaderCompilation(fragmentShaderID);

            glAttachShader(programID, vertexShaderID);
            glAttachShader(programID, fragmentShaderID);

            glLinkProgram(programID);
            checkProgramLinking(programID);

            glDeleteShader(vertexShaderID);
            glDeleteShader(fragmentShaderID);

        } catch (Exception exception) {
            exception.fillInStackTrace();
        }

    }

    private static void initializeVAO() {

        vao = glGenVertexArrays();

        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_DYNAMIC_DRAW);

        int stride = 26 * Float.BYTES;

        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2 * Float.BYTES);
        glVertexAttribPointer(2, 4, GL_FLOAT, false, stride, 4 * Float.BYTES);
        glVertexAttribPointer(3, 4, GL_FLOAT, false, stride, 8 * Float.BYTES);
        glVertexAttribPointer(4, 4, GL_FLOAT, false, stride, 12 * Float.BYTES);
        glVertexAttribPointer(5, 4, GL_FLOAT, false, stride, 16 * Float.BYTES);
        glVertexAttribPointer(6, 4, GL_FLOAT, false, stride, 20 * Float.BYTES);
        glVertexAttribPointer(7, 1, GL_FLOAT, false, stride, 24 * Float.BYTES);
        glVertexAttribPointer(8, 1, GL_FLOAT, false, stride, 25 * Float.BYTES);

        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
        glEnableVertexAttribArray(3);
        glEnableVertexAttribArray(4);
        glEnableVertexAttribArray(5);
        glEnableVertexAttribArray(6);
        glEnableVertexAttribArray(7);
        glEnableVertexAttribArray(8);

        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glBindVertexArray(0);

    }

    private static void checkShaderCompilation(int shaderID) {
        if (glGetShaderi(shaderID, GL_COMPILE_STATUS) == GL_FALSE) {
            String infoLog = glGetShaderInfoLog(shaderID);
            System.err.println("Shader compilation failed: " + infoLog);
        }
    }

    private static void checkProgramLinking(int programID) {
        if (glGetProgrami(programID, GL_LINK_STATUS) == GL_FALSE) {
            String infoLog = glGetProgramInfoLog(programID);
            System.err.println("Program linking failed: " + infoLog);
        }
    }

    private static final float[] dummyColor = {0, 0, 0, 0};

    public static void drawRectangle(int x, int y, int width, int height, float[] color, float alpha) {

        color = convertColor(color);

        int resolutionHeight = Window.gameWindowHeight;

        // vertex 1

        firstVertex(x, y, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color, dummyColor, dummyColor, dummyColor);

        setupShader(0);
        setupGlobalAlpha(alpha);

        // vertex 2

        secondVertex(x, y, height, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color, dummyColor, dummyColor, dummyColor);

        setupShader(0);
        setupGlobalAlpha(alpha);

        // vertex 3

        thirdVertex(x, y, width, height, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color, dummyColor, dummyColor, dummyColor);

        setupShader(0);
        setupGlobalAlpha(alpha);

        // vertex 4

        fourthVertex(x, y, width, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color, dummyColor, dummyColor, dummyColor);

        setupShader(0);
        setupGlobalAlpha(alpha);


        applyIndices();


    }

    public static void drawRectangleBorders(int x, int y, int width, int height, int size, float[] color, float alpha) {

        color = convertColor(color);

        int resolutionHeight = Window.gameWindowHeight;

        float[] sizes = {size, 0, 0, 0};

        // vertex 1

        firstVertex(x, y, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color, sizes, dummyColor, dummyColor);

        setupShader(1);
        setupGlobalAlpha(alpha);

        // vertex 2

        secondVertex(x, y, height, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color, sizes, dummyColor, dummyColor);

        setupShader(1);
        setupGlobalAlpha(alpha);

        // vertex 3

        thirdVertex(x, y, width, height, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color, sizes, dummyColor, dummyColor);

        setupShader(1);
        setupGlobalAlpha(alpha);

        // vertex 4

        fourthVertex(x, y, width, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color, sizes, dummyColor, dummyColor);

        setupShader(1);
        setupGlobalAlpha(alpha);


        applyIndices();


    }

    public static void drawCircle(int x, int y, int size, float[] color, float alpha) {

        color = convertColor(color);

        int resolutionHeight = Window.gameWindowHeight;

        // vertex 1

        firstVertex(x, y, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, size, size, resolutionHeight);
        setupColors(color, dummyColor, dummyColor, dummyColor);

        setupShader(2);
        setupGlobalAlpha(alpha);

        // vertex 2

        secondVertex(x, y, size, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, size, size, resolutionHeight);
        setupColors(color, dummyColor, dummyColor, dummyColor);

        setupShader(2);
        setupGlobalAlpha(alpha);

        // vertex 3

        thirdVertex(x, y, size, size, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, size, size, resolutionHeight);
        setupColors(color, dummyColor, dummyColor, dummyColor);

        setupShader(2);
        setupGlobalAlpha(alpha);

        // vertex 4

        fourthVertex(x, y, size, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, size, size, resolutionHeight);
        setupColors(color, dummyColor, dummyColor, dummyColor);

        setupShader(2);
        setupGlobalAlpha(alpha);


        applyIndices();


    }

    public static void drawGradientRectangle(int x, int y, int width, int height, float[] color1, float[] color2, float[] color3, float[] color4, float alpha) {

        color1 = convertColor(color1);
        color2 = convertColor(color2);
        color3 = convertColor(color3);
        color4 = convertColor(color4);

        int resolutionHeight = Window.gameWindowHeight;

        // vertex 1

        firstVertex(x, y, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color1, color2, color3, color4);

        setupShader(3);
        setupGlobalAlpha(alpha);

        // vertex 2

        secondVertex(x, y, height, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color1, color2, color3, color4);

        setupShader(3);
        setupGlobalAlpha(alpha);

        // vertex 3

        thirdVertex(x, y, width, height, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color1, color2, color3, color4);

        setupShader(3);
        setupGlobalAlpha(alpha);

        // vertex 4

        fourthVertex(x, y, width, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color1, color2, color3, color4);

        setupShader(3);
        setupGlobalAlpha(alpha);


        applyIndices();


    }

    public static void drawMenuTexture(int x, int y, int width, int height, float alpha) {

        int resolutionHeight = Window.gameWindowHeight;

        // vertex 1

        firstVertex(x, y, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(dummyColor, dummyColor, dummyColor, dummyColor);

        setupShader(4);
        setupGlobalAlpha(alpha);

        // vertex 2

        secondVertex(x, y, height, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(dummyColor, dummyColor, dummyColor, dummyColor);

        setupShader(4);
        setupGlobalAlpha(alpha);

        // vertex 3

        thirdVertex(x, y, width, height, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(dummyColor, dummyColor, dummyColor, dummyColor);

        setupShader(4);
        setupGlobalAlpha(alpha);

        // vertex 4

        fourthVertex(x, y, width, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(dummyColor, dummyColor, dummyColor, dummyColor);

        setupShader(4);
        setupGlobalAlpha(alpha);


        applyIndices();


    }

    public static void drawTriangle1(int x, int y, int width, int height, float[] color, float alpha) {

        int resolutionHeight = Window.gameWindowHeight;

        color = convertColor(color);

        // vertex 1

        firstVertex(x, y, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color, dummyColor, dummyColor, dummyColor);

        setupShader(5);
        setupGlobalAlpha(alpha);

        // vertex 2

        secondVertex(x, y, height, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color, dummyColor, dummyColor, dummyColor);

        setupShader(5);
        setupGlobalAlpha(alpha);

        // vertex 3

        thirdVertex(x, y, width, height, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color, dummyColor, dummyColor, dummyColor);

        setupShader(5);
        setupGlobalAlpha(alpha);

        // vertex 4

        fourthVertex(x, y, width, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color, dummyColor, dummyColor, dummyColor);

        setupShader(5);
        setupGlobalAlpha(alpha);


        applyIndices();


    }

    public static void drawTriangle2(int x, int y, int width, int height, float[] color, float alpha) {

        int resolutionHeight = Window.gameWindowHeight;

        color = convertColor(color);

        // vertex 1

        firstVertex(x, y, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color, dummyColor, dummyColor, dummyColor);

        setupShader(6);
        setupGlobalAlpha(alpha);

        // vertex 2

        secondVertex(x, y, height, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color, dummyColor, dummyColor, dummyColor);

        setupShader(6);
        setupGlobalAlpha(alpha);

        // vertex 3

        thirdVertex(x, y, width, height, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color, dummyColor, dummyColor, dummyColor);

        setupShader(6);
        setupGlobalAlpha(alpha);

        // vertex 4

        fourthVertex(x, y, width, resolutionHeight);
        vertices[v++] = 0;
        vertices[v++] = 0;

        setupRectangleProperties(x, y, width, height, resolutionHeight);
        setupColors(color, dummyColor, dummyColor, dummyColor);

        setupShader(6);
        setupGlobalAlpha(alpha);


        applyIndices();


    }

    public static void drawFrameBuffer(float[] color) {

        final float
                tx = 0,
                ty = 0,
                tx2 = 1,
                ty2 = 1;

        color = convertColor(color);
        float[] fontType = {0, 0, 0, 0};


        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = tx;
        vertices[v++] = ty;

        setupRectangleProperties(0, 0, 0, 0, 0);
        setupColors(color, fontType, dummyColor, dummyColor);

        setupShader(8);
        setupGlobalAlpha(255);


        vertices[v++] = mc.getWindow().getWidth();
        vertices[v++] = 0;
        vertices[v++] = tx2;
        vertices[v++] = ty;

        setupRectangleProperties(0, 0, 0, 0, 0);
        setupColors(color, fontType, dummyColor, dummyColor);

        setupShader(8);
        setupGlobalAlpha(255);


        vertices[v++] = mc.getWindow().getWidth();
        vertices[v++] = mc.getWindow().getHeight();
        vertices[v++] = tx2;
        vertices[v++] = ty2;

        setupRectangleProperties(0, 0, 0, 0, 0);
        setupColors(color, fontType, dummyColor, dummyColor);

        setupShader(8);
        setupGlobalAlpha(255);


        vertices[v++] = 0;
        vertices[v++] = mc.getWindow().getHeight();
        vertices[v++] = tx;
        vertices[v++] = ty2;

        setupRectangleProperties(0, 0, 0, 0, 0);
        setupColors(color, fontType, dummyColor, dummyColor);

        setupShader(8);
        setupGlobalAlpha(255);


        applyIndices();


    }

    public static void drawCharacter(char c, float x, float y, float[] color, float alpha, Font font) {

        int[] charData = font.charactersMap.get(c);
        int charX = charData[0];
        int charY = charData[1];
        int charWidth = charData[2];
        int charHeight = charData[3];

        float tx = (float) charX / 2048;
        float ty = (float) (charY + charHeight) / 2048;
        float tx2 = (float) (charX + charWidth) / 2048;
        float ty2 = (float) charY / 2048;

        int resolutionHeight = Window.gameWindowHeight;

        color = convertColor(color);

        x = (float) Math.floor(x);
        y = (float) Math.floor(y);

        // vertex 1

        vertices[v++] = x;
        vertices[v++] = y;
        vertices[v++] = tx;
        vertices[v++] = ty;

        float[] fontType = {0, 0, 0, 0};

        if(font == Text.getMenuBoldFont())
            fontType[0] = 1;
        else if(font == Text.getMenuBindsFont())
            fontType[0] = 2;
        else if(font == Text.MENU_ICONS)
            fontType[0] = 3;

        setupRectangleProperties(0, 0, 0, 0, 0);
        setupColors(color, fontType, dummyColor, dummyColor);

        setupShader(7);
        setupGlobalAlpha(alpha);

        // vertex 2

        vertices[v++] = x + charWidth;
        vertices[v++] = y;
        vertices[v++] = tx2;
        vertices[v++] = ty;

        setupRectangleProperties(0, 0, 0, 0, 0);
        setupColors(color, fontType, dummyColor, dummyColor);

        setupShader(7);
        setupGlobalAlpha(alpha);

        // vertex 3

        vertices[v++] = x + charWidth;
        vertices[v++] = y + charHeight;
        vertices[v++] = tx2;
        vertices[v++] = ty2;

        setupRectangleProperties(0, 0, 0, 0, 0);
        setupColors(color, fontType, dummyColor, dummyColor);

        setupShader(7);
        setupGlobalAlpha(alpha);

        // vertex 4

        vertices[v++] = x;
        vertices[v++] = y + charHeight;
        vertices[v++] = tx;
        vertices[v++] = ty2;

        setupRectangleProperties(0, 0, 0, 0, 0);
        setupColors(color, fontType, dummyColor, dummyColor);

        setupShader(7);
        setupGlobalAlpha(alpha);


        applyIndices();


    }

    private static void firstVertex(int x, int y, int resolutionHeight) {
        vertices[v++] = x;
        vertices[v++] = resolutionHeight - y;
    }

    private static void secondVertex(int x, int y, int height, int resolutionHeight) {
        vertices[v++] = x;
        vertices[v++] = resolutionHeight - (y + height);
    }

    private static void thirdVertex(int x, int y, int width, int height, int resolutionHeight) {
        vertices[v++] = x + width;
        vertices[v++] = resolutionHeight - (y + height);
    }

    private static void fourthVertex(int x, int y, int width, int resolutionHeight) {
        vertices[v++] = x + width;
        vertices[v++] = resolutionHeight - y;
    }

    private static void setupRectangleProperties(int x, int y, int width, int height, int resolutionHeight) {

        vertices[v++] = x;
        vertices[v++] = resolutionHeight - y - height;
        vertices[v++] = width;
        vertices[v++] = height;

    }

    private static void setupColors(float[] color1, float[] color2, float[] color3, float[] color4) {

        vertices[v++] = color1[0];
        vertices[v++] = color1[1];
        vertices[v++] = color1[2];
        vertices[v++] = color1[3];

        vertices[v++] = color2[0];
        vertices[v++] = color2[1];
        vertices[v++] = color2[2];
        vertices[v++] = color2[3];

        vertices[v++] = color3[0];
        vertices[v++] = color3[1];
        vertices[v++] = color3[2];
        vertices[v++] = color3[3];

        vertices[v++] = color4[0];
        vertices[v++] = color4[1];
        vertices[v++] = color4[2];
        vertices[v++] = color4[3];

    }

    private static void setupShader(int shader) {

        vertices[v++] = shader;

    }

    private static void setupGlobalAlpha(float alpha) {

        vertices[v++] = alpha / 255f;

    }

    private static void applyIndices() {

        int start = v / 26 - 4;

        indices[i++] = start;
        indices[i++] = 1 + start;
        indices[i++] = 2 + start;
        indices[i++] = 2 + start;
        indices[i++] = 3 + start;
        indices[i++] = start;

    }

    private static float[] convertColor(float[] color) {

        float[] outColor;

        if(color.length < 4)
            outColor = new float[]{Math.abs(color[0]), Math.abs(color[1]), Math.abs(color[2]), 255};
        else
            outColor = new float[]{Math.abs(color[0]), Math.abs(color[1]), Math.abs(color[2]), Math.abs(color[3])};

        for (int i = 0; i < outColor.length; i++)
            outColor[i] = outColor[i] / 255.0f;

        return outColor;

    }

    public static void drawAll() {

        if(!uploadBuffers())
            return;

        //defaultBlendFunc();

        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);

        beginShaderProgram(Window.gameWindowWidth, Window.gameWindowHeight);

        int boundTextureUnit0 = glGetInteger(GL_ACTIVE_TEXTURE);
        glActiveTexture(GL_TEXTURE0);
        int prevTexture0 = glGetInteger(GL_TEXTURE_BINDING_2D);

        glActiveTexture(GL_TEXTURE1);
        int prevTexture1 = glGetInteger(GL_TEXTURE_BINDING_2D);

        glActiveTexture(GL_TEXTURE2);
        int prevTexture2 = glGetInteger(GL_TEXTURE_BINDING_2D);

        glActiveTexture(GL_TEXTURE3);
        int prevTexture3 = glGetInteger(GL_TEXTURE_BINDING_2D);

        glActiveTexture(GL_TEXTURE4);
        int prevTexture4 = glGetInteger(GL_TEXTURE_BINDING_2D);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, Text.getMenuFont().loadedTexture.getId());

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, Text.getMenuBoldFont().loadedTexture.getId());

        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, Text.getMenuBindsFont().loadedTexture.getId());

        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, Text.MENU_ICONS.loadedTexture.getId());

        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, frameBuffer.textureId);

        glUniform1i(glGetUniformLocation(programID, "menuFont"), 0);
        glUniform1i(glGetUniformLocation(programID, "menuBoldFont"), 1);
        glUniform1i(glGetUniformLocation(programID, "menuBindsFont"), 2);
        glUniform1i(glGetUniformLocation(programID, "menuIconsFont"), 3);
        glUniform1i(glGetUniformLocation(programID, "frameBufferTexture"), 4);

        int[] lastVAO = new int[1];
        glGetIntegerv(GL_VERTEX_ARRAY_BINDING, lastVAO);

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(lastVAO[0]);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, prevTexture0);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, prevTexture1);

        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, prevTexture2);

        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, prevTexture3);

        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, prevTexture4);

        glActiveTexture(boundTextureUnit0);

        glUseProgram(previousProgram);

        cleanUpBuffers();

    }

    private static boolean uploadBuffers() {

        if(v == 0)
            return false;

        v = i = 0;

        int[] lastVBO = new int[1];
        glGetIntegerv(GL_ARRAY_BUFFER_BINDING, lastVBO);

        int[] lastEBO = new int[1];
        glGetIntegerv(GL_ELEMENT_ARRAY_BUFFER_BINDING, lastEBO);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        ByteBuffer buffer = glMapBufferRange(GL_ARRAY_BUFFER, 0, (long) vertices.length * Float.BYTES,
                GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

        if (buffer != null) {
            FloatBuffer floatBuffer = buffer.asFloatBuffer();
            floatBuffer.put(vertices);
            glUnmapBuffer(GL_ARRAY_BUFFER);
        }

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        ByteBuffer buffer1 = glMapBufferRange(GL_ELEMENT_ARRAY_BUFFER, 0, (long) indices.length * Integer.BYTES,
                GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

        if (buffer1 != null) {
            IntBuffer intBuffer = buffer1.asIntBuffer();
            intBuffer.put(indices);
            glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);
        }

        glBindBuffer(GL_ARRAY_BUFFER, lastVBO[0]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastEBO[0]);

        return true;

    }

    private static void defaultBlendFunc() {

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    }

    private static void beginShaderProgram(int resolutionWidth, int resolutionHeight) {

        glUseProgram(programID);

        glUniform2f(glGetUniformLocation(programID, "renderResolution"), resolutionWidth, resolutionHeight);

    }

    private static void cleanUpBuffers() {

        Arrays.fill(vertices, 0);
        Arrays.fill(indices, 0);

    }

    private static final String rectangleVertexShaderSource = """
                        
            #version 330 core
                        
            layout (location = 0) in vec2 vertexPosition;
            layout (location = 1) in vec2 texturePosition;
                        
            layout (location = 2) in vec4 rectangleProperties;
                        
            layout (location = 3) in vec4 color1;
            layout (location = 4) in vec4 color2;
            layout (location = 5) in vec4 color3;
            layout (location = 6) in vec4 color4;

            layout (location = 7) in float shader;
                        
            layout (location = 8) in float globalAlpha;

            uniform vec2 renderResolution;

            out vec2 fragmentTexturePosition;

            out vec2 rectanglePosition;
            out vec2 rectangleSize;
                        
            out vec4 firstColor;
            out vec4 secondColor;
            out vec4 thirdColor;
            out vec4 fourthColor;
                        
            out float shaderId;
                        
            out float alpha;

            void main()
            {
                        
                vec2 normalizedPos = (vertexPosition / renderResolution) * 2.0 - 1.0;
                gl_Position = vec4(normalizedPos.x, normalizedPos.y, 0.0, 1.0);

                fragmentTexturePosition = texturePosition;

                rectanglePosition = rectangleProperties.xy;
                rectangleSize = rectangleProperties.zw;
                
                firstColor = color1;
                secondColor = color2;
                thirdColor = color3;
                fourthColor = color4;
                
                shaderId = shader;
                
                alpha = globalAlpha;
                
            }
                
            """;

    private static final String fragmentShaderSource = """
                        
            #version 330 core
               
            in vec2 fragmentTexturePosition;

            in vec2 rectanglePosition;
            in vec2 rectangleSize;
                        
            in vec4 firstColor;
            in vec4 secondColor;
            in vec4 thirdColor;
            in vec4 fourthColor;
                        
            in float shaderId;
                        
            in float alpha;
                       
            uniform vec2 renderResolution;
            
            uniform sampler2D menuFont;
            uniform sampler2D menuBoldFont;
            uniform sampler2D menuBindsFont;
            uniform sampler2D menuIconsFont;
            
            uniform sampler2D frameBufferTexture;
                        
            out vec4 FragColor;
                        
            void main() {
                       
                if (shaderId == 0) {
                
                    FragColor = vec4(firstColor.rgb, firstColor.a * alpha);
                
                } else if (shaderId == 1) { // color2 = size xd
                
                    float size = secondColor.r;
                
                    if (size > 0) {
                    
                        if (
                            gl_FragCoord.x >= rectanglePosition.x + size &&
                            gl_FragCoord.x <= rectanglePosition.x + rectangleSize.x - size &&
                            gl_FragCoord.y >= rectanglePosition.y + size &&
                            gl_FragCoord.y <= rectanglePosition.y + rectangleSize.y - size
                        ) {
                            discard;
                        }
                        
                        FragColor = vec4(firstColor.rgb, firstColor.a * alpha);
                        
                    }
                
                } else if (shaderId == 2) {

                    vec2 center = vec2(rectanglePosition.x + rectangleSize.x / 2, rectanglePosition.y + rectangleSize.y / 2);
                    vec2 halfSize = rectangleSize / 2.0;
                    
                    vec2 distVec = abs(gl_FragCoord.xy - center);
                    
                    float distance = length(distVec);

                    float circleAlpha = 1 - smoothstep(halfSize.x - 5, halfSize.x, distance);
                    
                    FragColor = vec4(firstColor.rgb, circleAlpha * alpha);
                     
                } else if (shaderId == 3) {
                    
                    float normalizedX = (gl_FragCoord.x - rectanglePosition.x) / rectangleSize.x;
                    float normalizedY = (gl_FragCoord.y - rectanglePosition.y) / rectangleSize.y;
                    
                    normalizedX = clamp(normalizedX, 0, 1);
                    normalizedY = clamp(normalizedY, 0, 1);
                    
                    FragColor = mix(
                        mix(vec4(firstColor.rgb, firstColor.a * alpha), vec4(secondColor.rgb, secondColor.a * alpha), normalizedX),
                        mix(vec4(thirdColor.rgb, thirdColor.a * alpha), vec4(fourthColor.rgb, fourthColor.a * alpha), normalizedX),
                        normalizedY
                    );
                
                } else if (shaderId == 4) {
                
                    vec2 pixelCoord = gl_FragCoord.xy + vec2(-1, 1);
             
                    bool isStripeColumn = int(pixelCoord.x) % 2 == 1;
                    float yShift = mod(floor(pixelCoord.x / 2.0), 2.0) * 2.0;
                    bool isBlackStripe = mod(pixelCoord.y + yShift, 4.0) < 3.0;
                    
                    if (isStripeColumn && isBlackStripe) {
                        FragColor = vec4(0.045, 0.045, 0.045, alpha);
                    } else {
                        FragColor = vec4(0.08, 0.08, 0.08, alpha);
                    }
                
                } else if (shaderId == 5) {
                
                    float normalizedX = (gl_FragCoord.x - rectanglePosition.x) / rectangleSize.x;
                    float normalizedY = (gl_FragCoord.y - rectanglePosition.y) / rectangleSize.y;
                    
                    normalizedX = clamp(normalizedX, 0, 1);
                    normalizedY = clamp(normalizedY, 0, 1);
                    
                    if (normalizedX < normalizedY) {
                        discard;
                    }
                    
                    FragColor = vec4(firstColor.rgb, firstColor.a * alpha);
                    
                } else if (shaderId == 6) {
                
                    float base = 1.0;
                    float height = 1.0;
                    
                    float normalizedX = (gl_FragCoord.x - rectanglePosition.x) / rectangleSize.x;
                    float normalizedY = (gl_FragCoord.y - rectanglePosition.y) / rectangleSize.y;
                    
                    normalizedX = clamp(normalizedX, 0, 1);
                    normalizedY = clamp(normalizedY, 0, 1);
                    
                    if (normalizedY >= abs(normalizedX - base / 2.0) * (height / (base / 2.0))) {
                        FragColor = vec4(firstColor.rgb, firstColor.a * alpha);
                    } else {
                        discard;
                    }
                
                } else if (shaderId == 7) {
                    
                    float font = secondColor.r; // again lol
                    vec4 sampled = texture(menuFont, fragmentTexturePosition);
                    
                    if (font == 1) {
                    
                        sampled = texture(menuBoldFont, fragmentTexturePosition);
                    
                    } else if (font == 2) {
                    
                        sampled = texture(menuBindsFont, fragmentTexturePosition);
                    
                    } else if (font == 3) {
                    
                        sampled = texture(menuIconsFont, fragmentTexturePosition);
                    
                    }
                    
                    FragColor = vec4(firstColor.rgb, firstColor.a * alpha) * sampled;
                    
                } else if (shaderId == 8) {
                
                    vec4 sampled = texture(frameBufferTexture, fragmentTexturePosition) * firstColor;
                    FragColor = sampled;
                    
                }
                        
            }
                        
            """;

}