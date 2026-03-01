
package net.mcreator.trollden.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.mcreator.trollden.client.model.Modelwarden3;

public class TrolldenRenderer extends MobRenderer<TrolldenEntity, Modelwarden3<TrolldenEntity>> {
	public TrolldenRenderer(EntityRendererProvider.Context context) {
		super(context, new Modelwarden3<TrolldenEntity>(context.bakeLayer(Modelwarden3.LAYER_LOCATION)), 1.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(TrolldenEntity entity) {
		return new ResourceLocation("trollden:textures/entities/my-warden-on-planetminecraft-com.png");
	}
}
