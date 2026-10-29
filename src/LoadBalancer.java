import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class LoadBalancer {
    // Volatile reference ensures instant thread visibility when swapping strategies
    private volatile LoadBalancingStrategy strategy;
    // CopyOnWriteArrayList enables thread-safe iteration without locking on reads
    private final List<ServerInstance> servers;

    public LoadBalancer(LoadBalancingStrategy initialStrategy) {
        this.strategy = Objects.requireNonNull(initialStrategy, "Strategy cannot be null");
        this.servers = new CopyOnWriteArrayList<>();
    }

    public void addServer(ServerInstance server) {
        if (!servers.contains(server)) {
            servers.add(server);
        }
    }

    public void removeServer(ServerInstance server) {
        servers.remove(server);
    }

    /**
     * Swaps the load balancing algorithm dynamically at runtime.
     */
    public void setStrategy(LoadBalancingStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "Strategy cannot be null");
    }

    public ServerInstance route(RequestContext context) {
        List<ServerInstance> healthyServers = servers.stream()
                .filter(ServerInstance::isHealthy)
                .collect(Collectors.toList());

        if (healthyServers.isEmpty()) {
            throw new IllegalStateException("No healthy backend instances available");
        }

        return strategy.selectInstance(healthyServers, context);
    }

    public ServerInstance route() {
        return route(null);
    }
}