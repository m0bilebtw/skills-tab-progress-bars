package com.github.m0bilebtw;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DarkenType {
    None("None"),
    Level99("Level 99"),
    LevelCustom("Custom Level"),
    XP200m("200m XP"),
    XPCustom("Custom XP");

    private final String displayName;

    @Override
    public String toString() {
        return displayName;
    }
}
