package com.urbanelf.ima.ui;

import java.awt.Font;

import javax.swing.UIManager;

public class Fonts {
    public static final Font SMALL;

    private Fonts() {
    }

    static {
        final Font lafFont = UIManager.getFont("Label.font");
        SMALL = lafFont.deriveFont(Font.PLAIN, 12f);
    }
}
