package com.jkantrell.mc.underilla.core.api;

public interface Block {

    boolean isAir();
    boolean isSolid();
    boolean isSolidAndSurfaceBlock();
    boolean isLiquid();
    boolean isWaterloggable();
    void waterlog();
    String getName();
    String getNameSpace();

}
