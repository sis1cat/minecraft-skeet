package sisicat.main.functions.visual;

import com.darkmagician6.eventapi.EventTarget;
import com.darkmagician6.eventapi.types.Priority;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.CameraType;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import sisicat.events.EntityEvent;
import sisicat.events.World2DGraphics;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;
import sisicat.main.functions.combat.Rage;
import sisicat.main.gui.elements.Window;
import sisicat.main.utilities.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL30.*;

public class PlayerESPFunction extends Function {

    private final FunctionSetting
            shieldBreakSound = new FunctionSetting("Shield break sound");

    private final FunctionSetting

            visualizeAimbot = new FunctionSetting("Visualize aimbot"),
            visualizeAimbotColor = new FunctionSetting("Dot color", new float[]{220, 0.5f, 1, 0.4f}),

            boundingBox = new FunctionSetting("Bounding box"),
            bbBorderColor = new FunctionSetting("bb Border color", new float[]{215, 0.5f, 1f, 0.78f}),
            bbFirstFillColor = new FunctionSetting("bb First fill color", new float[]{220, 0, 1, 0.01f}),
            bbSecondFillColor = new FunctionSetting("bb Second fill color", new float[]{220, 0.5f, 1, 0.75f}),

            healthBar = new FunctionSetting("Health bar"),
            hbBorderColor = new FunctionSetting("hb Border color", new float[]{215, 0.5f, 1f, 0.78f}),
            hbFirstFillColor = new FunctionSetting("hb First fill color", new float[]{220, 0, 1, 0.01f}),
            hbSecondFillColor = new FunctionSetting("hb Second fill color", new float[]{220, 0.5f, 1, 0.75f}),
            hbTextColor = new FunctionSetting("hb Text color", new float[]{0f, 0f, 1f, 1f}),

            name = new FunctionSetting("Name"),
            nameColor = new FunctionSetting("Name color", new float[]{0f, 0f, 1f, 1f}),

            flags = new FunctionSetting("Flags"),
            flagsColor = new FunctionSetting("Flags color", new float[]{0f, 0f, 1f, 1f}),

            itemIcon = new FunctionSetting("Item icon"),
            itemIconColor = new FunctionSetting("Item icon color", new float[]{1f, 1f, 1f, 1f}),
            armorIcons = new FunctionSetting("Armor icons"),
            armorIconsColor = new FunctionSetting("Armor icons color", new float[]{1f, 1f, 1f, 1f});

    private final float[] vertices = new float[14 * 4 * 2000];
    private final int[] indices = new int[6 * 2000];

    private final int
            vao,
            vbo,
            ebo;

    private int shaderProgram;

    public PlayerESPFunction(String name) {
        super(name);

        this.addSetting(

                this.visualizeAimbot,
                this.visualizeAimbotColor,

                this.shieldBreakSound,

                this.boundingBox,
                this.bbBorderColor,
                this.bbFirstFillColor,
                this.bbSecondFillColor,

                this.healthBar,
                this.hbBorderColor,
                this.hbFirstFillColor,
                this.hbSecondFillColor,
                this.hbTextColor,

                this.name,
                this.nameColor,

                this.flags,
                this.flagsColor,

                this.itemIcon,
                this.itemIconColor,

                this.armorIcons,
                this.armorIconsColor

        );

        this.setCanBeActivated(true);

        shaderProgram = createShaderProgram();

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_DYNAMIC_DRAW);

        int stride = 14 * Float.BYTES;

        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 4, GL_FLOAT, false, stride, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glVertexAttribPointer(3, 1, GL_FLOAT, false, stride, 8 * Float.BYTES);
        glEnableVertexAttribArray(3);

        glVertexAttribPointer(4, 2, GL_FLOAT, false, stride, 9 * Float.BYTES);
        glEnableVertexAttribArray(4);

        glVertexAttribPointer(5, 2, GL_FLOAT, false, stride, 11 * Float.BYTES);
        glEnableVertexAttribArray(5);

