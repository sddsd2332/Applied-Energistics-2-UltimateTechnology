/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.client.render.cablebus;


import appeng.client.render.VertexFormats;
import com.google.common.base.Preconditions;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;

import javax.vecmath.Vector4f;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;


/**
 * Builds the quads for a cube.
 */
public class CubeBuilder {

    private VertexFormat format;

    private final List<BakedQuad> output;

    private final EnumMap<EnumFacing, TextureAtlasSprite> textures = new EnumMap<>(EnumFacing.class);

    private EnumSet<EnumFacing> drawFaces = EnumSet.allOf(EnumFacing.class);

    private final EnumMap<EnumFacing, Vector4f> customUv = new EnumMap<>(EnumFacing.class);

    private final byte[] uvRotations = new byte[EnumFacing.values().length];

    private int color = 0xFFFFFFFF;

    private boolean useStandardUV = false;

    private boolean renderFullBright;

    public CubeBuilder(VertexFormat format, List<BakedQuad> output) {
        this.output = output;
        this.format = format;
    }

    public CubeBuilder(VertexFormat format) {
        this(format, new ArrayList<>(6));
    }

    public void addCube(float x1, float y1, float z1, float x2, float y2, float z2) {
        x1 /= 16.0f;
        y1 /= 16.0f;
        z1 /= 16.0f;
        x2 /= 16.0f;
        y2 /= 16.0f;
        z2 /= 16.0f;

        // If brightness is forced to specific values, extend the vertex format to contain the multi-texturing lightmap
        // offset
        VertexFormat savedFormat = null;
        if (this.renderFullBright) {
            savedFormat = this.format;
            this.format = VertexFormats.getFormatWithLightMap(this.format);
        }

        for (EnumFacing face : this.drawFaces) {
            this.putFace(face, x1, y1, z1, x2, y2, z2);
        }

        // Restore old format
        if (savedFormat != null) {
            this.format = savedFormat;
        }
    }

    public void addQuad(EnumFacing face, float x1, float y1, float z1, float x2, float y2, float z2) {
        // If brightness is forced to specific values, extend the vertex format to contain the multi-texturing lightmap
        // offset
        VertexFormat savedFormat = null;
        if (this.renderFullBright) {
            savedFormat = this.format;
            this.format = new VertexFormat(savedFormat);
            if (!this.format.getElements().contains(DefaultVertexFormats.TEX_2S)) {
                this.format.addElement(DefaultVertexFormats.TEX_2S);
            }
        }

        this.putFace(face, x1, y1, z1, x2, y2, z2);

        // Restore old format
        if (savedFormat != null) {
            this.format = savedFormat;
        }
    }

    private static final class UvVector {
        float u1;
        float u2;
        float v1;
        float v2;
    }

