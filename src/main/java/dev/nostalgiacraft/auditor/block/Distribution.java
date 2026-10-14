package dev.nostalgiacraft.auditor.block;

import java.util.List;

public final class Distribution {
    @FunctionalInterface
    public interface Outlet {
        int offer(int count, boolean simulate);
    }

    private Distribution() {
    }

    public static int split(int count, List<? extends Outlet> outlets, int start, boolean simulate) {
        if (simulate || outlets.size() < 2) {
            return inOrder(count, outlets, simulate);
        }
        int size = outlets.size();
        int share = count / size;
        int rest = count % size;
        int accepted = 0;
        for (int index = 0; index < size; index++) {
            int offset = Math.floorMod(index - start, size);
            int portion = share + (offset < rest ? 1 : 0);
            if (portion > 0) {
                accepted += outlets.get(index).offer(portion, false);
            }
        }
        for (int index = 0; index < size && accepted < count; index++) {
            accepted += outlets.get(Math.floorMod(start + index, size)).offer(count - accepted, false);
        }
        return accepted;
    }

    public static int inOrder(int count, List<? extends Outlet> outlets, boolean simulate) {
        int accepted = 0;
        for (Outlet outlet : outlets) {
            if (accepted == count) {
                break;
            }
            accepted += outlet.offer(count - accepted, simulate);
        }
        return accepted;
    }
}
