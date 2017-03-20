package LeapTestTeamCityBridgeGroup;

/**
 * Created by Роман on 16.01.2017.
 */
import java.util.*;

public final class Runners {
    private static final TreeMap<String, RunnerVersion> AvailableRunners = new TreeMap<String, RunnerVersion>();

    public TreeMap<String, RunnerVersion> getAllRunners() { return AvailableRunners; }
    public Set<String> getSupportedVersions() { return AvailableRunners.descendingKeySet(); }
    public static RunnerVersion getRunner(String version) {
        return AvailableRunners.get(version);
    }

    static {
        AvailableRunners.put("1.1.0", new RunnerVersion("1.1.0") {
            @Override
            public String getRunnerPath() {
                StringBuilder sb = new StringBuilder();
                sb.append("ConsoleApp.exe");
                return sb.toString();
            }
        });
    }
}