    private void putFace(EnumFacing face, float x1, float y1, float z1, float x2, float y2, float z2) {

        TextureAtlasSprite texture = this.textures.get(face);

        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(this.format);
        builder.setTexture(texture);
        builder.setQuadOrientation(face);
        builder.setQuadTint(-1);
        builder.setApplyDiffuseLighting(true);

        UvVector uv = new UvVector();

        // The user might have set specific UV coordinates for this face
        Vector4f customUv = this.customUv.get(face);
        if (customUv != null) {
            uv.u1 = texture.getInterpolatedU(customUv.x);
            uv.v1 = texture.getInterpolatedV(customUv.y);
            uv.u2 = texture.getInterpolatedU(customUv.z);
            uv.v2 = texture.getInterpolatedV(customUv.w);
        } else if (this.useStandardUV) {
            uv = this.getStandardUv(face, texture, x1, y1, z1, x2, y2, z2);
        } else {
            uv = this.getDefaultUv(face, texture, x1, y1, z1, x2, y2, z2);
        }

        switch (face) {
            case DOWN:
                this.putVertexTR(builder, face, x2, y1, z1, uv);
                this.putVertexBR(builder, face, x2, y1, z2, uv);
                this.putVertexBL(builder, face, x1, y1, z2, uv);
                this.putVertexTL(builder, face, x1, y1, z1, uv);
                break;
            case UP:
                this.putVertexTL(builder, face, x1, y2, z1, uv);
                this.putVertexBL(builder, face, x1, y2, z2, uv);
                this.putVertexBR(builder, face, x2, y2, z2, uv);
                this.putVertexTR(builder, face, x2, y2, z1, uv);
                break;
            case NORTH:
                this.putVertexBR(builder, face, x2, y2, z1, uv);
                this.putVertexTR(builder, face, x2, y1, z1, uv);
                this.putVertexTL(builder, face, x1, y1, z1, uv);
                this.putVertexBL(builder, face, x1, y2, z1, uv);
                break;
            case SOUTH:
                this.putVertexBL(builder, face, x1, y2, z2, uv);
                this.putVertexTL(builder, face, x1, y1, z2, uv);
                this.putVertexTR(builder, face, x2, y1, z2, uv);
                this.putVertexBR(builder, face, x2, y2, z2, uv);
                break;
            case WEST:
                this.putVertexTL(builder, face, x1, y1, z1, uv);
                this.putVertexTR(builder, face, x1, y1, z2, uv);
                this.putVertexBR(builder, face, x1, y2, z2, uv);
                this.putVertexBL(builder, face, x1, y2, z1, uv);
                break;
            case EAST:
                this.putVertexBR(builder, face, x2, y2, z1, uv);
                this.putVertexBL(builder, face, x2, y2, z2, uv);
                this.putVertexTL(builder, face, x2, y1, z2, uv);
                this.putVertexTR(builder, face, x2, y1, z1, uv);
                break;
        }

        this.output.add(builder.build());
    }

    private UvVector getDefaultUv(EnumFacing face, TextureAtlasSprite texture, float x1, float y1, float z1, float x2, float y2, float z2) {

        UvVector uv = new UvVector();

        switch (face) {
            case DOWN:
                uv.u1 = texture.getInterpolatedU(x1 * 16);
                uv.v1 = texture.getInterpolatedV(z1 * 16);
                uv.u2 = texture.getInterpolatedU(x2 * 16);
                uv.v2 = texture.getInterpolatedV(z2 * 16);
                break;
            case UP:
                uv.u1 = texture.getInterpolatedU(x1 * 16);
                uv.v1 = texture.getInterpolatedV(z1 * 16);
                uv.u2 = texture.getInterpolatedU(x2 * 16);
                uv.v2 = texture.getInterpolatedV(z2 * 16);
                break;
            case NORTH:
                uv.u1 = texture.getInterpolatedU(x1 * 16);
                uv.v1 = texture.getInterpolatedV(16 - y1 * 16);
                uv.u2 = texture.getInterpolatedU(x2 * 16);
                uv.v2 = texture.getInterpolatedV(16 - y2 * 16);
                break;
            case SOUTH:
                uv.u1 = texture.getInterpolatedU(x1 * 16);
                uv.v1 = texture.getInterpolatedV(16 - y1 * 16);
                uv.u2 = texture.getInterpolatedU(x2 * 16);
                uv.v2 = texture.getInterpolatedV(16 - y2 * 16);
                break;
            case WEST:
                uv.u1 = texture.getInterpolatedU(z1 * 16);
                uv.v1 = texture.getInterpolatedV(16 - y1 * 16);
                uv.u2 = texture.getInterpolatedU(z2 * 16);
                uv.v2 = texture.getInterpolatedV(16 - y2 * 16);
                break;
            case EAST:
                uv.u1 = texture.getInterpolatedU(z2 * 16);
                uv.v1 = texture.getInterpolatedV(16 - y1 * 16);
                uv.u2 = texture.getInterpolatedU(z1 * 16);
                uv.v2 = texture.getInterpolatedV(16 - y2 * 16);
                break;
        }

        return uv;
    }

