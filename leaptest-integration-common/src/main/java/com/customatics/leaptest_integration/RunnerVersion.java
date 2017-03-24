package com.customatics.leaptest_integration;

/**
 * Created by Роман on 16.01.2017.
 */

public abstract class RunnerVersion {
    public final String version;
    public RunnerVersion(String version) {
        this.version = version;
    }
    public abstract String getRunnerPath();
}

