/**
 * 
 */
package alphabetsoup.simulators.markettaskallocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Disk;

import alphabetsoup.framework.Circle;
import alphabetsoup.framework.SimulationWorld;
import alphabetsoup.userinterface.RenderWindow;
import alphabetsoup.userinterface.Renderable;

/**
 * @author Chris Hazard
 *
 */
public class SituatedMarketRender implements Renderable {
	
	MultiItemDoubleAuction<?, ?, ?, ?> situatedMarket = null;
	
	static private Disk disk = new Disk();

	public SituatedMarketRender() {
		
	}
	public SituatedMarketRender(MultiItemDoubleAuction situatedMarket) {
		this.situatedMarket = situatedMarket;
	}

	/* (non-Javadoc)
	 * @see alphabetsoup.userinterface.Renderable#isMouseOver(float, float)
	 */
	public boolean isMouseOver(float mouse_x, float mouse_y) {
		Circle c = new Circle(SimulationWorld.getSimulationWorld().map.getTolerance(), mouse_x, mouse_y);
		Economy economy = SimulationWorldMarketTaskAllocation.getSimulationWorld().economy;
		Circle location = economy.getMarketLocation(situatedMarket);
		return c.IsCollision(location.getX(), location.getY(), 0);
	}

	/* (non-Javadoc)
	 * @see alphabetsoup.userinterface.Renderable#render()
	 */
	public void render() {
		Economy economy = SimulationWorldMarketTaskAllocation.getSimulationWorld().economy;
		Circle location = economy.getMarketLocation(situatedMarket);
		
		GL11.glPushMatrix();
		GL11.glTranslatef(location.getX(), location.getY(), 0.0f);

		GL11.glColor4ub((byte)0x33, (byte)0x33, (byte)0x33, (byte)0xC0);
		disk.draw(0.0f, SimulationWorld.getSimulationWorld().map.getTolerance(), 14, 1);

		GL11.glPopMatrix();
	}

	/* (non-Javadoc)
	 * @see alphabetsoup.userinterface.Renderable#renderDetails()
	 */
	public void renderDetails() {
		//render additional info
		float x = 10.0f;
		float y = 220.0f;
		GL11.glColor4f(0.0f, 0.0f, 0.0f, 1.0f);

		for(String s : situatedMarket.getAdditionalInfo()) {
			RenderWindow.renderString(x, y, s);
			y += RenderWindow.getFontRenderHeight();
		}
	}

	/* (non-Javadoc)
	 * @see alphabetsoup.userinterface.Renderable#renderOverlayDetails()
	 */
	public void renderOverlayDetails() {
	}

}
