package com.feed_the_beast.ftblib.client;

import com.feed_the_beast.ftblib.FTBLib;
import com.feed_the_beast.ftblib.FTBLibConfig;
import com.feed_the_beast.ftblib.events.client.CustomClickEvent;
import com.feed_the_beast.ftblib.lib.ClientATHelper;
import com.feed_the_beast.ftblib.lib.client.ClientUtils;
import com.feed_the_beast.ftblib.lib.gui.GuiIcons;
import com.feed_the_beast.ftblib.lib.icon.AtlasSpriteIcon;
import com.feed_the_beast.ftblib.lib.icon.Color4I;
import com.feed_the_beast.ftblib.lib.icon.IconPresets;
import com.feed_the_beast.ftblib.lib.math.MathUtils;
import com.feed_the_beast.ftblib.lib.util.InvUtils;
import com.feed_the_beast.ftblib.lib.util.NBTUtils;
import com.feed_the_beast.ftblib.lib.util.SidedUtils;
import com.feed_the_beast.ftblib.lib.util.text_components.Notification;
import com.feed_the_beast.ftblib.net.MessageAdminPanelGui;
import com.feed_the_beast.ftblib.net.MessageMyTeamGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.chat.IChatListener;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author LatvianModder
 */
@Mod.EventBusSubscriber(modid = FTBLib.MOD_ID, value = Side.CLIENT)
public class FTBLibClientEventHandler
{
	private static Temp currentNotification;
	private static double sidebarButtonScale = 0D;
	public static Rectangle lastDrawnArea = new Rectangle();

	private static final IChatListener CHAT_LISTENER = (type, component) ->
	{
		if (type == ChatType.GAME_INFO)
		{
			if (component instanceof Notification || FTBLibClientConfig.replace_vanilla_status_messages)
			{
				ResourceLocation id = component instanceof Notification ? ((Notification) component).getId() : Notification.VANILLA_STATUS;
				Temp.MAP.remove(id);

				if (currentNotification != null && currentNotification.widget.id.equals(id))
				{
					currentNotification = null;
				}

				Temp.MAP.put(id, component);
			}
			else
			{
				ClientUtils.MC.ingameGUI.setOverlayMessage(component.getFormattedText(), false);
			}
		}
	};

	public static class NotificationWidget
	{
		public final ITextComponent notification;
		public final ResourceLocation id;
		public final List<String> text;
		public int width, height;
		public final FontRenderer font;
		public final long timer;

		public NotificationWidget(ITextComponent n, FontRenderer f)
		{
			notification = n;
			id = n instanceof Notification ? ((Notification) n).getId() : Notification.VANILLA_STATUS;
			width = 0;
			font = f;
			text = new ArrayList<>();
			timer = n instanceof Notification ? ((Notification) n).getTimer().ticks() : 60L;

			for (String s : font.listFormattedStringToWidth(notification.getFormattedText(), new ScaledResolution(ClientUtils.MC).getScaledWidth()))
			{
				for (String line : s.split("\n"))
				{
					if (!line.isEmpty())
					{
						line = line.trim();
						text.add(line);
						width = Math.max(width, font.getStringWidth(line));
					}
				}
			}

			width += 4;
			height = text.size() * 11;

			if (text.isEmpty())
			{
				width = 20;
				height = 20;
			}
		}
	}

	private static class Temp
	{
		private static final LinkedHashMap<ResourceLocation, ITextComponent> MAP = new LinkedHashMap<>();

		private long tick, endTick;
		private NotificationWidget widget;

		private Temp(ITextComponent n)
		{
			widget = new NotificationWidget(n, ClientUtils.MC.fontRenderer);
			tick = endTick = -1L;
		}

		public void render(ScaledResolution screen, float partialTicks)
		{
			if (tick == -1L || tick >= endTick)
			{
				return;
			}

			int alpha = (int) Math.min(255F, (endTick - tick - partialTicks) * 255F / 20F);

			if (alpha <= 2)
			{
				return;
			}

			GlStateManager.pushMatrix();
			GlStateManager.translate((int) (screen.getScaledWidth() / 2F), (int) (screen.getScaledHeight() - 68F), 0F);
			GlStateManager.disableDepth();
			GlStateManager.depthMask(false);
			GlStateManager.disableLighting();
			GlStateManager.enableBlend();
			GlStateManager.color(1F, 1F, 1F, 1F);

			int offy = -(widget.text.size() * 11) / 2;

			for (int i = 0; i < widget.text.size(); i++)
			{
				String string = widget.text.get(i);
				widget.font.drawStringWithShadow(string, -widget.font.getStringWidth(string) / 2, offy + i * 11, 0xFFFFFF | (alpha << 24));
			}

			GlStateManager.depthMask(true);
			GlStateManager.color(1F, 1F, 1F, 1F);
			GlStateManager.enableLighting();
			GlStateManager.popMatrix();
			GlStateManager.enableDepth();
		}

