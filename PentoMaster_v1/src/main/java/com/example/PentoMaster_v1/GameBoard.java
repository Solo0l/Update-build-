package com.example.PentoMaster_v1;

import java.util.List;

public class GameBoard {
    private static final int ROWS = 6;
    private static final int COLS = 10;
    private boolean[][] grid;
    private CellStatus[][] cellStatuses; // مصفوفة الحالات
    private int occupiedCount = 0;
    private int targetCells = ROWS * COLS;
    // تأكد أنها 10 أعمدة و 6 صفوف بالضبط لتطابق حجم البورد في الصورة
    private final int[][][] LEVELS = {
            {
                    {1,1,1,1,1,1,1,1,1,1},
                    {1,1,1,1,1,1,1,1,1,1},
                    {1,1,1,1,1,1,1,1,1,1},
                    {1,1,1,1,1,1,1,1,1,1},
                    {1,1,1,1,1,1,1,1,1,1},
                    {1,1,1,1,1,1,1,1,1,1}
            }
    };
    // أضف هذا داخل GameBoard.java
    public int[][] getCurrentMap() {
        return currentMap;
    }

        private int[][] currentMap;
        // تصميم المستويات (0 = جدار/محظور، 1 = مسموح)


    public void loadLevel(int levelNum) {
        this.currentMap = LEVELS[Math.min(levelNum, LEVELS.length - 1)];

        occupiedCount = 0;
        targetCells = 0;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = false;
                if (currentMap[r][c] == 1) targetCells++;
            }
        }
    }


    public GameBoard() {
        this.grid = new boolean[ROWS][COLS];
        this.cellStatuses = new CellStatus[ROWS][COLS];

        // جهّز حالات الخلايا مرة واحدة
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                cellStatuses[r][c] = new CellStatus();
                if (Math.random() < 0.1) {
                    cellStatuses[r][c].modifier = CellModifier.CRACKED;
                }
            }
        }

        loadLevel(0);
    }
    // تأكد من وجود هذه الدالة لكي لا ينهار كود الـ Main
    public void applyModifierEffects(Pentomino piece, int gridX, int gridY) {
        List<int[]> coords = piece.getCoordinates();
        for (int[] point : coords) {
            int tx = gridX + point[0];
            int ty = gridY + point[1];
            if (ty >= 0 && ty < ROWS && tx >= 0 && tx < COLS) {
                if (cellStatuses[ty][tx] != null) {
                    cellStatuses[ty][tx].applyHit();
                }
            }
        }
    }

    public boolean canPlacePiece(Pentomino piece, int gridX, int gridY) {
        List<int[]> coords = piece.getCoordinates();
        for (int[] point : coords) {
            int tx = gridX + point[0];
            int ty = gridY + point[1];

            if (tx < 0 || tx >= COLS || ty < 0 || ty >= ROWS) return false;

            if (currentMap[ty][tx] == 0) return false;
            if (grid[ty][tx]) return false;

            // ✅ منع الوضع إذا الخلية "مكسورة" وصارت غير قابلة للوضع

        }
        return true;
    }

    public void placePiece(Pentomino piece, int gridX, int gridY) {
        List<int[]> coords = piece.getCoordinates();
        for (int[] point : coords) {
            int tx = gridX + point[0];
            int ty = gridY + point[1];

            if (ty >= 0 && ty < ROWS && tx >= 0 && tx < COLS) {
                if (!grid[ty][tx]) { // منع العدّ مرتين
                    grid[ty][tx] = true;
                    occupiedCount++;
                }
            }
        }

        // ✅ طبق تأثيرات الفخاخ بعد الوضع
        applyModifierEffects(piece, gridX, gridY);
    }
    public boolean isLevelComplete() {
        // نمر على كل الخلية في البورد
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                // إذا كان المربع "مسموح للعب" (1) ولكنه "فارغ" (false)
                // معناها اللاعب للحين ما خلص الليفل
                if (currentMap[row][col] == 1 && !grid[row][col]) {
                    return false;
                }
            }
        }
        // إذا مر على كل المربعات ولقاها مليانة، يعني مبروك الفوز!
        return true;
    }
    // خوارزمية البحث التراجعي لحل اللوحة (Backtracking)
    public boolean solve(List<Pentomino> availablePieces) {
        // القاعدة الأساسية: إذا امتلأت اللوحة، فقد نجحنا
        if (isFull()) return true;

        // البحث عن أول خلية فارغة
        int row = -1, col = -1;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (!grid[r][c]) {
                    row = r; col = c;
                    break;
                }
            }
            if (row != -1) break;
        }

        // تجربة كل القطع المتاحة
        for (int i = 0; i < availablePieces.size(); i++) {
            Pentomino piece = availablePieces.get(i);

            // تجربة كل الدورات الممكنة (4 دورات)
            for (int rot = 0; rot < 4; rot++) {
                if (canPlacePiece(piece, col, row)) {
                    placePiece(piece, col, row); // جرب الوضع

                    // استدعاء ذاتي (Recursion)
                    List<Pentomino> remaining = new java.util.ArrayList<>(availablePieces);
                    remaining.remove(i);
                    if (solve(remaining)) return true;

                    removePiece(piece, col, row); // تراجع (Backtrack) إذا فشل المسار
                }
                piece.rotate();
            }
        }
        return false; // لا يوجد حل لهذا المسار
    }

    public void removePiece(Pentomino piece, int gridX, int gridY) {
        List<int[]> coords = piece.getCoordinates();
        for (int[] point : coords) {
            int tx = gridX + point[0];
            int ty = gridY + point[1];
            if (tx >= 0 && tx < COLS && ty >= 0 && ty < ROWS) {
                if (grid[ty][tx]) {
                    grid[ty][tx] = false;
                    occupiedCount--;
                }
            }
        }
    }

    public boolean isOccupied(int x, int y) {
        if (x < 0 || x >= COLS || y < 0 || y >= ROWS) return true;
        return grid[y][x];
    }

    public boolean isFull() {
        return occupiedCount == targetCells;
    }

    public void clear() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                grid[i][j] = false;
            }
        }
        occupiedCount = 0;
    }

    public boolean isCracked(int x, int y) {
        if (x < 0 || x >= COLS || y < 0 || y >= ROWS) return false;

        // نتحقق إذا كان الـ Modifier هو CRACKED
        // وإذا كانت الصحة (health) لا تزال أكبر من 0
        return cellStatuses[y][x].modifier == CellModifier.CRACKED && cellStatuses[y][x].health > 0;
    }
}