package com.example.PentoMaster_v1;

public class CellStatus {
    public CellModifier modifier = CellModifier.NONE;
    public int health = 2;

    public boolean isPlaceable() {
        if (modifier == CellModifier.CRACKED && health <= 0) {
            return false;

        }return true;
    }

    public void applyHit() {
        if (modifier == CellModifier.CRACKED) {
            health--;
        }
    }
}