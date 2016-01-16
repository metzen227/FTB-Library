package ftb.lib.api;

import ftb.lib.client.*;
import ftb.lib.gui.GuiLM;
import latmod.lib.util.FinalIDObject;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.*;

import java.util.List;

public abstract class PlayerAction extends FinalIDObject
{
	public static enum Type
	{
		SELF,
		OTHER,
		BOTH;
		
		public boolean self()
		{ return this == SELF || this == BOTH; }
		
		public boolean other()
		{ return this == OTHER || this == BOTH; }
		
		public boolean equalsType(Type t)
		{
			if(t == null || t == BOTH) return true;
			else if(t == SELF) return self();
			else return other();
		}
	}
	
	public final Type type;
	public final int priority;
	public final TextureCoords icon;
	
	public PlayerAction(Type t, String id, int p, TextureCoords c)
	{
		super(id);
		type = (t == null) ? Type.SELF : t;
		priority = p;
		icon = c;
	}
	
	public abstract void onClicked(ILMPlayer self, ILMPlayer other);
	
	public String getTitleKey()
	{ return ID; }
	
	@SideOnly(Side.CLIENT)
	public String getTitle()
	{ return I18n.format(getTitleKey()); }
	
	public void addMouseOverText(List<String> l) { }
	
	public void render(int ax, int ay, double z)
	{
		FTBLibClient.setTexture(icon);
		GuiLM.drawTexturedRectD(ax, ay, z, 16, 16, icon.minU, icon.minV, icon.maxU, icon.maxV);
	}
	
	public void postRender(int ax, int ay, double z)
	{
	}
	
	public int compareTo(Object o)
	{
		int i = Integer.compare(((PlayerAction) o).priority, priority);
		return (i == 0) ? super.compareTo(o) : i;
	}
	
	public boolean isVisibleFor(ILMPlayer self, ILMPlayer other)
	{ return true; }
}