package a.gautham.neweditor.helper;

import android.graphics.Typeface;

public class TextStyleBuilder {

    private String text;
    private int textSize;
    private int text_color;
    private int[] padding;
    private Typeface typeface;
    private int bg_color;
    private boolean hasBg;
    private int paintflags;

    public TextStyleBuilder(String text, int textSize, int text_color, int[] padding, Typeface typeface,
                            int bg_color, boolean hasBg, int paintflags) {
        this.text = text;
        this.textSize = textSize;
        this.text_color = text_color;
        this.padding = padding;
        this.typeface = typeface;
        this.bg_color = bg_color;
        this.hasBg = hasBg;
        this.paintflags = paintflags;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getTextSize() {
        return textSize;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public int getText_color() {
        return text_color;
    }

    public void setText_color(int text_color) {
        this.text_color = text_color;
    }

    public int[] getPadding() {
        return padding;
    }

    public void setPadding(int[] padding) {
        this.padding = padding;
    }

    public Typeface getTypeface() {
        return typeface;
    }

    public void setTypeface(Typeface typeface) {
        this.typeface = typeface;
    }

    public int getBg_color() {
        return bg_color;
    }

    public void setBg_color(int bg_color) {
        this.bg_color = bg_color;
    }

    public boolean isHasBg() {
        return hasBg;
    }

    public void setHasBg(boolean hasBg) {
        this.hasBg = hasBg;
    }

    public int getPaintflags() {
        return paintflags;
    }

    public void setPaintflags(int paintflags) {
        this.paintflags = paintflags;
    }
}