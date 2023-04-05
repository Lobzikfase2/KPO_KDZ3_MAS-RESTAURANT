package hse.java.kdz.jade.restaurant.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.io.IOException;
import java.io.Serializable;

import static hse.java.kdz.jade.restaurant.tools.AgentTools.getAgentContainer;
import static hse.java.kdz.jade.restaurant.tools.AgentTools.stopSimulation;

/**
 * Абстрактный класс агента ресторана.
 * Является базовым классом для всех пользовательских агентов.
 */
public abstract class RestaurantAgent extends Agent {

    /**
     * Описание агента для взаимодействия с сервисом желтых страниц JADE
     */
    private DFAgentDescription dfd;

    /**
     * Метод регистрации 'сервиса' агента в сервисе желтых страниц JADE
     * @param name       - имя сервиса
     * @param properties - свойства, описывающие сервис
     */
    public void register(String name, Property... properties) {
        dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(name);
        sd.setName(name);
        for (var property : properties) {
            sd.addProperties(property);
        }
        dfd.addServices(sd);
        boolean success = false;
        int count = 0;
        while (!success) {
            try {
                DFService.register(this, dfd);
                success = true;
            } catch (Exception e) {
                count++;
                if (count >= 10) {
                    String report = "=================ВНУТРЕННЯЯ ОШИБКА ПЛАТФОРМЫ JADE================\n";
                    report += "\t-Не удалось зарегистрировать агента в DFService\n";
                    report += "\t-Просто перезапустите симуляцию. Если это не поможет, то попробуйте увеличить\n";
                    report += "\t значение параметра 'SIMULATION_DECELERATION_FACTOR";
                    System.out.println(report);
                    stopSimulation(this);
                    break;
                }
                sleep(200);
            }
        }
    }

    /**
     * Метод отмены регистрации 'сервиса' агента в сервисе желтых страниц JADE
     */
    public void deregister() {
        if (dfd != null) {
            try {
                DFService.deregister(this, dfd);
            } catch (Exception e) {
                report("отмена регистрации в сервисе желтых страниц не была закончена!");
            }
        }
    }

    /**
     * Метод поиска других агентов, агента в сервисе желтых страниц JADE
     * @param name       - имя, предоставляемого агентом сервиса
     * @param properties - свойства, описывающие сервис
     */
    public AID[] find(String name, Property... properties) {
        AID[] agents = new AID[0];
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setName(name);
        for (var property : properties) {
            sd.addProperties(property);
        }
        template.addServices(sd);
        boolean success = false;
        int count = 0;
        while (!success) {
            try {
                DFAgentDescription[] result = DFService.search(this, template);
                agents = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    agents[i] = result[i].getName();
                }
                success = true;
            } catch (Exception fe) {
                count++;
                if (count >= 3) {
                    String report = "=================ВНУТРЕННЯЯ ОШИБКА ПЛАТФОРМЫ JADE================\n";
                    report += "\t-Поиск агента в системе привел к критической ошибке\n";
                    report += "\t-Просто перезапустите симуляцию. Если это не поможет, то попробуйте увеличить\n";
                    report += "\t значение параметра 'SIMULATION_DECELERATION_FACTOR";
                    System.out.println(report);
                    stopSimulation(this);
                    break;
                }
                sleep(500);
            }
        }
        return agents;
    }

    /**
     * Метод создания и запуска нового агента в платформе JADE
     * @param name - имя агента в системе
     * @param tClass - класс создаваемого агента
     * @param args - объекты, передаваемые агенту при его инициализации
     */
    public <T> void create(String name, Class<T> tClass, Object... args) {
        AgentController ac;
        boolean success = false;
        int count = 0;
        while (!success) {
            try {
                if (count > 0) {
                    name += "[" + count + "]";
                }
                ac = getAgentContainer().createNewAgent(name, tClass.getName(), args);
            } catch (StaleProxyException e) {
                String report = "=================ВНУТРЕННЯЯ ОШИБКА ПЛАТФОРМЫ JADE================\n";
                report += "\t-Некоторые имена агентов совпадают (" + name + ")\n";
                report += "\t-Проверьте ваши входные файлы на уникальность значений";
                System.out.println(report);
                stopSimulation(this);
                return;
            }
            try {
                ac.start();
                success = true;
            } catch (StaleProxyException e) {
                sleep(200);
                count++;
                if (count > 10) {
                    String report = "=================ВНУТРЕННЯЯ ОШИБКА ПЛАТФОРМЫ JADE================\n";
                    report += "\t-Попытка создания агента в системе привела к критической ошибке\n";
                    report += "\t-Просто перезапустите симуляцию. Если это не поможет, то попробуйте увеличить\n";
                    report += "\t значение параметра 'SIMULATION_DECELERATION_FACTOR";
                    System.out.println(report);
                    stopSimulation(this);
                    return;
                }
            }
        }
    }

    /**
     * Метод для остановки агента на заданное время в миллисекундах
     * @param millis - количество миллисекунд
     */
    void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Метод переопределяет базовое поведение агента при его удалении
     */
    @Override
    protected void takeDown() {
        report("уничтожен");
        deregister();
    }


    /**
     * Метод для выведения агентом сообщения в консоль от его имени
     * @param string - текст сообщения
     */
    protected void report(String string) {
        System.out.println(getLocalName() + ": " + string);
    }

    /**
     * Метод для десериализации из сообщения объекта заданного типа
     * @param msg - сообщение агента
     * @return - объект из сообщения
     * @param <T> - тип объекта из сообщения
     */
    protected <T> T getObjectFromMsg(ACLMessage msg) {
        try {
            return (T) msg.getContentObject();
        } catch (UnreadableException e) {
            System.out.println("Ошибка десериализации объекта из сообщения!");
            stopSimulation(this);
            return null;
        }
    }

    /**
     * Метод для сериализации объекта заданного типа в сообщение
     * @param msg - сообщение
     * @param t - сериализуемый объект
     * @param <T> - тип объекта
     */
    protected <T extends Serializable> void setObjectToMsg(ACLMessage msg, T t) {
        try {
            msg.setContentObject(t);
        } catch (IOException e) {
            System.out.println("Ошибка сериализации объекта в сообщение!");
            stopSimulation(this);
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику удаления агента
     */
    protected class DeleteBehaviour extends Behaviour {
        private boolean isDone = false;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("delete"));
            ACLMessage msg = receive(mt);
            if (msg != null) {
                doDelete();
                isDone = true;
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return isDone;
        }
    }
}
