package com.example.PentoMaster_v1;

import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ShapeData {

    // الأشكال الـ 12 القياسية
    private static final int[][][] SHAPES = {
            {{0,0}, {1,0}, {2,0}, {1,1}, {1,2}}, // T
            {{1,0}, {1,1}, {0,1}, {0,2}, {0,3}}, // N (Corrected)
            {{0,0}, {0,1}, {0,2}, {0,3}, {1,3}}, // L
            {{0,0}, {0,1}, {0,2}, {0,3}, {1,1}}, // Y
            {{0,0}, {1,0}, {1,1}, {1,2}, {2,2}}, // Z
            {{1,0}, {2,0}, {0,1}, {1,1}, {1,2}}, // F
            {{0,0}, {2,0}, {0,1}, {1,1}, {2,1}}, // U
            {{0,0}, {0,1}, {0,2}, {1,2}, {2,2}}, // V
            {{1,0}, {0,1}, {1,1}, {2,1}, {1,2}}, // X
            {{0,0}, {0,1}, {0,2}, {0,3}, {0,4}}, // I
            {{0,0}, {1,0}, {0,1}, {1,1}, {0,2}}, // P
            {{0,0}, {0,1}, {1,1}, {1,2}, {2,2}}  // W
    };

    // قائمة الألوان
    private static final Color[] COLORS = {
            Color.MAGENTA, Color.GREEN, Color.ORANGE, Color.BLUE,
            Color.YELLOW, Color.CYAN, Color.LIGHTGRAY, Color.LIME,
            Color.RED, Color.GOLD, Color.PURPLE, Color.TEAL
    };

    private static final Random random = new Random();

    // دالة إنشاء قطعة عشوائية (شكل عشوائي + لون عشوائي)
    public static Pentomino createRandomPiece() {
        int shapeIndex = random.nextInt(SHAPES.length);
        int[][] selectedShape = SHAPES[shapeIndex];

        List<int[]> coords = new ArrayList<>();
        Collections.addAll(coords, selectedShape);

        // اختيار لون عشوائي (غير مرتبط بالشكل) - متطلب Section 2
        Color color = COLORS[random.nextInt(COLORS.length)];

        return new Pentomino(coords, color);
    }

    public static Pentomino createPieceByIndex(int index) {
        int safeIndex = Math.abs(index % SHAPES.length);
        int[][] selectedShape = SHAPES[safeIndex];
        List<int[]> coords = new ArrayList<>();
        Collections.addAll(coords, selectedShape);
        Color color = COLORS[safeIndex % COLORS.length];
        return new Pentomino(coords, color);
    }
}