		private boolean tick()
		{
			tick = ClientUtils.MC.world.getTotalWorldTime();

			if (endTick == -1L)
			{
				endTick = tick + widget.timer;
			}

			return tick >= endTick || Math.min(255F, (endTick - tick) * 255F / 20F) <= 2F;
		}

		private boolean isImportant()
		{
			return widget.notification instanceof Notification && ((Notification) widget.notification).isImportant();
		}
	}

	@SubscribeEvent
	public static void onConnected(FMLNetworkEvent.ClientConnectedToServerEvent event)
	{
		SidedUtils.UNIVERSE_UUID_CLIENT = null;
		currentNotification = null;
		Temp.MAP.clear();
		ClientATHelper.getChatListeners().get(ChatType.GAME_INFO).clear();
		ClientATHelper.getChatListeners().get(ChatType.GAME_INFO).add(CHAT_LISTENER);
	}

	@SubscribeEvent
	public static void onTooltip(ItemTooltipEvent event)
	{
		if (FTBLibClientConfig.item_ore_names)
		{
			Collection<String> ores = InvUtils.getOreNames(null, event.getItemStack());

			if (!ores.isEmpty())
			{
				event.getToolTip().add(I18n.format("ftblib_client.general.item_ore_names.item_tooltip"));

				for (String or : ores)
				{
					event.getToolTip().add("> " + or);
				}
			}
		}

		if (FTBLibClientConfig.item_nbt && GuiScreen.isShiftKeyDown())
		{
			NBTTagCompound nbt = GuiScreen.isAltKeyDown() ? event.getItemStack().getItem().getNBTShareTag(event.getItemStack()) : event.getItemStack().getTagCompound();

			if (nbt != null)
			{
				event.getToolTip().add(NBTUtils.getColoredNBTString(nbt));
			}
		}
	}

	@SubscribeEvent
	public static void onGuiInit(final GuiScreenEvent.InitGuiEvent.Post event)
	{
		//sidebarButtonScale = 0D;

		if (areButtonsVisible(event.getGui()))
		{
			event.getButtonList().add(new GuiButtonSidebarGroup((InventoryEffectRenderer) event.getGui()));
		}
	}

