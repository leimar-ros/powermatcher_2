package net.powermatcher.runtime.agents;

import java.util.HashSet;
import java.util.Set;

import net.powermatcher.api.Agent;
import net.powermatcher.api.AgentEndpoint;
import net.powermatcher.api.MatcherEndpoint;
import net.powermatcher.api.TimeService;
import net.powermatcher.runtime.time.LoggingScheduler;
import net.powermatcher.runtime.time.SystemTimeService;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

@Component(immediate = true)
public class AgentInitializer {

    private final Set<Agent> agents = new HashSet<Agent>();
    private final LoggingScheduler scheduler = new LoggingScheduler();
    private final TimeService timeService = new SystemTimeService();

    @Activate
    public void activate() {
    }

    @Reference(dynamic = true, multiple = true, optional = true)
    public void addAgentEndpoint(AgentEndpoint agentEndpoint) {
        addAgent(agentEndpoint);
    }

    public void removeAgentEndpoint(AgentEndpoint agentEndpoint) {
        removeAgent(agentEndpoint);
    }

    @Reference(dynamic = true, multiple = true, optional = true)
    public void addMatcherEndpoint(MatcherEndpoint matcherEndpoint) {
        addAgent(matcherEndpoint);
    }

    public void removeMatcherEndpoint(MatcherEndpoint matcherEndpoint) {
        removeAgent(matcherEndpoint);
    }

    private synchronized void addAgent(Agent agent) {
        if (!agents.contains(agent)) {
            agent.setExecutorService(scheduler);
            agent.setTimeService(timeService);
            agents.add(agent);
        }
    }

    private synchronized void removeAgent(Agent agent) {
        agents.remove(agent);
    }

}
