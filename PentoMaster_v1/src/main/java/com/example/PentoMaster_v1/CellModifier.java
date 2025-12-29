package com.example.PentoMaster_v1;

public enum CellModifier {
    NONE,
    CRACKED,  // تختفي الخلية بعد محاولتين وضع
    FROZEN,   // تمنع الوضع لمدة 5 ثواني إذا فعلت
    VOLATILE  // تضاعف خسارة الـ Stability (العداد الأحمر)
}