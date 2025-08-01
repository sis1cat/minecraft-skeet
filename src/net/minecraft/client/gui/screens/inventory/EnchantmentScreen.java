package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EnchantmentScreen extends AbstractContainerScreen<EnchantmentMenu> {
    private static final ResourceLocation[] ENABLED_LEVEL_SPRITES = new ResourceLocation[]{
        ResourceLocation.withDefaultNamespace("container/enchanting_table/level_1"),
        ResourceLocation.withDefaultNamespace("container/enchanting_table/level_2"),
        ResourceLocation.withDefaultNamespace("container/enchanting_table/level_3")
    };
    private static final ResourceLocation[] DISABLED_LEVEL_SPRITES = new ResourceLocation[]{
        ResourceLocation.withDefaultNamespace("container/enchanting_table/level_1_disabled"),
        ResourceLocation.withDefaultNamespace("container/enchanting_table/level_2_disabled"),
        ResourceLocation.withDefaultNamespace("container/enchanting_table/level_3_disabled")
    };
    private static final ResourceLocation ENCHANTMENT_SLOT_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/enchanting_table/enchantment_slot_disabled");
    private static final ResourceLocation ENCHANTMENT_SLOT_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("container/enchanting_table/enchantment_slot_highlighted");
    private static final ResourceLocation ENCHANTMENT_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/enchanting_table/enchantment_slot");
    private static final ResourceLocation ENCHANTING_TABLE_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/enchanting_table.png");
    private static final ResourceLocation ENCHANTING_BOOK_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/enchanting_table_book.png");
    private final RandomSource random = RandomSource.create();
    private BookModel bookModel;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
    private ItemStack last = ItemStack.EMPTY;

    public EnchantmentScreen(EnchantmentMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
        this.bookModel = new BookModel(this.minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.tickBook();
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        for (int k = 0; k < 3; k++) {
            double d0 = pMouseX - (double)(i + 60);
            double d1 = pMouseY - (double)(j + 14 + 19 * k);
            if (d0 >= 0.0 && d1 >= 0.0 && d0 < 108.0 && d1 < 19.0 && this.menu.clickMenuButton(this.minecraft.player, k)) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, k);
                return true;
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    protected void renderBg(GuiGraphics p_282430_, float p_282530_, int p_281621_, int p_283333_) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        p_282430_.blit(RenderType::guiTextured, ENCHANTING_TABLE_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        this.renderBook(p_282430_, i, j, p_282530_);
        EnchantmentNames.getInstance().initSeed((long)this.menu.getEnchantmentSeed());
        int k = this.menu.getGoldCount();

        for (int l = 0; l < 3; l++) {
            int i1 = i + 60;
            int j1 = i1 + 20;
            int k1 = this.menu.costs[l];
            if (k1 == 0) {
                p_282430_.blitSprite(RenderType::guiTextured, ENCHANTMENT_SLOT_DISABLED_SPRITE, i1, j + 14 + 19 * l, 108, 19);
            } else {
                String s = k1 + "";
                int l1 = 86 - this.font.width(s);
                FormattedText formattedtext = EnchantmentNames.getInstance().getRandomName(this.font, l1);
                int i2 = 6839882;
                if ((k < l + 1 || this.minecraft.player.experienceLevel < k1) && !this.minecraft.player.getAbilities().instabuild) {
                    p_282430_.blitSprite(RenderType::guiTextured, ENCHANTMENT_SLOT_DISABLED_SPRITE, i1, j + 14 + 19 * l, 108, 19);
                    p_282430_.blitSprite(RenderType::guiTextured, DISABLED_LEVEL_SPRITES[l], i1 + 1, j + 15 + 19 * l, 16, 16);
                    p_282430_.drawWordWrap(this.font, formattedtext, j1, j + 16 + 19 * l, l1, (i2 & 16711422) >> 1, false);
                    i2 = 4226832;
                } else {
                    int j2 = p_281621_ - (i + 60);
                    int k2 = p_283333_ - (j + 14 + 19 * l);
                    if (j2 >= 0 && k2 >= 0 && j2 < 108 && k2 < 19) {
                        p_282430_.blitSprite(RenderType::guiTextured, ENCHANTMENT_SLOT_HIGHLIGHTED_SPRITE, i1, j + 14 + 19 * l, 108, 19);
                        i2 = 16777088;
                    } else {
                        p_282430_.blitSprite(RenderType::guiTextured, ENCHANTMENT_SLOT_SPRITE, i1, j + 14 + 19 * l, 108, 19);
                    }

                    p_282430_.blitSprite(RenderType::guiTextured, ENABLED_LEVEL_SPRITES[l], i1 + 1, j + 15 + 19 * l, 16, 16);
                    p_282430_.drawWordWrap(this.font, formattedtext, j1, j + 16 + 19 * l, l1, i2, false);
                    i2 = 8453920;
                }

                p_282430_.drawString(this.font, s, j1 + 86 - this.font.width(s), j + 16 + 19 * l + 7, i2);
            }
        }
    }

    private void renderBook(GuiGraphics pGuiGraphics, int pX, int pY, float pPartialTick) {
        float f = Mth.lerp(pPartialTick, this.oOpen, this.open);
        float f1 = Mth.lerp(pPartialTick, this.oFlip, this.flip);
        pGuiGraphics.flush();
        Lighting.setupForEntityInInventory();
        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().translate((float)pX + 33.0F, (float)pY + 31.0F, 100.0F);
        float f2 = 40.0F;
        pGuiGraphics.pose().scale(-40.0F, 40.0F, 40.0F);
        pGuiGraphics.pose().mulPose(Axis.XP.rotationDegrees(25.0F));
        pGuiGraphics.pose().translate((1.0F - f) * 0.2F, (1.0F - f) * 0.1F, (1.0F - f) * 0.25F);
        float f3 = -(1.0F - f) * 90.0F - 90.0F;
        pGuiGraphics.pose().mulPose(Axis.YP.rotationDegrees(f3));
        pGuiGraphics.pose().mulPose(Axis.XP.rotationDegrees(180.0F));
        float f4 = Mth.clamp(Mth.frac(f1 + 0.25F) * 1.6F - 0.3F, 0.0F, 1.0F);
        float f5 = Mth.clamp(Mth.frac(f1 + 0.75F) * 1.6F - 0.3F, 0.0F, 1.0F);
        this.bookModel.setupAnim(0.0F, f4, f5, f);
        pGuiGraphics.drawSpecial(p_367194_ -> {
            VertexConsumer vertexconsumer = p_367194_.getBuffer(this.bookModel.renderType(ENCHANTING_BOOK_LOCATION));
            this.bookModel.renderToBuffer(pGuiGraphics.pose(), vertexconsumer, 15728880, OverlayTexture.NO_OVERLAY);
        });
        pGuiGraphics.flush();
        pGuiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    @Override
    public void render(GuiGraphics p_283462_, int p_282491_, int p_281953_, float p_282182_) {
        float f = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        super.render(p_283462_, p_282491_, p_281953_, f);
        this.renderTooltip(p_283462_, p_282491_, p_281953_);
        boolean flag = this.minecraft.player.getAbilities().instabuild;
        int i = this.menu.getGoldCount();

        for (int j = 0; j < 3; j++) {
            int k = this.menu.costs[j];
            Optional<Holder.Reference<Enchantment>> optional = this.minecraft
                .level
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(this.menu.enchantClue[j]);
            if (!optional.isEmpty()) {
                int l = this.menu.levelClue[j];
                int i1 = j + 1;
                if (this.isHovering(60, 14 + 19 * j, 108, 17, (double)p_282491_, (double)p_281953_) && k > 0 && l >= 0 && optional != null) {
                    List<Component> list = Lists.newArrayList();
                    list.add(Component.translatable("container.enchant.clue", Enchantment.getFullname(optional.get(), l)).withStyle(ChatFormatting.WHITE));
                    if (!flag) {
                        list.add(CommonComponents.EMPTY);
                        if (this.minecraft.player.experienceLevel < k) {
                            list.add(Component.translatable("container.enchant.level.requirement", this.menu.costs[j]).withStyle(ChatFormatting.RED));
                        } else {
                            MutableComponent mutablecomponent;
                            if (i1 == 1) {
                                mutablecomponent = Component.translatable("container.enchant.lapis.one");
                            } else {
                                mutablecomponent = Component.translatable("container.enchant.lapis.many", i1);
                            }

                            list.add(mutablecomponent.withStyle(i >= i1 ? ChatFormatting.GRAY : ChatFormatting.RED));
                            MutableComponent mutablecomponent1;
                            if (i1 == 1) {
                                mutablecomponent1 = Component.translatable("container.enchant.level.one");
                            } else {
                                mutablecomponent1 = Component.translatable("container.enchant.level.many", i1);
                            }

                            list.add(mutablecomponent1.withStyle(ChatFormatting.GRAY));
                        }
                    }

                    p_283462_.renderComponentTooltip(this.font, list, p_282491_, p_281953_);
                    break;
                }
            }
        }
    }

    public void tickBook() {
        ItemStack itemstack = this.menu.getSlot(0).getItem();
        if (!ItemStack.matches(itemstack, this.last)) {
            this.last = itemstack;

            do {
                this.flipT = this.flipT + (float)(this.random.nextInt(4) - this.random.nextInt(4));
            } while (this.flip <= this.flipT + 1.0F && this.flip >= this.flipT - 1.0F);
        }

        this.oFlip = this.flip;
        this.oOpen = this.open;
        boolean flag = false;

        for (int i = 0; i < 3; i++) {
            if (this.menu.costs[i] != 0) {
                flag = true;
            }
        }

        if (flag) {
            this.open += 0.2F;
        } else {
            this.open -= 0.2F;
        }

        this.open = Mth.clamp(this.open, 0.0F, 1.0F);
        float f1 = (this.flipT - this.flip) * 0.4F;
        float f = 0.2F;
        f1 = Mth.clamp(f1, -0.2F, 0.2F);
        this.flipA = this.flipA + (f1 - this.flipA) * 0.9F;
        this.flip = this.flip + this.flipA;
    }
}