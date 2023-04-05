package hse.java.kdz.jade.restaurant.agents;

import jade.domain.FIPAAgentManagement.Property;
import hse.java.kdz.jade.restaurant.types.Equipment;

/**
 * Класса агента оборудования ресторана.
 * Является агентом конкретного ресурса ресторана
 */
public class EquipmentAgent extends ResourceAgent {

    @Override
    protected void setup() {
        Equipment equipment = (Equipment) getArguments()[0];
        status = Status.NOT_RESERVED;
        register("Equipment", new Property("equipmentType", equipment.getType()));
        report("был создан");
        addBehaviour(new ReservationBehaviour(equipment.getId(), "Equipment-reserving", String.valueOf(equipment.getType())));
        addBehaviour(new DeleteBehaviour());
    }
}