	public static boolean areButtonsVisible(@Nullable GuiScreen gui)
	{
		return FTBLibClientConfig.action_buttons != EnumSidebarButtonPlacement.DISABLED && gui instanceof InventoryEffectRenderer && !SidebarButtonManager.INSTANCE.groups.isEmpty();
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event)
	{
		if (event.phase == TickEvent.Phase.START)
		{
			if (ClientUtils.MC.world == null)
			{
				currentNotification = null;
				Temp.MAP.clear();
			}

			if (currentNotification != null)
			{
				if (currentNotification.tick())
				{
					currentNotification = null;
				}
			}

			if (currentNotification == null && !Temp.MAP.isEmpty())
			{
				currentNotification = new Temp(Temp.MAP.values().iterator().next());
				Temp.MAP.remove(currentNotification.widget.id);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
	public static void onGameOverlayRender(RenderGameOverlayEvent.Text event)
	{
		if (currentNotification != null && !currentNotification.isImportant())
		{
			currentNotification.render(event.getResolution(), event.getPartialTicks());
			GlStateManager.color(1F, 1F, 1F, 1F);
			GlStateManager.disableLighting();
			GlStateManager.enableBlend();
			GlStateManager.enableTexture2D();
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
	public static void onRenderTick(TickEvent.RenderTickEvent event)
	{
		if (event.phase == TickEvent.Phase.END && currentNotification != null && currentNotification.isImportant())
		{
			currentNotification.render(new ScaledResolution(ClientUtils.MC), event.renderTickTime);
			GlStateManager.color(1F, 1F, 1F, 1F);
			GlStateManager.disableLighting();
			GlStateManager.enableBlend();
			GlStateManager.enableTexture2D();
		}
	}

	@SubscribeEvent
	public static void onDebugInfoEvent(RenderGameOverlayEvent.Text event)
	{
		if (FTBLibClientConfig.debug_helper && !ClientUtils.MC.gameSettings.showDebugInfo && Keyboard.isKeyDown(Keyboard.KEY_F3))
		{
			event.getLeft().add(I18n.format("debug.help.help"));
		}
	}

	@SubscribeEvent
	public static void onBeforeTexturesStitched(TextureStitchEvent.Pre event)
	{
		ClientUtils.SPRITE_MAP.clear();

		try
		{
			for (Field field : GuiIcons.class.getDeclaredFields())
			{
				field.setAccessible(true);
				Object o = field.get(null);

				if (o instanceof AtlasSpriteIcon)
				{
					AtlasSpriteIcon a = (AtlasSpriteIcon) o;
					event.getMap().registerSprite(a.name);
					IconPresets.MAP.put(a.name.toString(), a);
				}
			}
		}
		catch (Exception ex)
		{
			if (FTBLibConfig.debugging.print_more_errors)
			{
				ex.printStackTrace();
			}
		}
	}

	@SubscribeEvent
	public static void onCustomClick(CustomClickEvent event)
	{
		if (event.getID().getNamespace().equals(FTBLib.MOD_ID))
		{
			switch (event.getID().getPath())
			{
				case "client_config_gui":
					new GuiClientConfig().openGui();
					break;
				case "my_team_gui":
					new MessageMyTeamGui().sendToServer();
					break;
				case "admin_panel_gui":
					new MessageAdminPanelGui().sendToServer();
					break;
			}

			event.setCanceled(true);
		}
	}

	private static class GuiButtonSidebar
	{
		public final int buttonX, buttonY;
		public final SidebarButton button;
		public int x, y;

		public GuiButtonSidebar(int x, int y, SidebarButton b)
		{
			buttonX = x;
			buttonY = y;
			button = b;
		}
	}

	private static class GuiButtonSidebarGroup extends GuiButton
	{
		private final InventoryEffectRenderer gui;
		public final List<GuiButtonSidebar> buttons;
		private GuiButtonSidebar mouseOver;

		public GuiButtonSidebarGroup(InventoryEffectRenderer g)
		{
			super(495829, 0, 0, 0, 0, "");
			gui = g;
			buttons = new ArrayList<>();
		}

		@Override
		public void drawButton(Minecraft mc, int mx, int my, float partialTicks)
		{
			buttons.clear();
			mouseOver = null;
			int rx, ry = 0;
			boolean addedAny;
			boolean top = FTBLibClientConfig.action_buttons.top() || !gui.mc.player.getActivePotionEffects().isEmpty() || (gui instanceof GuiInventory && ((GuiInventory) gui).func_194310_f().isVisible());

			for (SidebarButtonGroup group : SidebarButtonManager.INSTANCE.groups)
			{
				rx = 0;
				addedAny = false;

				for (SidebarButton button : group.getButtons())
				{
					if (button.isVisible())
					{
						buttons.add(new GuiButtonSidebar(rx, ry, button));
						rx++;
						addedAny = true;
					}
				}

				if (addedAny)
				{
					ry++;
				}
			}

			int guiLeft = gui.getGuiLeft();
			int guiTop = gui.getGuiTop();

			if (top)
			{
				for (GuiButtonSidebar button : buttons)
				{
					if (FTBLibClientConfig.collapse_sidebar_buttons)
					{
						button.x = 4 + button.buttonX * 17;
						button.y = 4 + button.buttonY * 17;
					}
					else
					{
						button.x = 1 + button.buttonX * 17;
						button.y = 1 + button.buttonY * 17;
					}
				}
			}
			else
			{
				int offsetY = 8;

				if (gui instanceof GuiContainerCreative)
				{
					offsetY = 6;
				}

				for (GuiButtonSidebar button : buttons)
				{
					button.x = guiLeft - 18 - button.buttonY * 17;
					button.y = guiTop + offsetY + button.buttonX * 17;
				}
			}

			x = Integer.MAX_VALUE;
			y = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int maxY = Integer.MIN_VALUE;

			for (GuiButtonSidebar b : buttons)
			{
				if (b.x >= 0 && b.y >= 0)
				{
					x = Math.min(x, b.x);
					y = Math.min(y, b.y);
					maxX = Math.max(maxX, b.x + 16);
					maxY = Math.max(maxY, b.y + 16);
				}

				if (mx >= b.x && my >= b.y && mx < b.x + 16 && my < b.y + 16)
				{
					mouseOver = b;
				}
			}

			x -= 2;
			y -= 2;
			maxX += 2;
			maxY += 2;

			width = maxX - x;
			height = maxY - y;

			if (sidebarButtonScale <= 0D)
			{
				if (mx >= x && my >= y && mx < x + 16 && my < y + 16)
				{
					sidebarButtonScale = 0.01D;
				}
			}

			if (mx < x || my < y || mx >= x + width || my >= y + height)
			{
				sidebarButtonScale -= partialTicks * 0.3D * FTBLibClientConfig.sidebar_button_collapse_speed;

				if (sidebarButtonScale < 0D)
				{
					sidebarButtonScale = 0D;
				}
			}
			else if (sidebarButtonScale > 0D)
			{
				sidebarButtonScale += partialTicks * 0.3D * FTBLibClientConfig.sidebar_button_collapse_speed;

				if (sidebarButtonScale > 1D)
				{
					sidebarButtonScale = 1D;
				}
			}

			if (!FTBLibClientConfig.collapse_sidebar_buttons)
			{
				sidebarButtonScale = 1D;
			}

			zLevel = 0F;

			GlStateManager.pushMatrix();
			GlStateManager.translate(0, 0, 500);

			FontRenderer font = mc.fontRenderer;

			GlStateManager.enableBlend();
			GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GlStateManager.color(1F, 1F, 1F, 1F);

			GlStateManager.pushMatrix();

			double scale = 1.0;
			if (sidebarButtonScale < 1D)
			{
				scale = Math.min(16D / MathUtils.lerp(width, 16D, sidebarButtonScale), 16D / MathUtils.lerp(height, 16D, sidebarButtonScale));
				GlStateManager.scale(scale, scale, 1D);
			}

			if (FTBLibClientConfig.collapse_sidebar_buttons)
			{
				int alpha = (int) MathUtils.lerp(50D, 100D, sidebarButtonScale);
				Color4I.GRAY.withAlpha(alpha).draw(x, y, width, height);
				Color4I.DARK_GRAY.withAlpha(alpha).draw(x + 1, y + 1, width - 2, height - 2);
			}

			int alpha255 = (int) MathUtils.lerp(80D, 255D, sidebarButtonScale);

			for (GuiButtonSidebar b : buttons)
			{
				b.button.getIcon().draw(b.x, b.y, 16, 16, Color4I.WHITE.withAlpha(alpha255));

				if (sidebarButtonScale >= 1D && b == mouseOver)
				{
					Color4I.WHITE.withAlpha(33).draw(b.x, b.y, 16, 16);
				}

				if (sidebarButtonScale >= 1D && b.button.hasCustomText() && b.button.getCustomTextHandler() != null)
				{
					String text = b.button.getCustomTextHandler().get();

					if (!text.isEmpty())
					{
						int nw = font.getStringWidth(text);
						int width = 16;
						Color4I.LIGHT_RED.draw(b.x + width - nw, b.y - 1, nw + 1, 9);
						font.drawString(text, b.x + width - nw + 1, b.y, 0xFFFFFFFF);
						GlStateManager.color(1F, 1F, 1F, 1F);
					}
				}
			}

			if (mouseOver != null && sidebarButtonScale >= 1D)
			{
				int mx1 = mx + 10;
				int my1 = Math.max(3, my - 9);

				List<String> list = new ArrayList<>();
				list.add(I18n.format(mouseOver.button.getLangKey()));

				if (mouseOver.button.getTooltipHandler() != null)
				{
					mouseOver.button.getTooltipHandler().accept(list);
				}

				int tw = 0;

				for (String s : list)
				{
					tw = Math.max(tw, font.getStringWidth(s));
				}

				GlStateManager.pushMatrix();
				GlStateManager.translate(0, 0, 500);
				GlStateManager.enableBlend();
				GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				Color4I.DARK_GRAY.draw(mx1 - 3, my1 - 2, tw + 6, 2 + list.size() * 10);

				for (int i = 0; i < list.size(); i++)
				{
					font.drawString(list.get(i), mx1, my1 + i * 10, 0xFFFFFFFF);
				}

				GlStateManager.color(1F, 1F, 1F, 1F);
				GlStateManager.popMatrix();
			}

			GlStateManager.color(1F, 1F, 1F, 1F);
			GlStateManager.popMatrix();
			GlStateManager.popMatrix();
			zLevel = 0F;

			lastDrawnArea = new Rectangle(
				(int) Math.ceil(x * scale),
				(int) Math.ceil(y * scale),
				(int) Math.ceil(width * scale),
				(int) Math.ceil(height * scale)
			);
		}

		@Override
		public boolean mousePressed(Minecraft mc, int mx, int my)
		{
			if (super.mousePressed(mc, mx, my))
			{
				if (sidebarButtonScale >= 1D && mouseOver != null)
				{
					mouseOver.button.onClicked(GuiScreen.isShiftKeyDown());

					if (!(ClientUtils.MC.currentScreen instanceof InventoryEffectRenderer))
					{
						sidebarButtonScale = 0D;
					}
				}

				return true;
			}

			return false;
		}
	}
}