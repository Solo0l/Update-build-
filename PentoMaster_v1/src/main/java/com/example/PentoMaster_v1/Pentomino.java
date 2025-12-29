package com.example.PentoMaster_v1;

import javafx.scene.Group;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

import java.util.ArrayList;
import java.util.List;

public class Pentomino extends Group {

    private static final int BLOCK_SIZE = 50;

    private final List<int[]> coordinates = new ArrayList<>();
    private final Color color;

    public Pentomino(List<int[]> coords, Color c) {
        // نسخ آمن للإحداثيات (عشان ما يصير تعديل على نفس المصفوفة الأصلية)
        for (int[] p : coords) {
            this.coordinates.add(new int[]{p[0], p[1]});
        }
        this.color = c;

        // ✅ أهم شيء: خلي الإحداثيات تبدأ من (0,0) قبل الرسم
        normalize();
        draw();

        // Hover effects
        this.setOnMouseEntered(e -> {
            this.setEffect(new Glow(0.8));
            this.setScaleX(1.05);
            this.setScaleY(1.05);
        });

        this.setOnMouseExited(e -> {
            this.setEffect(null);
            this.setScaleX(1.0);
            this.setScaleY(1.0);
        });
    }

    public Color getColor() {
        return color;
    }

    public List<int[]> getCoordinates() {
        return coordinates;
    }

    // ✅ يجعل أقل X وأقل Y = 0 دائماً (يمنع الإحداثيات السالبة بعد الدوران/القلب)
    private void normalize() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;

        for (int[] p : coordinates) {
            if (p[0] < minX) minX = p[0];
            if (p[1] < minY) minY = p[1];
        }

        for (int[] p : coordinates) {
            p[0] -= minX;
            p[1] -= minY;
        }
    }

    private void draw() {
        this.getChildren().clear();

        for (int[] p : coordinates) {
            Rectangle rect = new Rectangle(BLOCK_SIZE, BLOCK_SIZE);
            rect.setFill(color);
            rect.setStroke(Color.BLACK);
            rect.setStrokeType(StrokeType.INSIDE);
            rect.setStrokeWidth(1);

            rect.setX(p[0] * BLOCK_SIZE);
            rect.setY(p[1] * BLOCK_SIZE);

            this.getChildren().add(rect);
        }
    }

    // دوران لليمين 90 درجة حول الأصل
    public void rotate() {
        for (int[] p : coordinates) {
            int x = p[0];
            int y = p[1];
            p[0] = -y;
            p[1] = x;
        }
        normalize();
        draw();
    }

    public void rotateLeft() {
        // دوران لليسار = 3 مرات يمين
        rotate();
        rotate();
        rotate();
    }

    // قلب أفقي
    public void flip() {
        for (int[] p : coordinates) {
            p[0] = -p[0];
        }
        normalize();
        draw();
    }
}