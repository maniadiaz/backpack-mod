package com.example.examplemod;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Original leather pack geometry, shared by the player and mob render layers. */
final class BackpackModel {
    static Model.Simple create() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, 1, 2.8F, 8, 10, 4), PartPose.ZERO);
        root.addOrReplaceChild("front_pocket", CubeListBuilder.create().texOffs(0, 16).addBox(-3, 6, 6.8F, 6, 4, 1), PartPose.ZERO);
        root.addOrReplaceChild("flap", CubeListBuilder.create().texOffs(24, 0).addBox(-4.5F, 0.5F, 2.5F, 9, 2, 5), PartPose.ZERO);
        root.addOrReplaceChild("buckle", CubeListBuilder.create().texOffs(48, 16).addBox(-1, 2.5F, 7.5F, 2, 2, 0.5F), PartPose.ZERO);
        root.addOrReplaceChild("left_pocket", CubeListBuilder.create().texOffs(0, 32).addBox(-6, 5, 3.3F, 2, 5, 3), PartPose.ZERO);
        root.addOrReplaceChild("right_pocket", CubeListBuilder.create().texOffs(0, 32).addBox(4, 5, 3.3F, 2, 5, 3), PartPose.ZERO);
        root.addOrReplaceChild("left_strap", CubeListBuilder.create().texOffs(24, 16).addBox(-3.5F, 0, -2.7F, 1.5F, 10, 0.6F), PartPose.ZERO);
        root.addOrReplaceChild("right_strap", CubeListBuilder.create().texOffs(24, 16).addBox(2, 0, -2.7F, 1.5F, 10, 0.6F), PartPose.ZERO);
        root.addOrReplaceChild("left_shoulder", CubeListBuilder.create().texOffs(24, 32).addBox(-3.5F, -0.4F, -2.7F, 1.5F, 0.6F, 6), PartPose.ZERO);
        root.addOrReplaceChild("right_shoulder", CubeListBuilder.create().texOffs(24, 32).addBox(2, -0.4F, -2.7F, 1.5F, 0.6F, 6), PartPose.ZERO);
        root.addOrReplaceChild("handle_top", CubeListBuilder.create().texOffs(24, 16).addBox(-2, -1.5F, 4, 4, 0.8F, 1), PartPose.ZERO);
        root.addOrReplaceChild("handle_left", CubeListBuilder.create().texOffs(24, 16).addBox(-2, -0.8F, 4, 0.8F, 1.8F, 1), PartPose.ZERO);
        root.addOrReplaceChild("handle_right", CubeListBuilder.create().texOffs(24, 16).addBox(1.2F, -0.8F, 4, 0.8F, 1.8F, 1), PartPose.ZERO);
        return new Model.Simple(LayerDefinition.create(mesh, 64, 64).bakeRoot(), RenderTypes::entityCutout);
    }
}