    private UvVector getStandardUv(EnumFacing face, TextureAtlasSprite texture, float x1, float y1, float z1, float x2, float y2, float z2) {
        UvVector uv = new UvVector();
        switch (face) {
            case DOWN:
                uv.u1 = texture.getInterpolatedU(x1 * 16);
                uv.v1 = texture.getInterpolatedV(16 - z1 * 16);
                uv.u2 = texture.getInterpolatedU(x2 * 16);
                uv.v2 = texture.getInterpolatedV(16 - z2 * 16);
                break;
            case UP:
                uv.u1 = texture.getInterpolatedU(x1 * 16);
                uv.v1 = texture.getInterpolatedV(z1 * 16);
                uv.u2 = texture.getInterpolatedU(x2 * 16);
                uv.v2 = texture.getInterpolatedV(z2 * 16);
                break;
            case NORTH:
                uv.u1 = texture.getInterpolatedU(16 - x1 * 16);
                uv.v1 = texture.getInterpolatedV(16 - y1 * 16);
                uv.u2 = texture.getInterpolatedU(16 - x2 * 16);
                uv.v2 = texture.getInterpolatedV(16 - y2 * 16);
                break;
            case SOUTH:
                uv.u1 = texture.getInterpolatedU(x1 * 16);
                uv.v1 = texture.getInterpolatedV(16 - y1 * 16);
                uv.u2 = texture.getInterpolatedU(x2 * 16);
                uv.v2 = texture.getInterpolatedV(16 - y2 * 16);
                break;
            case WEST:
                uv.u1 = texture.getInterpolatedU(z1 * 16);
                uv.v1 = texture.getInterpolatedV(16 - y1 * 16);
                uv.u2 = texture.getInterpolatedU(z2 * 16);
                uv.v2 = texture.getInterpolatedV(16 - y2 * 16);
                break;
            case EAST:
                uv.u1 = texture.getInterpolatedU(16 - z2 * 16);
                uv.v1 = texture.getInterpolatedV(16 - y1 * 16);
                uv.u2 = texture.getInterpolatedU(16 - z1 * 16);
                uv.v2 = texture.getInterpolatedV(16 - y2 * 16);
                break;
        }
        return uv;
    }

    // uv.u1, uv.v1
    private void putVertexTL(UnpackedBakedQuad.Builder builder, EnumFacing face, float x, float y, float z, UvVector uv) {
        float u, v;

        switch (this.uvRotations[face.ordinal()]) {
            default:
            case 0:
                u = uv.u1;
                v = uv.v1;
                break;
            case 1: // 90° clockwise
                u = uv.u1;
                v = uv.v2;
                break;
            case 2: // 180° clockwise
                u = uv.u2;
                v = uv.v2;
                break;
            case 3: // 270° clockwise
                u = uv.u2;
                v = uv.v1;
                break;
        }

        this.putVertex(builder, face, x, y, z, u, v);
    }

    // uv.u2, uv.v1
    private void putVertexTR(UnpackedBakedQuad.Builder builder, EnumFacing face, float x, float y, float z, UvVector uv) {
        float u, v;

        switch (this.uvRotations[face.ordinal()]) {
            default:
            case 0:
                u = uv.u2;
                v = uv.v1;
                break;
            case 1: // 90° clockwise
                u = uv.u1;
                v = uv.v1;
                break;
            case 2: // 180° clockwise
                u = uv.u1;
                v = uv.v2;
                break;
            case 3: // 270° clockwise
                u = uv.u2;
                v = uv.v2;
                break;
        }
        this.putVertex(builder, face, x, y, z, u, v);
    }

    // uv.u2, uv.v2
    private void putVertexBR(UnpackedBakedQuad.Builder builder, EnumFacing face, float x, float y, float z, UvVector uv) {

        float u;
        float v;

        switch (this.uvRotations[face.ordinal()]) {
            default:
            case 0:
                u = uv.u2;
                v = uv.v2;
                break;
            case 1: // 90° clockwise
                u = uv.u2;
                v = uv.v1;
                break;
            case 2: // 180° clockwise
                u = uv.u1;
                v = uv.v1;
                break;
            case 3: // 270° clockwise
                u = uv.u1;
                v = uv.v2;
                break;
        }

        this.putVertex(builder, face, x, y, z, u, v);
    }