        glVertexAttribPointer(6, 1, GL_FLOAT, false, stride, 13 * Float.BYTES);
        glEnableVertexAttribArray(6);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

    }

    private Vec3 aimbotDot = new Vec3(0, 0, 0);
    private float distance;

    private void drawAimbotDot() {

        if(Rage.targetDot == null) {

            aimbotDot = null;
            distance = 6;

            return;

        }

        if(aimbotDot == null)
            aimbotDot = Rage.targetDot;

        aimbotDot.x = Mth.lerp(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), aimbotDot.x, Rage.targetDot.x);
        aimbotDot.y = Mth.lerp(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), aimbotDot.y, Rage.targetDot.y);
        aimbotDot.z = Mth.lerp(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), aimbotDot.z, Rage.targetDot.z);

        double entityX = Mth.lerp(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), Rage.currentTarget.xOld, Rage.currentTarget.getX());
        double entityY = Mth.lerp(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), Rage.currentTarget.yOld, Rage.currentTarget.getY());
        double entityZ = Mth.lerp(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), Rage.currentTarget.zOld, Rage.currentTarget.getZ());

        final Vec3 finalDotPosition = new Vec3(aimbotDot.x + entityX, aimbotDot.y + entityY, aimbotDot.z + entityZ);

        Vec2 onScreenCoordinates = World.project(
                finalDotPosition.x, finalDotPosition.y, finalDotPosition.z
        );

        distance = Mth.lerp(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), distance, (float) finalDotPosition.distanceTo(mc.getEntityRenderDispatcher().camera.getPosition()));

        float size = (100f * ((mc.options.fov().get() <= 70f ? 70f : 70 / ((float)mc.gameRenderer.getFov(mc.getEntityRenderDispatcher().camera, mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), true) / 70f)) / (float)mc.gameRenderer.getFov(mc.getEntityRenderDispatcher().camera, mc.getDeltaTracker().getRealtimeDeltaTicks(), true))) * (6 / distance);

        Render.drawCircle((int)(onScreenCoordinates.x - size / 2), (int)(onScreenCoordinates.y - size / 2), (int)size, visualizeAimbotColor.getRGBAColor(), visualizeAimbotColor.getRGBAColor()[3]);
    }

    public void drawESP(LivingEntity livingEntity) {

        final double
                x = Mth.lerp(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), livingEntity.xOld, livingEntity.getX()),
                y = Mth.lerp(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), livingEntity.yOld, livingEntity.getY()),
                z = Mth.lerp(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), livingEntity.zOld, livingEntity.getZ());

        final Vec3 size =
                new Vec3(
                        livingEntity.getBoundingBox().maxX - livingEntity.getBoundingBox().minX + 0.1,
                        livingEntity.lerpedYAABBSize,
                        livingEntity.getBoundingBox().maxZ - livingEntity.getBoundingBox().minZ + 0.1
                );

        final AABB box =
                new AABB(
                        x - size.x / 2f, y, z - size.z / 2f,
                        x + size.x / 2f, y + size.y, z + size.z / 2f
                );

        final double
                centerX = (box.minX + box.maxX) / 2.0,
                centerZ = (box.minZ + box.maxZ) / 2.0;

        final double radius =
                Math.sqrt(
                        Math.pow((box.maxX - box.minX) / 2.0, 2) + Math.pow((box.maxZ - box.minZ) / 2.0, 2)
                );

        final double
                minY = box.minY,
                maxY = box.maxY;

        int sectors = 32;

        double angleStep = 2 * Math.PI / sectors;

        List<double[]> corners = new ArrayList<>();

        for (int i = 0; i < sectors; i++) {

            double angle = i * angleStep;

            double offsetX = radius * Math.cos(angle);
            double offsetZ = radius * Math.sin(angle);

            corners.add(new double[]{centerX + offsetX, minY, centerZ + offsetZ});
            corners.add(new double[]{centerX + offsetX, maxY, centerZ + offsetZ});

        }

        float minX = Float.MAX_VALUE;
        float minY2D = Float.MAX_VALUE;

        float maxX = -200;
        float maxY2D = -200;

        for (double[] corner : corners) {

            Vec2 screenPos = World.project(corner[0], corner[1], corner[2]);

            if (screenPos == null)
                continue;

            if(screenPos.y < -200)
                screenPos.y = -200;

            if(screenPos.y > Window.gameWindowSize.y + 200)
                screenPos.y = Window.gameWindowSize.y + 200;

            if(screenPos.x < -200)
                screenPos.x = -200;

            if(screenPos.x > Window.gameWindowSize.x + 200)
                screenPos.x = Window.gameWindowSize.x + 200;

            if (screenPos.x < minX)
                minX = screenPos.x;

            if (screenPos.y < minY2D)
                minY2D = screenPos.y;

            if (screenPos.x > maxX)
                maxX = screenPos.x;

            if (screenPos.y > maxY2D)
                maxY2D = screenPos.y;

        }

        if (minX < Float.MAX_VALUE && minY2D < Float.MAX_VALUE) {

            int lastBoxMinX = (int) minX;
            int lastBoxMinY = (int) minY2D;
            int lastBoxMaxX = (int) maxX;
            int lastBoxMaxY = (int) maxY2D;

            if(boundingBox.getCanBeActivated() || healthBar.getCanBeActivated())
                drawRectangle(lastBoxMinX - 6, lastBoxMinY, lastBoxMaxX, lastBoxMaxY, Mth.clamp(livingEntity.barHealth, 0, livingEntity.getMaxHealth()), livingEntity.getMaxHealth(), livingEntity.alpha);

            if(healthBar.getCanBeActivated()) {

                float maxBarSize = lastBoxMaxY - lastBoxMinY - 1;
                float barSize = maxBarSize / livingEntity.getMaxHealth() * livingEntity.barHealth;

                float healthY = Math.min(lastBoxMinY + 1 + (maxBarSize - barSize) - (int)(Text.getMenuFont().getFontHeight() / 2f), lastBoxMaxY - 1 - Text.getMenuFont().getFontHeight());

                healthY = Mth.clamp(healthY, lastBoxMinY + 1, lastBoxMaxY - Text.getMenuFont().getFontHeight());

                String health = Float.toString(Math.round(livingEntity.getHealth() * 10) / 10f);

                renderTextWithShadow(health, lastBoxMinX - 9 - Text.getMenuFont().getStringWidth(health), healthY, hbTextColor.getRGBAColor(), false, livingEntity.alpha);

            }

            Player player = (Player) livingEntity;

            if(this.name.getCanBeActivated()) {

                for(UpperAttribute upperAttribute : livingEntity.upperAttributes) {

                    if(upperAttribute instanceof NameAttribute nameAttribute) {

                        final Vec2 centerPos2D = World.project(livingEntity.getPosition(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)).x, livingEntity.getPosition(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)).y, livingEntity.getPosition(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)).z);

                        float centeredX = lastBoxMinX + (lastBoxMaxX - lastBoxMinX) / 2f;

                        int ypos = -(int) (double) (player.upperAttributes.indexOf(upperAttribute) * (this.armorIcons.getCanBeActivated() ? 16 : 0));
                        nameAttribute.draw((int) centeredX, lastBoxMinY - Text.getMenuFont().getFontHeight() - 4, ypos, nameColor.getRGBAColor(), this, livingEntity);
                    }
                }

            }

            if(this.itemIcon.getCanBeActivated()) {

                for(PlayerProperty prop : player.props) {

                    if(prop instanceof ItemProperty item) {
                        int ypos = (int) (double) (player.props.indexOf(prop) * (Text.getMenuFont().getFontHeight() + 3));
                        item.draw(lastBoxMaxX + 3, lastBoxMinY, ypos, itemIconColor.getRGBAColor(), this, player);
                    }

                }

            }

            if(this.flags.getCanBeActivated()) {

                for(PlayerProperty prop : player.props) {

                    if(prop instanceof ItemProperty)
                        continue;

                    int ypos = (int) (double) (player.props.indexOf(prop) * (Text.getMenuFont().getFontHeight() + 3));

                    prop.draw(lastBoxMaxX + 3, lastBoxMinY, ypos, flagsColor.getRGBAColor(), this, livingEntity);

                }

            }

            if(this.armorIcons.getCanBeActivated()) {

                for (UpperAttribute upperAttribute : livingEntity.upperAttributes) {

                    if (upperAttribute instanceof ArmorAttribute armorAttribute) {

                        int armorLineSize = armorAttribute.armor.size() * 14;

                        int ypos = -(int) (double) (player.upperAttributes.indexOf(upperAttribute) * (Text.getMenuFont().getFontHeight() + 3));
                        armorAttribute.draw(lastBoxMinX + (int) ((lastBoxMaxX - lastBoxMinX) / 2f), lastBoxMinY - 18, armorLineSize / 2, ypos, armorIconsColor.getRGBAColor(), this, livingEntity);
                    }

                }

            }

        }

    }

    public static class UpperAttribute {

        public int priority = 0;
        public float iy = -1;
        public final Animation animation = new Animation();

    }

    public static String getFormattedNameWithColors(String playerName) {

        ClientPacketListener connection = mc.getConnection();

        if (connection == null) return playerName;

        final PlayerInfo playerInfo = connection.getOnlinePlayers().stream()
                .filter(info -> info.getProfile().getName().equals(playerName))
                .findFirst().orElse(null);

        if (playerInfo == null)
            return playerName;

        Component nameComponent = playerInfo.getTabListDisplayName();

        if (nameComponent == null)
            return playerName;

        return convertComponentToLegacy(nameComponent);

    }

    private static String convertComponentToLegacy(Component component) {

        StringBuilder sb = new StringBuilder();

        component.visit((style, text) -> {

            TextColor color = style.getColor();

            if (color != null)
                sb.append(getMinecraftColorCode(color.getValue()));

            sb.append(text);

            return Optional.empty();

        }, Style.EMPTY);

        return sb.toString();

    }

    private static String getMinecraftColorCode(int hexColor) {

        return switch (hexColor) {
            case 0x000000 -> "\u00A70"; // Черный
            case 0x0000AA -> "\u00A71"; // Темно-синий
            case 0x00AA00 -> "\u00A72"; // Темно-зеленый
            case 0x00AAAA -> "\u00A73"; // Бирюзовый
            case 0xAA0000 -> "\u00A74"; // Темно-красный
            case 0xAA00AA -> "\u00A75"; // Фиолетовый
            case 0xFFAA00 -> "\u00A76"; // Оранжевый
            case 0xAAAAAA -> "\u00A77"; // Серый
            case 0x555555 -> "\u00A78"; // Темно-серый
            case 0x5555FF -> "\u00A79"; // Синий
            case 0x55FF55 -> "\u00A7a"; // Зеленый
            case 0x55FFFF -> "\u00A7b"; // Голубой
            case 0xFF5555 -> "\u00A7c"; // Красный
            case 0xFF55FF -> "\u00A7d"; // Розовый
            case 0xFFFF55 -> "\u00A7e"; // Желтый
            case 0xFFFFFF -> "\u00A7f"; // Белый
            default -> convertHexToMinecraftFormat(hexColor);
        };
    }

    private static String convertHexToMinecraftFormat(int hex) {
        return String.format("\u00A7x\u00A7%x\u00A7%x\u00A7%x\u00A7%x\u00A7%x\u00A7%x",
                (hex >> 20) & 0xF, (hex >> 16) & 0xF,
                (hex >> 12) & 0xF, (hex >> 8) & 0xF,
                (hex >> 4) & 0xF, hex & 0xF);
    }
    public static class NameAttribute extends UpperAttribute {

        public NameAttribute() {
            this.priority = 1;
        }

        public void draw(int x, int y, int yOffset, float[] color, PlayerESPFunction PESPF, LivingEntity livingEntity) {

            if(iy == -1)
                iy = yOffset;

            iy = animation.interpolate(iy, (float) yOffset, 50d);

            if(livingEntity instanceof Player player) {
                PESPF.renderTextWithShadow(getFormattedNameWithColors(player.getGameProfile().getName()), x, y + (float) Math.floor(iy), color, true, livingEntity.alpha);
            } else PESPF.renderTextWithShadow(livingEntity.getName().getString(), x, y + (float) Math.floor(iy), color, true, livingEntity.alpha);

        }

    }

    public static class ArmorAttribute extends UpperAttribute {

        public ArrayList<ArmorElement> armor = new ArrayList<>();

        public ArmorAttribute() {
            this.priority = 0;
        }

        private float ix = -1;
        private final Animation animation1 = new Animation();
        public void draw(int x, int y, int xOffset, int yOffset, float[] color, PlayerESPFunction PESPF, LivingEntity livingEntity) {

            if(iy == -1)
                iy = yOffset;

            if(ix == -1)
                ix = xOffset;

            iy = animation.interpolate(iy, (float) yOffset, 50d);
            ix = animation1.interpolate(ix, (float) xOffset, 50d);

            for(ArmorElement armorElement : armor) {
                int xOffset1 = armor.indexOf(armorElement) * 14;

                armorElement.draw(x - (int) Math.floor(ix), y + (int) Math.floor(iy), xOffset1, color, PESPF, livingEntity);
            }
        }

        public static class ArmorElement {

            float ix = -1;
            private final Animation animation = new Animation();
            public final ItemStack armorElement;

            public int priority = 0;

            public ArmorElement(ItemStack armorElement) {
                this.armorElement = armorElement;

                if(armorElement.getItem().toString().contains("helmet"))
                    priority = 0;
                else if(armorElement.getItem().toString().contains("chestplate"))
                    priority = 1;
                else if(armorElement.getItem().toString().contains("leggings"))
                    priority = 2;
                else if(armorElement.getItem().toString().contains("boots"))
                    priority = 3;

            }

            public void draw(int x, int y, int xOffset, float[] color, PlayerESPFunction PESPF, LivingEntity livingEntity) {

                if(ix == -1)
                    ix = xOffset;

                ix = animation.interpolate(ix, (float) xOffset, 50d);

                PESPF.drawItem16(armorElement, x + (float) Math.floor(ix), y, color, livingEntity.alpha);

            }

        }

    }

    public static class PlayerProperty {

        public final String name;
        public float iy = -1;
        public final Animation animation = new Animation();

        public PlayerProperty(String name) {
            this.name = name;
        }

        public void draw(int x, int y, int yOffset, float[] color, PlayerESPFunction PESPF, LivingEntity livingEntity) {

            if(iy == -1)
                iy = yOffset;

            iy = animation.interpolate(iy, (float) yOffset, 50d);

            PESPF.renderTextWithShadow(name.toLowerCase(), (float) x, y + (float) Math.floor(iy), color, false, livingEntity.alpha);

        }

    }

    public static class ProtectedProperty extends PlayerProperty {

        public ProtectedProperty() {
            super("Protected");
        }

    }

    public static class InvulnerableProperty extends PlayerProperty {

        public InvulnerableProperty() {
            super("Invulnerable");
        }

    }

    public static class TargetProperty extends PlayerProperty {

        public TargetProperty() {
            super("Target");
        }

    }

    public static class CrouchingProperty extends PlayerProperty {

        public CrouchingProperty() {
            super("Crouching");
        }

    }

    public static class BlockingProperty extends PlayerProperty {

        public BlockingProperty() {
            super("Blocking");
        }

    }

    public static class ChargingProperty extends PlayerProperty {

        public ChargingProperty() {
            super("Charging");
        }

    }

    public static class ItemProperty extends PlayerProperty {

        public ItemProperty() {
            super("!");
        }

        public void draw(int x, int y, int yOffset, float[] color, PlayerESPFunction PESPF, Player player) {

            if(iy == -1)
                iy = yOffset;

            iy = animation.interpolate(iy, (float) yOffset, 50d);


            PESPF.drawItem(player.getMainHandItem(), (float) x, y + (float) Math.floor(iy), color, player.alpha);

        }

    }

    private int
            v = 0,
            i = 0;

    public void drawCharacter(char c, float x, float y, float[] color, float alpha) {

        if(vertices.length - v < 60)
            return;

        int[] charData = Text.getMenuFont().charactersMap.get(c);
        int charX = charData[0];
        int charY = charData[1];
        int charWidth = charData[2];
        int charHeight = charData[3];

        float tx = (float) charX / 2048;
        float ty = (float) (charY + charHeight) / 2048;
        float tx2 = (float) (charX + charWidth) / 2048;
        float ty2 = (float) charY / 2048;

        color = convertColor(color);

        x = (float) Math.floor(x);
        y = (float) Math.floor(y);

        // vertex 1

        vertices[v++] = x;
        vertices[v++] = y;

        vertices[v++] = color[0];
        vertices[v++] = color[1];
        vertices[v++] = color[2];
        vertices[v++] = color[3];
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 1;
        vertices[v++] = tx;
        vertices[v++] = ty;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = (float) Math.floor(alpha);

        // vertex 2

        vertices[v++] = x + charWidth;
        vertices[v++] = y;

        vertices[v++] = color[0];
        vertices[v++] = color[1];
        vertices[v++] = color[2];
        vertices[v++] = color[3];
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 1;
        vertices[v++] = tx2;
        vertices[v++] = ty;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = (float) Math.floor(alpha);

        // vertex 3

        vertices[v++] = x + charWidth;
        vertices[v++] = y + charHeight;

        vertices[v++] = color[0];
        vertices[v++] = color[1];
        vertices[v++] = color[2];
        vertices[v++] = color[3];
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 1;
        vertices[v++] = tx2;
        vertices[v++] = ty2;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = (float) Math.floor(alpha);

        // vertex 4

        vertices[v++] = x;
        vertices[v++] = y + charHeight;

        vertices[v++] = color[0];
        vertices[v++] = color[1];
        vertices[v++] = color[2];
        vertices[v++] = color[3];
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 1;
        vertices[v++] = tx;
        vertices[v++] = ty2;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = (float) Math.floor(alpha);


        int start = v / 14 - 4;

        indices[i++] = start;
        indices[i++] = 1 + start;
        indices[i++] = 2 + start;
        indices[i++] = 2 + start;
        indices[i++] = 3 + start;
        indices[i++] = start;


    }

    public void drawOutlinedCharacter(char c, float x, float y, float[] color, float alpha) {

        if(vertices.length - v < 60)
            return;

        int[] charData = Text.getMenuFont().charactersMap.get(c);
        int charX = charData[0];
        int charY = charData[1];
        int charWidth = charData[2];
        int charHeight = charData[3] + 1; // for outline lower pixels

        y -= 1; // compensate

        float tx = (float) charX / 2048;
        float ty = (float) (charY + charHeight) / 2048;
        float tx2 = (float) (charX + charWidth) / 2048;
        float ty2 = (float) (charY) / 2048;

        int resolutionHeight = Window.gameWindowHeight;

        color = convertColor(color);

        x = (float) Math.floor(x);
        y = (float) Math.floor(y);

        // vertex 1

        vertices[v++] = x;
        vertices[v++] = y;

        vertices[v++] = color[0];
        vertices[v++] = color[1];
        vertices[v++] = color[2];
        vertices[v++] = color[3];
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 3;
        vertices[v++] = tx;
        vertices[v++] = ty;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = (float) Math.floor(alpha);

        // vertex 2

        vertices[v++] = x + charWidth;
        vertices[v++] = y;

        vertices[v++] = color[0];
        vertices[v++] = color[1];
        vertices[v++] = color[2];
        vertices[v++] = color[3];
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 3;
        vertices[v++] = tx2;
        vertices[v++] = ty;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = (float) Math.floor(alpha);

        // vertex 3

        vertices[v++] = x + charWidth;
        vertices[v++] = y + charHeight;

        vertices[v++] = color[0];
        vertices[v++] = color[1];
        vertices[v++] = color[2];
        vertices[v++] = color[3];
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 3;
        vertices[v++] = tx2;
        vertices[v++] = ty2;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = (float) Math.floor(alpha);

        // vertex 4

        vertices[v++] = x;
        vertices[v++] = y + charHeight;

        vertices[v++] = color[0];
        vertices[v++] = color[1];
        vertices[v++] = color[2];
        vertices[v++] = color[3];
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 3;
        vertices[v++] = tx;
        vertices[v++] = ty2;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = (float) Math.floor(alpha);


        int start = v / 14 - 4;

        indices[i++] = start;
        indices[i++] = 1 + start;
        indices[i++] = 2 + start;
        indices[i++] = 2 + start;
        indices[i++] = 3 + start;
        indices[i++] = start;


    }

    public boolean isItemEnchanted(ItemStack itemStack) {

        return itemStack.isEnchanted();

    }

    public void drawItem(ItemStack itemStack, float x, float y, float[] color, float alpha) {

        if(vertices.length - v < 60)
            return;

        float health = isItemEnchanted(itemStack) ? -0.1f : 0f;
        float maxHealth = 0;

        String itemName = itemStack.getItem().toString();

        if(itemName.equals("air"))
            return;

        y = Window.gameWindowSize.y - y - 32;

        ItemsBuffer.ItemTexture itemData = ItemsBuffer.texturesMap.get(itemName);

        if(itemData == null)
            return;

        int itemX = itemData.x();
        int itemY = itemData.y();

        int itemSize = 32;

        float tx = (float) itemX / 1440;
        float ty = (float) (itemY + itemSize) / 1440;
        float tx2 = (float) (itemX + itemSize) / 1440;
        float ty2 = (float) itemY / 1440;

        double radius = 0.5;

        double angle = (System.currentTimeMillis() / 2000.0) % (2 * Math.PI);

        double x1 = 0.5 + Math.cos(angle) * radius;
        double y1 = 0.5 + Math.sin(angle) * radius;

        int ex = (int) (x1 * (128 - 16));
        int ey = (int) (y1 * (128 - 16));

        float etx = (float) ex / 128;
        float ety = (float) (ey + 16) / 128;
        float etx2 = (float) (ex + 16) / 128;
        float ety2 = (float) ey / 128;

        color = convertColor(color);

        if(itemStack.getItem() instanceof LingeringPotionItem) {
            health = -3;
            maxHealth = (float) itemStack.get(DataComponents.POTION_CONTENTS).getColor();
            tx = ty2 = 0;
            ty = tx2 = 1;
        } else if (itemStack.getItem() instanceof SplashPotionItem) {
            health = -2;
            maxHealth = (float) itemStack.get(DataComponents.POTION_CONTENTS).getColor();
            tx = ty2 = 0;
            ty = tx2 = 1;
        } else if (itemStack.getItem() instanceof PotionItem) {
            health = -1;
            maxHealth = (float) itemStack.get(DataComponents.POTION_CONTENTS).getColor();

            tx = ty2 = 0;
            ty = tx2 = 1;
        }

        // vertex 1

        vertices[v++] = x;
        vertices[v++] = y;

        vertices[v++] = color[0];
        vertices[v++] = color[1];
        vertices[v++] = color[2];
        vertices[v++] = color[3];
        vertices[v++] = health;
        vertices[v++] = maxHealth;
        vertices[v++] = 2;
        vertices[v++] = tx;
        vertices[v++] = ty;
        vertices[v++] = etx;
        vertices[v++] = ety;
        vertices[v++] = (float) Math.floor(alpha);

        // vertex 2

        vertices[v++] = x + itemSize;
        vertices[v++] = y;

        vertices[v++] = color[0];
        vertices[v++] = color[1];
        vertices[v++] = color[2];
        vertices[v++] = color[3];
        vertices[v++] = health;
        vertices[v++] = maxHealth;
        vertices[v++] = 2;
        vertices[v++] = tx2;
        vertices[v++] = ty;
        vertices[v++] = etx2;
        vertices[v++] = ety;
        vertices[v++] = (float) Math.floor(alpha);

        // vertex 3

        vertices[v++] = x + itemSize;
        vertices[v++] = y + itemSize;

        vertices[v++] = color[0];
        vertices[v++] = color[1];
        vertices[v++] = color[2];
        vertices[v++] = color[3];
        vertices[v++] = health;
        vertices[v++] = maxHealth;
        vertices[v++] = 2;
        vertices[v++] = tx2;
        vertices[v++] = ty2;
        vertices[v++] = etx2;
        vertices[v++] = ety2;
        vertices[v++] = (float) Math.floor(alpha);

        // vertex 4

        vertices[v++] = x;
        vertices[v++] = y + itemSize;

        vertices[v++] = color[0];
        vertices[v++] = color[1];
        vertices[v++] = color[2];
        vertices[v++] = color[3];
        vertices[v++] = health;
        vertices[v++] = maxHealth;
        vertices[v++] = 2;
        vertices[v++] = tx;
        vertices[v++] = ty2;
        vertices[v++] = etx;
        vertices[v++] = ety2;
        vertices[v++] = (float) Math.floor(alpha);


        int start = v / 14 - 4;

        indices[i++] = start;
        indices[i++] = 1 + start;
        indices[i++] = 2 + start;
        indices[i++] = 2 + start;
        indices[i++] = 3 + start;
        indices[i++] = start;


    }

    public void drawItem16(ItemStack itemStack, float x, float y, float[] color, float alpha) {

        if(vertices.length - v < 60)
            return;

        float health = isItemEnchanted(itemStack) ? -0.1f : 0f;
        float maxHealth = 0;

        String itemName = itemStack.getItem().toString();

        if(itemName.equals("air"))
            return;

        y = Window.gameWindowSize.y - y - 16;

        ItemsBuffer.ItemTexture itemData = ItemsBuffer.texturesMap.get(itemName);

        if(itemData == null)
            return;

        int itemX = itemData.x() / 2;
        int itemY = itemData.y() / 2;

        int itemSize = 16;

        float tx = (float) itemX / 720;
        float ty = (float) (itemY + itemSize) / 720;
        float tx2 = (float) (itemX + itemSize) / 720;
        float ty2 = (float) itemY / 720;

        double radius = 0.5;

        double angle = (System.currentTimeMillis() / 2000.0) % (2 * Math.PI);

        double x1 = 0.5 + Math.cos(angle) * radius;
        double y1 = 0.5 + Math.sin(angle) * radius;

        int ex = (int) (x1 * (128 - 16));
        int ey = (int) (y1 * (128 - 16));

        float etx = (float) ex / 128;
        float ety = (float) (ey + 16) / 128;
        float etx2 = (float) (ex + 16) / 128;
        float ety2 = (float) ey / 128;

        int resolutionHeight = Window.gameWindowHeight;

        color = convertColor(color);

        if(itemStack.getItem() instanceof LingeringPotionItem lingeringPotionItem) {
            health = -3;
            maxHealth = (float) lingeringPotionItem.getDefaultInstance().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor();
            tx = ty2 = 0;
            ty = tx2 = 1;
        } else if (itemStack.getItem() instanceof SplashPotionItem splashPotionItem) {
            health = -2;
            maxHealth = (float) splashPotionItem.getDefaultInstance().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor();
            tx = ty2 = 0;
            ty = tx2 = 1;
        } else if (itemStack.getItem() instanceof PotionItem potionItem) {
            health = -1;
            maxHealth = (float) potionItem.getDefaultInstance().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor();
            tx = ty2 = 0;
            ty = tx2 = 1;
        }

        // vertex 1

        vertices[v++] = x;
        vertices[v++] = y;

        vertices[v++] = color[0];
        vertices[v++] = color[1];
        vertices[v++] = color[2];
        vertices[v++] = color[3];
        vertices[v++] = health;
        vertices[v++] = maxHealth;
        vertices[v++] = 2;
        vertices[v++] = tx;
        vertices[v++] = ty;
        vertices[v++] = etx;
        vertices[v++] = ety;
        vertices[v++] = (float) Math.floor(alpha);

        // vertex 2

        vertices[v++] = x + itemSize;
        vertices[v++] = y;

        vertices[v++] = color[0];
        vertices[v++] = color[1];
        vertices[v++] = color[2];
        vertices[v++] = color[3];
        vertices[v++] = health;
        vertices[v++] = maxHealth;
        vertices[v++] = 2;
        vertices[v++] = tx2;
        vertices[v++] = ty;
        vertices[v++] = etx2;
        vertices[v++] = ety;
        vertices[v++] = (float) Math.floor(alpha);

        // vertex 3

        vertices[v++] = x + itemSize;
        vertices[v++] = y + itemSize;

        vertices[v++] = color[0];
        vertices[v++] = color[1];
        vertices[v++] = color[2];
        vertices[v++] = color[3];
        vertices[v++] = health;
        vertices[v++] = maxHealth;
        vertices[v++] = 2;
        vertices[v++] = tx2;
        vertices[v++] = ty2;
        vertices[v++] = etx2;
        vertices[v++] = ety2;
        vertices[v++] = (float) Math.floor(alpha);

        // vertex 4

        vertices[v++] = x;
        vertices[v++] = y + itemSize;

        vertices[v++] = color[0];
        vertices[v++] = color[1];
        vertices[v++] = color[2];
        vertices[v++] = color[3];
        vertices[v++] = health;
        vertices[v++] = maxHealth;
        vertices[v++] = 2;
        vertices[v++] = tx;
        vertices[v++] = ty2;
        vertices[v++] = etx;
        vertices[v++] = ety2;
        vertices[v++] = (float) Math.floor(alpha);


        int start = v / 14 - 4;

        indices[i++] = start;
        indices[i++] = 1 + start;
        indices[i++] = 2 + start;
        indices[i++] = 2 + start;
        indices[i++] = 3 + start;
        indices[i++] = start;


    }

    public void renderText(String text, float x, float y, float[] color, boolean minecraftColored, float alpha, boolean outlined) {

        if (vertices.length - v < 56) {
            //IDefault.displayClientChatMessage("ESP: Too many objects for render.");
            return;
        }

        y = Window.gameWindowSize.y - y - Text.getMenuFont().fontMetrics.getHeight() - Text.getMenuFont().yOffset;

        float[] lastColor = new float[]{color[0], color[1], color[2], color[3]};

        boolean isNextColor = false;
        boolean isHexColor = false;

        int hexColorIndex = 0;
        StringBuilder hexColor = new StringBuilder(0);

        for (int i = 0; i < text.length(); i++) {

            char c = text.charAt(i);

            if (isHexColor && minecraftColored) {
                if (hexColorIndex < 6) {

                    if (c == '\u00A7')
                        continue;

                    hexColor.append(c);
                    hexColorIndex++;

                    if (hexColorIndex == 6) {
                        int intHexColor = Integer.parseInt(hexColor.toString(), 16);
                        lastColor[0] = (float) ((intHexColor >> 16) & 0xFF);
                        lastColor[1] = (float) ((intHexColor >> 8) & 0xFF);
                        lastColor[2] = (float) (intHexColor & 0xFF);
                        isHexColor = false;
                    }
                }
                continue;
            }

            if (isNextColor && minecraftColored) {
                if (c == 'x') {
                    isHexColor = true;
                    hexColorIndex = 0;
                    hexColor = new StringBuilder(0);
                } else {
                    lastColor =
                        switch (c) {
                            case 'a' -> new float[]{85, 255, 85, lastColor[3]};
                            case 'b' -> new float[]{85, 255, 255, lastColor[3]};
                            case 'c' -> new float[]{255, 85, 85, lastColor[3]};
                            case 'd' -> new float[]{255, 85, 255, lastColor[3]};
                            case 'e' -> new float[]{255, 255, 85, lastColor[3]};
                            case 'f' -> new float[]{color[0], color[1], color[2], lastColor[3]};
                            case 'l', 'r' -> new float[]{color[0], color[1], color[2], lastColor[3]};
                            case '0' -> new float[]{0, 0, 0, lastColor[3]};
                            case '1' -> new float[]{0, 0, 170, lastColor[3]};
                            case '2' -> new float[]{0, 170, 0, lastColor[3]};
                            case '3' -> new float[]{0, 170, 170, lastColor[3]};
                            case '4' -> new float[]{170, 0, 0, lastColor[3]};
                            case '5' -> new float[]{170, 0, 170, lastColor[3]};
                            case '6' -> new float[]{255, 170, 0, lastColor[3]};
                            case '7' -> new float[]{170, 170, 170, lastColor[3]};
                            case '8' -> new float[]{85, 85, 85, lastColor[3]};
                            case '9' -> new float[]{85, 85, 255, lastColor[3]};
                            default -> lastColor;
                        };
                }
                isNextColor = false;
                continue;
            }

            if (c == '\u00A7') {
                isNextColor = true;
                continue;
            }

            int[] charData = Text.getMenuFont().charactersMap.get(c);

            if (charData == null) {
                c = '?';
                charData = Text.getMenuFont().charactersMap.get(c);
            }

            if (c == 9889) {
                c = '\u03de';
                charData = Text.getMenuFont().charactersMap.get(c);
            }

            if (outlined)
                drawOutlinedCharacter(c, (int) x, (int) y, lastColor, alpha);
            else
                drawCharacter(c, (int) x, (int) y, lastColor, alpha);

            x += charData[2];

        }

    }

    public void renderTextWithShadow(String text, float x, float y, float[] color, boolean centered, float alpha){

        if(Window.windowScale == 1) {
            renderOutlinedText(text, x, y, color, Color.c12, centered, true, alpha);
            return;
        }

        renderText(Font.removeParagraphPairs(text), x + (centered ? -Text.getMenuFont().getStringWidth(text) / 2f : 0) + 1, y + 1, new float[]{12, 12, 12, color[3]}, false, alpha, false);
        renderText(text, x + (centered ? -Text.getMenuFont().getStringWidth(text) / 2f : 0) , y , color, true, alpha, false);

    }

    public void renderOutlinedText(String text, float x, float y, float[] textColor, float[] outlineColor, boolean centered, boolean minecraftColored, float alpha){
        outlineColor[3] = 1000;
        x += (centered ? -Text.getMenuFont().getStringWidth(text) / 2f : 0);

        //float outlineAlpha = (float) Math.pow(alpha / 255f, 2) * 255f;

        this.renderText(text, x, y, textColor, minecraftColored, alpha, true);

    }

    public void drawRectangle(float x1, float y1, float x2, float y2, float health, float maxHealth, float alpha) {

        if(vertices.length - v < 60)
            return;

        float width = x2 - x1;
        float height = y2 - y1;

        vertices[v++] = x1;
        vertices[v++] = Window.gameWindowSize.y - y1;

        vertices[v++] = x1;
        vertices[v++] = Window.gameWindowSize.y - y1 - height;
        vertices[v++] = width;
        vertices[v++] = height;
        vertices[v++] = health;
        vertices[v++] = maxHealth;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = (float) Math.floor(alpha);


        vertices[v++] = x1;
        vertices[v++] = Window.gameWindowSize.y - y1 - height;

        vertices[v++] = x1;
        vertices[v++] = Window.gameWindowSize.y - y1 - height;
        vertices[v++] = width;
        vertices[v++] = height;
        vertices[v++] = health;
        vertices[v++] = maxHealth;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = (float) Math.floor(alpha);


        vertices[v++] = x2;
        vertices[v++] = Window.gameWindowSize.y - y1 - height;

        vertices[v++] = x1;
        vertices[v++] = Window.gameWindowSize.y - y1 - height;
        vertices[v++] = width;
        vertices[v++] = height;
        vertices[v++] = health;
        vertices[v++] = maxHealth;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = (float) Math.floor(alpha);


        vertices[v++] = x2;
        vertices[v++] = Window.gameWindowSize.y - y1;

        vertices[v++] = x1;
        vertices[v++] = Window.gameWindowSize.y - y1 - height;
        vertices[v++] = width;
        vertices[v++] = height;
        vertices[v++] = health;
        vertices[v++] = maxHealth;
        vertices[v++] = 0;
        vertices[v++] = (float) Math.floor(alpha);
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = 0;
        vertices[v++] = (float) Math.floor(alpha);


        int start = v / 14 - 4;

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

    @EventTarget
    void _event(EntityEvent entityEvent){

        if(!shieldBreakSound.isActivated())
            return;

        if(entityEvent.Id == 29)
            mc.level.playLocalSound(entityEvent.livingEntity.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.BLOCKS, 1F, 0.8F + mc.level.random.nextFloat() * 0.4F, false);
        else if (entityEvent.Id == 30)
            mc.level.playLocalSound(entityEvent.livingEntity.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.BLOCKS, 0.8F, 0.8F + mc.level.random.nextFloat() * 0.4F, false);

    }
    public static ArrayList<LivingEntity> removedEntities = new ArrayList<>();
    @EventTarget(value = Priority.LOWEST)
    void _event(World2DGraphics ignored){

        if(mc.level == null)
            return;

        ArrayList<AbstractClientPlayer> players = new ArrayList<>(mc.level.players());

        players.sort ((player1, player2) -> {

            double
                    distance1 = player1.position().distanceTo(mc.getEntityRenderDispatcher().camera.getPosition()),
                    distance2 = player2.position().distanceTo(mc.getEntityRenderDispatcher().camera.getPosition());

            return Double.compare(distance2, distance1);

        });

        for(Player player : mc.level.players()) {
            Rage.fixHealth(player);
            player.barHealth = player.barAnimation.interpolate(player.barHealth, player.getHealth(), 50d);
            player.alpha = player.alphaAnimation.interpolate(player.alpha, 255, 100d);
            player.lerpedYAABBSize = player.yAABBSizeAnimation.interpolate(
                    player.lerpedYAABBSize,
                    (float)(player.getBoundingBox().maxY - player.getBoundingBox().minY + 0.1),
                    100d
            );
        }

        Iterator<LivingEntity> iterator = removedEntities.iterator();
        while (iterator.hasNext()) {
            LivingEntity livingEntity = iterator.next();
            livingEntity.alpha = livingEntity.alphaAnimation.interpolate(livingEntity.alpha, 0, 100d);
            if (livingEntity.alpha <= 1) {
                iterator.remove();
            }
        }


            for (Player player : players)
                if ((player.is(mc.player) || !Rage.isBot(player)) && !(player.is(mc.player) && mc.options.getCameraType() == CameraType.FIRST_PERSON))
                    drawESP(player);

            for (LivingEntity livingEntity : removedEntities)
                if (livingEntity instanceof Player player && (player.is(mc.player) || !Rage.isBot(player)) && !(player.is(mc.player) && mc.options.getCameraType() == CameraType.FIRST_PERSON))
                    drawESP(player);

        if(v == 0) {
            if(visualizeAimbot.getCanBeActivated()) {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                drawAimbotDot();
                Render.drawAll();
            }
            return;
        }

        v = i = 0;

        int[] lastVBO = new int[1];
        glGetIntegerv(GL_ARRAY_BUFFER_BINDING, lastVBO);

        int[] lastEBO = new int[1];
        glGetIntegerv(GL_ELEMENT_ARRAY_BUFFER_BINDING, lastEBO);


        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        ByteBuffer buffer = glMapBufferRange(GL_ARRAY_BUFFER, 0, (long) vertices.length * Float.BYTES,
                GL_MAP_WRITE_BIT | GL_MAP_UNSYNCHRONIZED_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

        if (buffer != null) {
            FloatBuffer floatBuffer = buffer.asFloatBuffer();
            floatBuffer.put(vertices);
            glUnmapBuffer(GL_ARRAY_BUFFER);
        }

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        ByteBuffer buffer1 = glMapBufferRange(GL_ELEMENT_ARRAY_BUFFER, 0, (long) indices.length * Integer.BYTES,
                GL_MAP_WRITE_BIT | GL_MAP_UNSYNCHRONIZED_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

        if (buffer1 != null) {
            IntBuffer intBuffer = buffer1.asIntBuffer();
            intBuffer.put(indices);
            glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);
        }


        glBindBuffer(GL_ARRAY_BUFFER, lastVBO[0]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastEBO[0]);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);

        glUseProgram(shaderProgram);

        int boundTextureUnit0 = glGetInteger(GL_ACTIVE_TEXTURE);

        glActiveTexture(GL_TEXTURE0);
        int prevTexture0 = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, Text.getMenuFont().loadedTexture.getId());
        glUniform1i(glGetUniformLocation(shaderProgram, "menuFont"), 0);

        glActiveTexture(GL_TEXTURE1);
        int prevTexture1 = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, ItemsBuffer.texturesAtlas);
        glUniform1i(glGetUniformLocation(shaderProgram, "itemsAtlas"), 1);

        glActiveTexture(GL_TEXTURE2);
        int prevTexture2 = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, ItemsBuffer.potionTexture.getId());
        glUniform1i(glGetUniformLocation(shaderProgram, "potion"), 2);

        glActiveTexture(GL_TEXTURE3);
        int prevTexture3 = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, ItemsBuffer.splashPotionTexture.getId());
        glUniform1i(glGetUniformLocation(shaderProgram, "splashPotion"), 3);

        glActiveTexture(GL_TEXTURE4);
        int prevTexture4 = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, ItemsBuffer.lingeringPotionTexture.getId());
        glUniform1i(glGetUniformLocation(shaderProgram, "lingeringPotion"), 4);

        glActiveTexture(GL_TEXTURE5);
        int prevTexture5 = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, ItemsBuffer.potionOverlayTexture.getId());
        glUniform1i(glGetUniformLocation(shaderProgram, "potionOverlay"), 5);

        glActiveTexture(GL_TEXTURE6);
        int prevTexture6 = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, ItemsBuffer.enchantmentTexture.getId());
        glUniform1i(glGetUniformLocation(shaderProgram, "enchantment"), 6);

        glUniform2f(glGetUniformLocation(shaderProgram, "renderResolution"), Window.gameWindowSize.x, Window.gameWindowSize.y);

        glUniform4f(
                glGetUniformLocation(shaderProgram, "bbBorderColor"),
                !boundingBox.getCanBeActivated() ? -1 : bbBorderColor.getRGBAColor()[0] / 255f, bbBorderColor.getRGBAColor()[1] / 255f,
                bbBorderColor.getRGBAColor()[2] / 255f, bbBorderColor.getRGBAColor()[3] / 255f

        );

        glUniform4f(
                glGetUniformLocation(shaderProgram, "bbFirstColor"),
                bbFirstFillColor.getRGBAColor()[0] / 255f, bbFirstFillColor.getRGBAColor()[1] / 255f,
                bbFirstFillColor.getRGBAColor()[2] / 255f, bbFirstFillColor.getRGBAColor()[3] / 255f

        );

        glUniform4f(
                glGetUniformLocation(shaderProgram, "bbSecondColor"),
                bbSecondFillColor.getRGBAColor()[0] / 255f, bbSecondFillColor.getRGBAColor()[1] / 255f,
                bbSecondFillColor.getRGBAColor()[2] / 255f, bbSecondFillColor.getRGBAColor()[3] / 255f
        );


        glUniform4f(
                glGetUniformLocation(shaderProgram, "hbBorderColor"),
                !healthBar.getCanBeActivated() ? -1 : hbBorderColor.getRGBAColor()[0] / 255f, hbBorderColor.getRGBAColor()[1] / 255f,
                hbBorderColor.getRGBAColor()[2] / 255f, hbBorderColor.getRGBAColor()[3] / 255f

        );

        glUniform4f(
                glGetUniformLocation(shaderProgram, "hbFirstColor"),
                hbFirstFillColor.getRGBAColor()[0] / 255f, hbFirstFillColor.getRGBAColor()[1] / 255f,
                hbFirstFillColor.getRGBAColor()[2] / 255f, hbFirstFillColor.getRGBAColor()[3] / 255f

        );

        glUniform4f(
                glGetUniformLocation(shaderProgram, "hbSecondColor"),
                hbSecondFillColor.getRGBAColor()[0] / 255f, hbSecondFillColor.getRGBAColor()[1] / 255f,
                hbSecondFillColor.getRGBAColor()[2] / 255f, hbSecondFillColor.getRGBAColor()[3] / 255f
        );

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

        glActiveTexture(GL_TEXTURE5);
        glBindTexture(GL_TEXTURE_2D, prevTexture5);

        glActiveTexture(GL_TEXTURE6);
        glBindTexture(GL_TEXTURE_2D, prevTexture6);

        glActiveTexture(boundTextureUnit0);

        if(glIsProgram(previousProgram))
            glUseProgram(previousProgram);

        Arrays.fill(vertices, 0);
        Arrays.fill(indices, 0);

        if(visualizeAimbot.getCanBeActivated()) {
            drawAimbotDot();
            Render.drawAll();
        }

    }

    private int createShaderProgram() {

        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, espBoxVertexShaderSource);
        glCompileShader(vertexShader);

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, espBoxFragmentShaderSource);
        glCompileShader(fragmentShader);

        int shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return shaderProgram;

    }

    private String espBoxFragmentShaderSource =

            """
                    #version 330 core
                    
                    out vec4 FragColor;
                    
                    in vec2 rectanglePosition;
                    in vec2 rectangleSize;
                    
                    in vec2 playerHealth;
                    
                    in float shaderId;
                    in vec2 fragTex;
                    in vec2 entFragTex;
                    
                    in float espAlpha;

                    uniform vec2 renderResolution;

                    uniform vec4 bbBorderColor;
                    uniform vec4 bbFirstColor;
                    uniform vec4 bbSecondColor;
                    
                    uniform vec4 hbBorderColor;
                    uniform vec4 hbFirstColor;
                    uniform vec4 hbSecondColor;

                    uniform sampler2D menuFont;
                    uniform sampler2D itemsAtlas;
                    
                    uniform sampler2D potion;
                    uniform sampler2D splashPotion;
                    uniform sampler2D lingeringPotion;
                    uniform sampler2D potionOverlay;
                    uniform sampler2D enchantment;

                    vec4 potionOverlayColor() {
                    
                         int intColor = int(playerHealth.y);

                         float r = float((intColor >> 16) & 0xFF) / 255.0;
                         float g = float((intColor >> 8)  & 0xFF) / 255.0;
                         float b = float(intColor & 0xFF) / 255.0;

                         return vec4(r, g, b, 1.0);
                         
                    }

                    void main() {
                        
                        if (shaderId == 0) {
                            vec2 rectPos = vec2(rectanglePosition.x + 6, rectanglePosition.y);
                            vec2 rectSize = vec2(rectangleSize.x - 6, rectangleSize.y);
                            
                            if (hbBorderColor.r != -1) {
                            
                                if (
                                    gl_FragCoord.x - rectanglePosition.x >= 0 && gl_FragCoord.x - rectanglePosition.x <= 4 &&
                                    gl_FragCoord.y - rectanglePosition.y >= 0 && gl_FragCoord.y - rectanglePosition.y <= rectangleSize.y
                                ) {
                                    
                                    float maxBarSize = rectangleSize.y - 1;
                                    float barSize = maxBarSize / playerHealth.y * playerHealth.x;
                                    
                                    if (
                                        gl_FragCoord.x - rectanglePosition.x >= 1 && gl_FragCoord.x - rectanglePosition.x <= 3 &&
                                        gl_FragCoord.y - rectanglePosition.y >= 1 && gl_FragCoord.y - rectanglePosition.y <= barSize
                                    ) {
                                        float normalizedY = clamp((gl_FragCoord.y - rectPos.y - 2) / (rectSize.y - 2), 0, 1);
                                        FragColor = mix(hbSecondColor, hbFirstColor, normalizedY);
                                    } else {
                                        FragColor = hbBorderColor;
                                    }
                                    FragColor.a *= espAlpha / 255.0;
                                    return;
                                
                                }
                            
                            }
                            
                            if (bbBorderColor.r == -1) {
                                return;
                            }
                            
                            if(
                                (gl_FragCoord.x - rectPos.x <= 3 || gl_FragCoord.x - rectPos.x >= rectSize.x - 3) ||
                                (gl_FragCoord.y - rectPos.y <= 3 || gl_FragCoord.y - rectPos.y >= rectSize.y - 3)
                            ) {
                            
                                if(
                                    (gl_FragCoord.x - rectPos.x >= 0 && gl_FragCoord.x - rectPos.x <= 2 && gl_FragCoord.y - rectPos.y >= 0 && gl_FragCoord.y - rectPos.y <= rectSize.y) ||
                                    (gl_FragCoord.x - rectPos.x > rectSize.x - 2 && gl_FragCoord.x - rectPos.x <= rectSize.x && gl_FragCoord.y - rectPos.y > 0 && gl_FragCoord.y - rectPos.y < rectSize.y) ||
                                    (gl_FragCoord.x - rectPos.x > 2 && gl_FragCoord.x - rectPos.x <= rectSize.x - 2) && ((gl_FragCoord.y - rectPos.y >= 0 && gl_FragCoord.y - rectPos.y <= 2) || (gl_FragCoord.y - rectPos.y >= rectSize.y - 2 && gl_FragCoord.y - rectPos.y <= rectSize.y))
                                ) {
                                    FragColor = bbBorderColor;
                                    FragColor.a *= espAlpha / 255.0;
                                    return;
                                }
                                
                                return;
                                
                            }
                            
                            float normalizedY = clamp((gl_FragCoord.y - rectPos.y) / rectSize.y, 0, 1);
                            FragColor = mix(bbSecondColor, bbFirstColor, normalizedY);
                            FragColor.a *= espAlpha / 255.0;
                        } else if (shaderId == 1) {
                        
                            vec4 sampled = texture(menuFont, fragTex);

                            FragColor = vec4(rectanglePosition, rectangleSize) * sampled; // =(
                            FragColor.a *= espAlpha / 255.0;
                        } else if (shaderId == 2) {
                            
                            if(int(playerHealth.x) < 0) {
                                
                                vec4 sampled1 = vec4(0.0);
                                vec4 sampled2 = potionOverlayColor() * texture(potionOverlay, fragTex);
                                
                                switch (int(playerHealth.x)) {
                                    
                                    case -1:
                                        sampled1 = texture(potion, fragTex);
                                        break;
                                    case -2:
                                        sampled1 = texture(splashPotion, fragTex);
                                        break;
                                    case -3:
                                        sampled1 = texture(lingeringPotion, fragTex);
                                        break;
                                        
                                }

                                if(sampled1.a > 0) {
                                    FragColor = sampled1;
                                } else {
                                    FragColor = sampled2;
                                }
                                FragColor.a *= espAlpha / 255.0;
                                return;
                                
                            }

                            vec4 sampled = texture(itemsAtlas, fragTex);

                            vec4 itemColor = vec4(rectanglePosition, rectangleSize) * sampled; // =( x2
                            
                            FragColor = itemColor;
                            
                            if(playerHealth.x < 0) {
                            
                                vec4 sampledEnchantment = texture(enchantment, entFragTex);
                                
                                float maxChannel = max(max(sampledEnchantment.r, sampledEnchantment.g), sampledEnchantment.b);
                                                                  
                                float alpha = maxChannel;

                                if (alpha < 0.1) {
                                    alpha = 0.0;
                                }
                                
                                sampledEnchantment.a = alpha * 0.5;
                                
                                if(FragColor.a == 0) {
                                    sampledEnchantment.a = 0;
                                }
                                
                                vec3 blended = mix(itemColor.rgb, sampledEnchantment.rgb, sampledEnchantment.a);
                                FragColor = vec4(blended, itemColor.a);
                            
                            }
                            
                            FragColor.a *= espAlpha / 255.0;
                            
                        } else if (shaderId == 3) {
                        
                            float atlasSize = 2048.0;
                            float pixelSize = 1.0 / atlasSize;
                            
                            vec4 sampledText = vec4(rectanglePosition, rectangleSize.x, 1) * texture(menuFont, fragTex);

                            vec4 sampledOutline = (
                                texture(menuFont, fragTex + vec2(pixelSize, pixelSize)) +
                                texture(menuFont, fragTex + vec2(-pixelSize, -pixelSize)) +
                                texture(menuFont, fragTex + vec2(-pixelSize, pixelSize)) +
                                texture(menuFont, fragTex + vec2(pixelSize, -pixelSize)) +
                                texture(menuFont, fragTex + vec2(0, -pixelSize)) +
                                texture(menuFont, fragTex + vec2(0, pixelSize)) +
                                texture(menuFont, fragTex + vec2(-pixelSize, 0)) +
                                texture(menuFont, fragTex + vec2(pixelSize, 0))
                            );
                            
                            vec3 blackjopcasino = vec3(12.0 / 255.0, 12.0 / 255.0, 12.0 / 255.0);
                            
                            if(sampledOutline.a > 0) {
                                sampledOutline.rgb = blackjopcasino;
                                sampledOutline.a = 1;
                            }
                            
                            vec4 finalColor;
                            finalColor.rgb = sampledText.rgb * sampledText.a + sampledOutline.rgb * (1.0 - sampledText.a);
                            finalColor.a = max(sampledOutline.a, sampledText.a);
                            FragColor = finalColor;
                            
                            FragColor.a *= rectangleSize.y;
                            FragColor.a *= espAlpha / 255.0;
                        
                        } else {
                        
                            if(int(playerHealth.x) < 0) {
                                
                                vec4 sampled1 = vec4(0.0);
                                vec4 sampled2 = potionOverlayColor() * texture(potionOverlay, fragTex);
                                
                                switch (int(playerHealth.x)) {
                                    
                                    case -1:
                                        sampled1 = texture(potion, fragTex);
                                        break;
                                    case -2:
                                        sampled1 = texture(splashPotion, fragTex);
                                        break;
                                    case -3:
                                        sampled1 = texture(lingeringPotion, fragTex);
                                        break;
                                        
                                }

                                if(sampled1.a > 0) {
                                    FragColor = sampled1;
                                } else {
                                    FragColor = sampled2;
                                }
                                FragColor.a *= espAlpha / 255.0;
                                return;
                                
                            }

                            vec4 sampled = texture(itemsAtlas, fragTex);

                            vec4 itemColor = vec4(rectanglePosition, rectangleSize) * sampled; // =( x2
                            
                            FragColor = itemColor;
                            
                            if(playerHealth.x < 0) {
                            
                                vec4 sampledEnchantment = texture(enchantment, entFragTex);
                                
                                float maxChannel = max(max(sampledEnchantment.r, sampledEnchantment.g), sampledEnchantment.b);
                                                                  
                                float alpha = maxChannel;

                                if (alpha < 0.1) {
                                    alpha = 0.0;
                                }
                                
                                sampledEnchantment.a = alpha * 0.5;
                                
                                if(FragColor.a == 0) {
                                    sampledEnchantment.a = 0;
                                }
                                
                                vec3 blended = mix(itemColor.rgb, sampledEnchantment.rgb, sampledEnchantment.a);
                                FragColor = vec4(blended, itemColor.a);
                            
                            }
                            
                            FragColor.a *= espAlpha / 255.0;
                        
                        }
                         
                    }
                     
                    """;

    private String espBoxVertexShaderSource =

            """
                    #version 330 core
                    
                    layout (location = 0) in vec2 vertexPosition;
                    layout (location = 1) in vec4 rectangleProperties;
                    layout (location = 2) in vec2 health;
                    layout (location = 3) in float shader;
                    layout (location = 4) in vec2 texturePosition;
                    layout (location = 5) in vec2 enchantmentTexturePosition;
                    layout (location = 6) in float mainAlpha;
                    
                    out vec2 rectanglePosition;
                    out vec2 rectangleSize;
                    out vec2 playerHealth;
                    out float shaderId;
                    out vec2 fragTex;
                    out vec2 entFragTex;
                    out float espAlpha;
                    
                    uniform vec2 renderResolution;
                    
                    void main()
                    {
                    
                        vec2 normalizedPos = (vertexPosition / renderResolution) * 2.0 - 1.0;
                        gl_Position = vec4(normalizedPos.x, normalizedPos.y, 0.0, 1.0);

                        rectanglePosition = rectangleProperties.xy;
                        rectangleSize = rectangleProperties.zw;

                        playerHealth = health;
                        
                        shaderId = shader;
                        fragTex = texturePosition;
                        entFragTex = enchantmentTexturePosition;

                        espAlpha = mainAlpha;
                        
                    }
                    
                    """;

}
