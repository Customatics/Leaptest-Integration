package LeapTestTeamCityBridgeGroup;

import java.util.Set;
import java.util.TreeMap;

public final class DoneStatuses {
    private static final TreeMap<String, DoneStatus> AvailableStatuses = new TreeMap<String, DoneStatus>();

    public TreeMap<String, DoneStatus> getAllStatuses() { return AvailableStatuses; }
    public Set<String> getSupportedDoneStatuses() { return AvailableStatuses.descendingKeySet(); }
    public static DoneStatus getStatus(String status) {
        return AvailableStatuses.get(status);
    }

    static {
        AvailableStatuses.put("Success", new DoneStatus("Success") {});
        AvailableStatuses.put("Failed", new DoneStatus("Failed") {});

    }
}
