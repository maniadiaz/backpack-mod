package com.example.examplemod;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client classes stay isolated so a dedicated server never loads render APIs. */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class BackpackClient {
    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityTypes.ZOMBIE, BackpackZombieRenderer::new);
        event.registerEntityRenderer(EntityTypes.SKELETON, BackpackSkeletonRenderer::new);
        event.registerEntityRenderer(EntityTypes.CREEPER, BackpackCreeperRenderer::new);
    }

    @SubscribeEvent
    public static void playerLayers(EntityRenderersEvent.AddLayers event) {
        for (var type : event.getModelTypes()) {
            var candidate = event.getPlayerRenderer(type);
            if (!(candidate instanceof AvatarRenderer<?> renderer)) continue;
            if (renderer != null) renderer.addLayer(new BackpackLayer<>(renderer));
        }
    }

    private interface CarrierState { boolean hasBackpack(); }
    private static final class BackpackZombieState extends ZombieRenderState implements CarrierState {
        boolean backpack;
        public boolean hasBackpack() { return backpack; }
    }
    private static final class BackpackSkeletonState extends SkeletonRenderState implements CarrierState {
        boolean backpack;
        public boolean hasBackpack() { return backpack; }
    }
    private static final class BackpackCreeperState extends CreeperRenderState implements CarrierState {
        boolean backpack;
        public boolean hasBackpack() { return backpack; }
    }

    private static final class BackpackZombieRenderer extends ZombieRenderer {
        BackpackZombieRenderer(EntityRendererProvider.Context context) { super(context); addLayer(new BackpackLayer<>(this)); }
        @Override public ZombieRenderState createRenderState() { return new BackpackZombieState(); }
        @Override public void extractRenderState(Zombie entity, ZombieRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            if (state instanceof BackpackZombieState carrier) carrier.backpack = MobBackpacks.isCarrier(entity);
        }
    }
    private static final class BackpackSkeletonRenderer extends SkeletonRenderer {
        BackpackSkeletonRenderer(EntityRendererProvider.Context context) { super(context); addLayer(new BackpackLayer<>(this)); }
        @Override public SkeletonRenderState createRenderState() { return new BackpackSkeletonState(); }
        @Override public void extractRenderState(Skeleton entity, SkeletonRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            if (state instanceof BackpackSkeletonState carrier) carrier.backpack = MobBackpacks.isCarrier(entity);
        }
    }
    private static final class BackpackCreeperRenderer extends CreeperRenderer {
        BackpackCreeperRenderer(EntityRendererProvider.Context context) { super(context); addLayer(new BackpackLayer<>(this)); }
        @Override public CreeperRenderState createRenderState() { return new BackpackCreeperState(); }
        @Override public void extractRenderState(Creeper entity, CreeperRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            if (state instanceof BackpackCreeperState carrier) carrier.backpack = MobBackpacks.isCarrier(entity);
        }
    }

    private static final class BackpackLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
        private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "textures/entity/backpack.png");
        private final Model.Simple model;

        BackpackLayer(RenderLayerParent<S, M> parent) {
            super(parent);
            model = BackpackModel.create();
        }

        @Override public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
            boolean equipped = state instanceof CarrierState carrier && carrier.hasBackpack();
            BackpackLevel level = BackpackLevel.LEATHER;
            if (state instanceof AvatarRenderState avatar && Minecraft.getInstance().level != null) {
                equipped = !avatar.isSpectator && Minecraft.getInstance().level.getEntity(avatar.id) instanceof Player player
                    && EquippedBackpack.has(player);
                if (Minecraft.getInstance().level.getEntity(avatar.id) instanceof Player player) {
                    level = BackpackLevel.of(EquippedBackpack.get(player));
                }
            }
            if (state.isInvisible || !equipped) return;
            pose.pushPose();
            getParentModel().root().getChild("body").translateAndRotate(pose);
            Identifier texture = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "textures/entity/" + level.itemId() + ".png");
            collector.submitModel(model, Unit.INSTANCE, pose, texture, light, LivingEntityRenderer.getOverlayCoords(state, 0), state.outlineColor);
            pose.popPose();
        }
    }
}
