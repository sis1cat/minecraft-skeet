package net.minecraft.client.gui.screens.packs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PackSelectionModel {
    private final PackRepository repository;
    final List<Pack> selected;
    final List<Pack> unselected;
    final Function<Pack, ResourceLocation> iconGetter;
    final Runnable onListChanged;
    private final Consumer<PackRepository> output;

    public PackSelectionModel(Runnable pOnListChanged, Function<Pack, ResourceLocation> pIconGetter, PackRepository pRepository, Consumer<PackRepository> pOutput) {
        this.onListChanged = pOnListChanged;
        this.iconGetter = pIconGetter;
        this.repository = pRepository;
        this.selected = Lists.newArrayList(pRepository.getSelectedPacks());
        Collections.reverse(this.selected);
        this.unselected = Lists.newArrayList(pRepository.getAvailablePacks());
        this.unselected.removeAll(this.selected);
        this.output = pOutput;
    }

    public Stream<PackSelectionModel.Entry> getUnselected() {
        return this.unselected.stream().map(p_99920_ -> new PackSelectionModel.UnselectedPackEntry(p_99920_));
    }

    public Stream<PackSelectionModel.Entry> getSelected() {
        return this.selected.stream().map(p_99915_ -> new PackSelectionModel.SelectedPackEntry(p_99915_));
    }

    void updateRepoSelectedList() {
        this.repository.setSelected(Lists.reverse(this.selected).stream().map(Pack::getId).collect(ImmutableList.toImmutableList()));
    }

    public void commit() {
        this.updateRepoSelectedList();
        this.output.accept(this.repository);
    }

    public void findNewPacks() {
        this.repository.reload();
        this.selected.retainAll(this.repository.getAvailablePacks());
        this.unselected.clear();
        this.unselected.addAll(this.repository.getAvailablePacks());
        this.unselected.removeAll(this.selected);
    }

    @OnlyIn(Dist.CLIENT)
    public interface Entry {
        ResourceLocation getIconTexture();

        PackCompatibility getCompatibility();

        String getId();

        Component getTitle();

        Component getDescription();

        PackSource getPackSource();

        default Component getExtendedDescription() {
            return this.getPackSource().decorate(this.getDescription());
        }

        boolean isFixedPosition();

        boolean isRequired();

        void select();

        void unselect();

        void moveUp();

        void moveDown();

        boolean isSelected();

        default boolean canSelect() {
            return !this.isSelected();
        }

        default boolean canUnselect() {
            return this.isSelected() && !this.isRequired();
        }

        boolean canMoveUp();

        boolean canMoveDown();
    }

    @OnlyIn(Dist.CLIENT)
    abstract class EntryBase implements PackSelectionModel.Entry {
        private final Pack pack;

        public EntryBase(final Pack pPack) {
            this.pack = pPack;
        }

        protected abstract List<Pack> getSelfList();

        protected abstract List<Pack> getOtherList();

        @Override
        public ResourceLocation getIconTexture() {
            return PackSelectionModel.this.iconGetter.apply(this.pack);
        }

        @Override
        public PackCompatibility getCompatibility() {
            return this.pack.getCompatibility();
        }

        @Override
        public String getId() {
            return this.pack.getId();
        }

        @Override
        public Component getTitle() {
            return this.pack.getTitle();
        }

        @Override
        public Component getDescription() {
            return this.pack.getDescription();
        }

        @Override
        public PackSource getPackSource() {
            return this.pack.getPackSource();
        }

        @Override
        public boolean isFixedPosition() {
            return this.pack.isFixedPosition();
        }

        @Override
        public boolean isRequired() {
            return this.pack.isRequired();
        }

        protected void toggleSelection() {
            this.getSelfList().remove(this.pack);
            this.pack.getDefaultPosition().insert(this.getOtherList(), this.pack, Pack::selectionConfig, true);
            PackSelectionModel.this.onListChanged.run();
            PackSelectionModel.this.updateRepoSelectedList();
            this.updateHighContrastOptionInstance();
        }

        private void updateHighContrastOptionInstance() {
            if (this.pack.getId().equals("high_contrast")) {
                OptionInstance<Boolean> optioninstance = Minecraft.getInstance().options.highContrast();
                optioninstance.set(!optioninstance.get());
            }
        }

        protected void move(int pOffset) {
            List<Pack> list = this.getSelfList();
            int i = list.indexOf(this.pack);
            list.remove(i);
            list.add(i + pOffset, this.pack);
            PackSelectionModel.this.onListChanged.run();
        }

        @Override
        public boolean canMoveUp() {
            List<Pack> list = this.getSelfList();
            int i = list.indexOf(this.pack);
            return i > 0 && !list.get(i - 1).isFixedPosition();
        }

        @Override
        public void moveUp() {
            this.move(-1);
        }

        @Override
        public boolean canMoveDown() {
            List<Pack> list = this.getSelfList();
            int i = list.indexOf(this.pack);
            return i >= 0 && i < list.size() - 1 && !list.get(i + 1).isFixedPosition();
        }

        @Override
        public void moveDown() {
            this.move(1);
        }
    }

    @OnlyIn(Dist.CLIENT)
    class SelectedPackEntry extends PackSelectionModel.EntryBase {
        public SelectedPackEntry(final Pack p_99954_) {
            super(p_99954_);
        }

        @Override
        protected List<Pack> getSelfList() {
            return PackSelectionModel.this.selected;
        }

        @Override
        protected List<Pack> getOtherList() {
            return PackSelectionModel.this.unselected;
        }

        @Override
        public boolean isSelected() {
            return true;
        }

        @Override
        public void select() {
        }

        @Override
        public void unselect() {
            this.toggleSelection();
        }
    }

    @OnlyIn(Dist.CLIENT)
    class UnselectedPackEntry extends PackSelectionModel.EntryBase {
        public UnselectedPackEntry(final Pack p_99963_) {
            super(p_99963_);
        }

        @Override
        protected List<Pack> getSelfList() {
            return PackSelectionModel.this.unselected;
        }

        @Override
        protected List<Pack> getOtherList() {
            return PackSelectionModel.this.selected;
        }

        @Override
        public boolean isSelected() {
            return false;
        }

        @Override
        public void select() {
            this.toggleSelection();
        }

        @Override
        public void unselect() {
        }
    }
}