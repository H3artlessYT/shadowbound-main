package com.h3artless.shadowbound.ritual;

import java.util.List;

public final class RitualPatterns {
    public static final int[][] DIRS = {{1, 0}, {0, 1}, {-1, 1}, {-1, 0}, {0, -1}, {1, -1}};
    private static final String TURNS = "wedsaq";
    public static final int MAX_LENGTH = 48;

    private RitualPatterns() {}

    public static int dirBetween(int q0, int r0, int q1, int r1) {
        int dq = q1 - q0, dr = r1 - r0;
        for (int i = 0; i < 6; i++) {
            if (DIRS[i][0] == dq && DIRS[i][1] == dr) return i;
        }
        return -1;
    }

    public static String signature(List<int[]> path) {
        if (path.size() < 2) return "";
        StringBuilder sb = new StringBuilder();
        int prev = dirBetween(path.get(0)[0], path.get(0)[1], path.get(1)[0], path.get(1)[1]);
        for (int i = 2; i < path.size(); i++) {
            int d = dirBetween(path.get(i - 1)[0], path.get(i - 1)[1], path.get(i)[0], path.get(i)[1]);
            sb.append(TURNS.charAt(Math.floorMod(d - prev, 6)));
            prev = d;
        }
        if (sb.length() == 0) sb.append('-');
        return sb.toString();
    }

    public static String reverse(String sig) {
        StringBuilder sb = new StringBuilder(sig.length());
        for (int i = sig.length() - 1; i >= 0; i--) {
            char c = sig.charAt(i);
            sb.append(switch (c) {
                case 'q' -> 'e';
                case 'e' -> 'q';
                case 'a' -> 'd';
                case 'd' -> 'a';
                default -> c;
            });
        }
        return sb.toString();
    }

    public static int[][] walk(String sig, int startDir) {
        int[][] pts = new int[sig.length() + 2][];
        int q = 0, r = 0, d = startDir;
        pts[0] = new int[]{q, r};
        q += DIRS[d][0];
        r += DIRS[d][1];
        pts[1] = new int[]{q, r};
        for (int i = 0; i < sig.length(); i++) {
            int turn = TURNS.indexOf(sig.charAt(i));
            d = Math.floorMod(d + Math.max(turn, 0), 6);
            q += DIRS[d][0];
            r += DIRS[d][1];
            pts[i + 2] = new int[]{q, r};
        }
        return pts;
    }

    public static float x(int q, int r) {
        return q + r * 0.5f;
    }

    public static float y(int r) {
        return r * 0.8660254f;
    }
}
