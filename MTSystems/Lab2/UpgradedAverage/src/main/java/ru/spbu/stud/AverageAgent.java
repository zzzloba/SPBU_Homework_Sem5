package ru.spbu.stud;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import java.security.InvalidParameterException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class AverageAgent extends Agent {
    private double value;
    private AID receiverAID;
    private double protocolStep;
    // Shows if ready to send message to receiver
    private boolean isPending;
    private boolean isTriggered;

    private int leftReceived;
    private int totalValues = 1;

    public boolean isTriggered() {
        return isTriggered;
    }

    public boolean isCenter() {
        return Integer.parseInt(getAID().getLocalName()) == 0;
    }

    public double getValue() {
        return value;
    }

    public void sendMessage() {
        if (isPending && !isTriggered) {
            ACLMessage newMes = new ACLMessage(ACLMessage.INFORM);
            newMes.addReceiver(receiverAID);

            // Добавление помех, в данном случае некоторое число от -1 до 1
            var valueWithInterference = value + ThreadLocalRandom.current().nextDouble(-1, 1);
            newMes.setContent(valueWithInterference + ";" + totalValues);

            try {
                send(newMes);
            }
            finally {
                isPending = false;
                isTriggered = true;
            }
        }
    }

    public void proceedIncomingMessage(ACLMessage msg) {
        var args = msg.getContent().split(";");

        // Полученное сообщение с помехами
        var msgDoubleValue = Double.parseDouble(args[0]);
        var msgTotalValues = Integer.parseInt(args[1]);

        // Текущее значение с помехами
        var valueWithInterference = value + ThreadLocalRandom.current().nextDouble(-1, 1);

        value = value + protocolStep * (msgDoubleValue - valueWithInterference);

        if (isCenter()) {
            value = msgDoubleValue / msgTotalValues;
            return;
        }
        totalValues += msgTotalValues;
        leftReceived--;

        if (leftReceived == 0) {
            isPending = true;
        }
    }

    @Override
    protected void setup() {
        // Value in current node + total number of nodes including center node
        Object[] args = getArguments();

        if (args != null && args.length > 0) {
            if (args.length != 5) {
                throw new InvalidParameterException("Invalid parameters for agent setup");
            }

            value = Double.parseDouble(args[0].toString());

            // Number of agents without center element
            var numberOfAgents = Integer.parseInt(args[1].toString()) - 1;

            var id = Integer.parseInt(getAID().getLocalName());

            isPending = id % 2 == 1 && id != numberOfAgents;

            receiverAID = new AID(args[2].toString(), AID.ISLOCALNAME);
            leftReceived = Integer.parseInt(args[3].toString());
            protocolStep = Double.parseDouble(args[4].toString());
        }
        else {
            throw new InvalidParameterException("Invalid neighbours param");
        }

        addBehaviour(new FindAverageBehaviour(this, TimeUnit.SECONDS.toMillis(1)));
    }
}