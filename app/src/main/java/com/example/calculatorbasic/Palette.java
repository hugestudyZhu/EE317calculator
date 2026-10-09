package com.example.calculatorbasic;

/** Semantic color roles, paired light/dark for each appearance. */
final class Palette {
    final int canvas, ink, muted, number, utility, operation, accent, accentText, ripple;
    final int secondary, secondarySurface, clear, clearText, functionText;
    final int scienceRadius, basicRadius;
    Palette(boolean dark, int appearance) {
        switch (appearance) {
            case 1:
                canvas=dark ? 0xFF111820 : 0xFFF1F5FA;
                number=dark ? 0xFF293540 : 0xFFFFFFFF;
                utility=dark ? 0xFF1B2631 : 0xFFE4EBF3;
                operation=dark ? 0xFF243C54 : 0xFFDAEAFB;
                accent=dark ? 0xFFA5CEFF : 0xFF245FA4;
                secondary=dark ? 0xFFC6BDF0 : 0xFF65558F;
                secondarySurface=dark ? 0xFF312C43 : 0xFFECE6FA;
                scienceRadius=12; basicRadius=20;
                break;
            case 2:
                canvas=dark ? 0xFF161C19 : 0xFFF5F6F0;
                number=dark ? 0xFF303A33 : 0xFFFFFFFF;
                utility=dark ? 0xFF222C26 : 0xFFE7ECE2;
                operation=dark ? 0xFF304639 : 0xFFDAEBD9;
                accent=dark ? 0xFFB7D7AA : 0xFF416A43;
                secondary=dark ? 0xFFE2CCA0 : 0xFF795E2D;
                secondarySurface=dark ? 0xFF393329 : 0xFFF1E8D5;
                scienceRadius=14; basicRadius=22;
                break;
            default:
                canvas=dark ? 0xFF141416 : 0xFFF5F5F7;
                number=dark ? 0xFF303034 : 0xFFFFFFFF;
                utility=dark ? 0xFF222225 : 0xFFE9E9EE;
                operation=dark ? 0xFF3C2C21 : 0xFFFBE7D8;
                accent=dark ? 0xFFFFB16E : 0xFFAD4D0F;
                secondary=dark ? 0xFFB4C8DD : 0xFF466581;
                secondarySurface=dark ? 0xFF26313C : 0xFFE0EAF3;
                scienceRadius=18; basicRadius=26;
        }
        ink=dark ? 0xFFF3F3F5 : 0xFF202124;
        muted=dark ? 0xFFA0A4AD : 0xFF646972;
        functionText=dark ? 0xFFCDD1D8 : 0xFF414B58;
        clear=dark ? 0xFF3D292C : 0xFFF9E4E6;
        clearText=dark ? 0xFFF0B4B8 : 0xFFA03D48;
        accentText=dark ? canvas : 0xFFFFFFFF;
        ripple=dark ? 0x22FFFFFF : 0x18000000;
    }
}
