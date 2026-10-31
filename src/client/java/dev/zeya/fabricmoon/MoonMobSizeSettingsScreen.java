package dev.zeya.fabricmoon;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

public final class MoonMobSizeSettingsScreen extends Screen {
   private static final int PANEL_WIDTH = 340;
   private static final int PANEL_HEIGHT = 352;
   private static final int ROWS_PER_PAGE = 6;
   private static final int ROW_HEIGHT = 24;
   private static final int ROW_WIDTH = 280;
   private static final int MAX_LABEL_LENGTH = 36;
   private int page;

   public MoonMobSizeSettingsScreen() {
      super(Component.literal("Mobs do tamanho do player"));
   }

   protected void init() {
      List<EntityType<?>> mobs = MoonMobSizes.resizableMobs();
      int maxPage = Math.max(0, (mobs.size() - 1) / 6);
      this.page = Math.max(0, Math.min(this.page, maxPage));
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 352);
      int rowsTop = top + 64;
      int rowX = centerX - 140;
      int firstIndex = this.page * 6;

      for (int row = 0; row < 6; row++) {
         int index = firstIndex + row;
         if (index >= mobs.size()) {
            break;
         }

         EntityType<?> type = mobs.get(index);
         String id = MoonMobSizes.idOf(type);
         String label = (MoonConfig.isMobPlayerSizeTarget(id) ? "[ON] " : "[OFF] ") + shorten(MoonMobSizes.displayNameOf(type));
         this.addRenderableWidget(Button.builder(Component.literal(label), button -> this.toggleMob(id)).bounds(rowX, rowsTop + row * 24, 280, 20).build());
      }

      this.addRenderableWidget(Button.builder(Component.literal("< Anterior"), button -> this.changePage(-1)).bounds(centerX - 138, top + 214, 96, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("Proxima >"), button -> this.changePage(1)).bounds(centerX + 42, top + 214, 96, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("Mobs maiores"), button -> {
         MoonMobSizes.selectAllLargerThanPlayer();
         MoonConfig.save();
         this.clearWidgets();
         this.init();
      }).bounds(centerX - 138, top + 242, 135, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("Limpar selecao"), button -> {
         MoonConfig.clearMobPlayerSizeTargets();
         MoonConfig.save();
         this.clearWidgets();
         this.init();
      }).bounds(centerX + 3, top + 242, 135, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("- 0.1"), button -> this.changeMobSize(-0.1F)).bounds(centerX - 138, top + 286, 84, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("1.0x"), button -> this.setMobSize(1.0F)).bounds(centerX - 42, top + 286, 84, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("+ 0.1"), button -> this.changeMobSize(0.1F)).bounds(centerX + 54, top + 286, 84, 20).build());
      this.addRenderableWidget(
         Button.builder(Component.literal("Voltar"), button -> this.minecraft.gui.setScreen(new MoonPlayerSizeSettingsScreen()))
            .bounds(rowX, top + 318, 280, 22)
            .build()
      );
   }

   private void toggleMob(String id) {
      MoonConfig.toggleMobPlayerSizeTarget(id);
      MoonConfig.save();
      this.clearWidgets();
      this.init();
   }

   private void changePage(int direction) {
      this.page += direction;
      this.clearWidgets();
      this.init();
   }

   private void changeMobSize(float amount) {
      this.setMobSize(MoonConfig.mobSize + amount);
   }

   private void setMobSize(float size) {
      MoonConfig.mobSize = MoonConfig.clampMobSize(Math.round(size * 10.0F) / 10.0F);
      MoonConfig.save();
      this.clearWidgets();
      this.init();
   }

   private static String shorten(String text) {
      return text.length() <= 36 ? text : text.substring(0, 33) + "...";
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      int top = MoonScreenStyle.panelTop(this.height, 352);
      MoonScreenStyle.drawPanel(graphics, this.width / 2, top, 340, 352);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      List<EntityType<?>> mobs = MoonMobSizes.resizableMobs();
      int maxPage = Math.max(0, (mobs.size() - 1) / 6);
      int centerX = this.width / 2;
      int top = MoonScreenStyle.panelTop(this.height, 352);
      graphics.centeredText(this.font, this.title, centerX, top + 14, -1);
      graphics.centeredText(this.font, "Selecionados: " + MoonMobSizes.selectedCount() + " de " + mobs.size(), centerX, top + 42, -4799016);
      graphics.centeredText(this.font, "Magma cube mantem sempre o tamanho original.", centerX, top + 53, -7891030);
      graphics.centeredText(this.font, this.page + 1 + " / " + (maxPage + 1), centerX, top + 220, -1);
      MoonScreenStyle.drawSection(graphics, this.font, centerX, "TAMANHO DOS MOBS: " + String.format(Locale.ROOT, "%.1fx", MoonConfig.mobSize), top + 272);
      graphics.centeredText(this.font, "1.0x = altura do player. So afeta os mobs marcados.", centerX, top + 308, -7891030);
   }
}
