package sisicat.main.utilities;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import static org.lwjgl.opengl.GL30.*;

public class LWJGLHelper {

    public float[] vertices;
    public int[] indices;

    public int verticesCount = 0;
    public int indicesCount = 0;

    public final int
            VAO, VBO, EBO;

    public final int shaderProgramId;

    public String
            vertexShader,
            fragmentShader;

    public LWJGLHelper(){

        VAO = glGenVertexArrays();
        VBO = glGenBuffers();
        EBO = glGenBuffers();

        shaderProgramId = glCreateProgram();

    }

    public void compileShaders() {

        try {

            int vertexShaderID = glCreateShader(GL_VERTEX_SHADER);
            int fragmentShaderID = glCreateShader(GL_FRAGMENT_SHADER);

            glShaderSource(vertexShaderID, vertexShader);
            glCompileShader(vertexShaderID);

            glShaderSource(fragmentShaderID, fragmentShader);
            glCompileShader(fragmentShaderID);

            glAttachShader(shaderProgramId, vertexShaderID);
            glAttachShader(shaderProgramId, fragmentShaderID);

            glLinkProgram(shaderProgramId);

            glDeleteShader(vertexShaderID);
            glDeleteShader(fragmentShaderID);

        } catch (Exception exception) {
            exception.fillInStackTrace();
        }

    }

    public void draw() {

        if(!uploadBuffers())
            return;

        int[] lastVAO = new int[1];
        glGetIntegerv(GL_VERTEX_ARRAY_BINDING, lastVAO);

        glBindVertexArray(VAO);
        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);

        glBindVertexArray(lastVAO[0]);

        cleanUpBuffers();

    }

    private void cleanUpBuffers() {

        Arrays.fill(vertices, 0);
        Arrays.fill(indices, 0);

    }

    private boolean uploadBuffers() {

        if(verticesCount == 0)
            return false;

        verticesCount = indicesCount = 0;

        /*int[] lastVBO = new int[1];
        glGetIntegerv(GL_ARRAY_BUFFER_BINDING, lastVBO);

        int[] lastEBO = new int[1];
        glGetIntegerv(GL_ELEMENT_ARRAY_BUFFER_BINDING, lastEBO);

        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        ByteBuffer buffer = glMapBufferRange(GL_ARRAY_BUFFER, 0, (long) vertices.length * Float.BYTES,
                GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

        if (buffer != null) {
            FloatBuffer floatBuffer = buffer.asFloatBuffer();
            floatBuffer.put(vertices);
            glUnmapBuffer(GL_ARRAY_BUFFER);
        }

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
        ByteBuffer buffer1 = glMapBufferRange(GL_ELEMENT_ARRAY_BUFFER, 0, (long) indices.length * Integer.BYTES,
                GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

        if (buffer1 != null) {
            IntBuffer intBuffer = buffer1.asIntBuffer();
            intBuffer.put(indices);
            glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);
        }

        glBindBuffer(GL_ARRAY_BUFFER, lastVBO[0]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastEBO[0]);*/

        return true;

    }

}
