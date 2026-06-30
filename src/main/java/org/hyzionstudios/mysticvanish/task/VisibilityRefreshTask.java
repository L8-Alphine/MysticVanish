package org.hyzionstudios.mysticvanish.task;

import org.hyzionstudios.mysticvanish.manager.VisibilityManager;

public final class VisibilityRefreshTask implements Runnable {
    private final VisibilityManager visibilityManager;

    public VisibilityRefreshTask(VisibilityManager visibilityManager) {
        this.visibilityManager = visibilityManager;
    }

    @Override
    public void run() {
        visibilityManager.updateAllVisibility();
    }
}
