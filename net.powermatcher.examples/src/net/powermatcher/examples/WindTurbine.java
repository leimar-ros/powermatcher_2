package net.powermatcher.examples;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.flexiblepower.context.FlexiblePowerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;
import net.powermatcher.api.AgentEndpoint;
import net.powermatcher.api.data.Bid;
import net.powermatcher.api.messages.PriceUpdate;
import net.powermatcher.api.monitoring.ObservableAgent;
import net.powermatcher.core.BaseAgentEndpoint;

@Component(designateFactory = WindTurbine.Config.class,
           immediate = true,
           provide = { ObservableAgent.class, AgentEndpoint.class })
public class WindTurbine
    extends BaseAgentEndpoint
    implements AgentEndpoint {

    // Important fields to assign the Agent's Logger, Agent cofiguration and scheduler
    private static final Logger LOGGER = LoggerFactory.getLogger(PVPanelAgent.class);

    // Variables that will be needed for simulating our WindTurbine bid
    private static Random generator = new Random();

    public static interface Config {
        @Meta.AD(deflt = "windturbine",
                 description = "The unique identifier of the agent")
        String agentId();

        @Meta.AD(deflt = "concentrator",
                 description = "The agent identifier of the parent matcher to which this agent should be connected")
        String desiredParentId();

        @Meta.AD(deflt = "5",
                 description = "Number of seconds between bid updates")
        long bidUpdateRate();

        @Meta.AD(deflt = "1",
                 description = "The minimum value of the random demand.")
        double minimumDemand();

        @Meta.AD(deflt = "2000000",
                 description = "The maximum value of the random demand.")
        double maximumDemand();
    }

    // A delayed result-bearing action that can be cancelled.
    private ScheduledFuture<?> scheduledFuture;

    // The minimum value of the random demand.
    private double minimumDemand;

    // The maximum value of the random demand.
    private double maximumDemand;

    private Config config;

    // OSGi calls this method to activate a managed service.
    @Activate
    public void activate(Map<String, Object> properties) {
        config = Configurable.createConfigurable(Config.class, properties);
        init(config.agentId(), config.desiredParentId());

        minimumDemand = config.minimumDemand();
        maximumDemand = config.maximumDemand();

        LOGGER.info("Agent [{}], activated", config.agentId());
    }

    // OSGi calls this method to deactivate a managed service.
    @Override
    @Deactivate
    public void deactivate() {
        super.deactivate();
        scheduledFuture.cancel(false);
        LOGGER.info("Agent [{}], deactivated", getAgentId());
    }

    // {@inheritDoc}
    void doBidUpdate() {
        AgentEndpoint.Status currentStatus = getStatus();
        if (currentStatus.isConnected()) {
            double demand = minimumDemand + (maximumDemand - minimumDemand) * generator.nextDouble();
            publishBid(Bid.flatDemand(currentStatus.getMarketBasis(), demand));
        }
    }

    @Override
    public synchronized void handlePriceUpdate(PriceUpdate priceUpdate) {
        super.handlePriceUpdate(priceUpdate);
        // Nothing to control for a Wind Turbine
    }

    @Override
    public void setContext(FlexiblePowerContext context) {
        super.setContext(context);
        scheduledFuture = context.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                doBidUpdate();
            }
        }, Measure.valueOf(0, SI.SECOND), Measure.valueOf(config.bidUpdateRate(), SI.SECOND));
    }

}