    // uv.u1, uv.v2
    private void putVertexBL(UnpackedBakedQuad.Builder builder, EnumFacing face, float x, float y, float z, UvVector uv) {

        float u;
        float v;

        switch (this.uvRotations[face.ordinal()]) {
            default:
            case 0:
                u = uv.u1;
                v = uv.v2;
                break;
            case 1: // 90° clockwise
                u = uv.u2;
                v = uv.v2;
                break;
            case 2: // 180° clockwise
                u = uv.u2;
                v = uv.v1;
                break;
            case 3: // 270° clockwise
                u = uv.u1;
                v = uv.v1;
                break;
        }

        this.putVertex(builder, face, x, y, z, u, v);
    }

    private void putVertex(UnpackedBakedQuad.Builder builder, EnumFacing face, float x, float y, float z, float u, float v) {
        VertexFormat format = builder.getVertexFormat();

        for (int i = 0; i < format.getElementCount(); i++) {
            VertexFormatElement e = format.getElement(i);
            switch (e.getUsage()) {
                case POSITION:
                    builder.put(i, x, y, z);
                    break;
                case NORMAL:
                    builder.put(i, face.getFrontOffsetX(), face.getFrontOffsetY(), face.getFrontOffsetZ());
                    break;
                case COLOR:
                    // Color format is RGBA
                    float r = (this.color >> 16 & 0xFF) / 255f;
                    float g = (this.color >> 8 & 0xFF) / 255f;
                    float b = (this.color & 0xFF) / 255f;
                    float a = (this.color >> 24 & 0xFF) / 255f;
                    builder.put(i, r, g, b, a);
                    break;
                case UV:
                    if (e.getIndex() == 0) {
                        builder.put(i, u, v);
                    } else {
                        // Force Brightness to 15, this is for full bright mode
                        // this vertex element will only be present in that case
                        final float lightMapU = (float) (15 * 0x20) / 0xFFFF;
                        final float lightMapV = (float) (15 * 0x20) / 0xFFFF;
                        builder.put(i, lightMapU, lightMapV);
                    }
                    break;
                default:
                    builder.put(i);
                    break;
            }
        }
    }

    public void setTexture(TextureAtlasSprite texture) {
        for (EnumFacing face : EnumFacing.values()) {
            this.textures.put(face, texture);
        }
    }

    public void setTextures(TextureAtlasSprite up, TextureAtlasSprite down, TextureAtlasSprite north, TextureAtlasSprite south, TextureAtlasSprite east, TextureAtlasSprite west) {
        this.textures.put(EnumFacing.UP, up);
        this.textures.put(EnumFacing.DOWN, down);
        this.textures.put(EnumFacing.NORTH, north);
        this.textures.put(EnumFacing.SOUTH, south);
        this.textures.put(EnumFacing.EAST, east);
        this.textures.put(EnumFacing.WEST, west);
    }

    public void setTexture(EnumFacing facing, TextureAtlasSprite sprite) {
        this.textures.put(facing, sprite);
    }

    public void setDrawFaces(EnumSet<EnumFacing> drawFaces) {
        this.drawFaces = drawFaces;
    }

    public void setColor(int color) {
        this.color = color;
    }

    /**
     * Sets the vertex color for future vertices to the given RGB value, and forces the alpha component to 255.
     */
    public void setColorRGB(int color) {
        this.setColor(color | 0xFF000000);
    }

    public void setColorRGB(float r, float g, float b) {
        this.setColorRGB((int) (r * 255) << 16 | (int) (g * 255) << 8 | (int) (b * 255));
    }

    public void setRenderFullBright(boolean renderFullBright) {
        this.renderFullBright = renderFullBright;
    }

    public void setCustomUv(EnumFacing facing, float u1, float v1, float u2, float v2) {
        this.customUv.put(facing, new Vector4f(u1, v1, u2, v2));
    }

    public void setUvRotation(EnumFacing facing, int rotation) {
        if (rotation == 2) {
            rotation = 3;
        } else if (rotation == 3) {
            rotation = 2;
        }
        Preconditions.checkArgument(rotation >= 0 && rotation <= 3, "rotation");
        this.uvRotations[facing.ordinal()] = (byte) rotation;
    }

    /**
     * CubeBuilder uses UV optimized for cables by default.
     * This switches to standard UV coordinates.
     */
    public void useStandardUV() {
        this.useStandardUV = true;
    }

    public List<BakedQuad> getOutput() {
        return this.output;
    }
}
