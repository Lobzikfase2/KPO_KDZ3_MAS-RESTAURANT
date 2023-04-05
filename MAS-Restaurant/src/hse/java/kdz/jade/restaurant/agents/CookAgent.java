package hse.java.kdz.jade.restaurant.agents;

import hse.java.kdz.jade.restaurant.types.Cook;
import jade.domain.FIPAAgentManagement.Property;

/**
 * Класса агента повара ресторана.
 * Является агентом конкретного ресурса ресторана
 */
public class CookAgent extends ResourceAgent {

    @Override
    protected void setup() {
        Cook cook = (Cook) getArguments()[0];
        status = Status.NOT_RESERVED;
        register("Cook", new Property("id", cook.getId()));
        report("был создан");
        addBehaviour(new ReservationBehaviour(cook.getId(), "Cook-reserving", ""));
        addBehaviour(new DeleteBehaviour());
    }
}
