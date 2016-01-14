package nl.tue.iot;

import org.apache.commons.lang.StringUtils;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.security.SecurityStore;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rparra on 12/1/16.
 */
public class SensingRegistrationHandler extends RegistrationHandler {

    private SecurityStore securityStore;
    private ClientRegistry clientRegistry;
    private Map<String, Integer> stateRegistry;

    public SensingRegistrationHandler(ClientRegistry clientRegistry, SecurityStore securityStore) {
        super(clientRegistry, securityStore);
        this.securityStore = securityStore;
        this.clientRegistry = clientRegistry;
        this.stateRegistry = new HashMap<>();
    }

    @Override
    public RegisterResponse register(Identity sender, RegisterRequest registerRequest, InetSocketAddress serverEndpoint) {
        RegisterResponse response = super.register(sender, registerRequest, serverEndpoint);
        final Client client = clientRegistry.findByRegistrationId(response.getRegistrationID());

        String yValueTarget = "/3345/0/5703";
        String clientAddr = client.getAddress().getHostAddress();
        String uriPrefix = "coap://" + clientAddr + ":" + Integer.toString(client.getPort());

        String stateTarget = "/32700/0/32801";
        final CoapClient stateClient = new CoapClient(uriPrefix + stateTarget);

        stateRegistry.put(client.getRegistrationId(), -100);

        CoapClient yValueCoapClient = new CoapClient(uriPrefix + yValueTarget);
        System.out.println(uriPrefix + yValueTarget);

        yValueCoapClient.observe(new CoapHandler() {

            @Override public void onLoad(CoapResponse response) {
                float yValue = Float.parseFloat(response.getResponseText());
                String state = "";

                if(yValue == 100 && yValue != stateRegistry.get(client.getRegistrationId())) {
                    stateClient.put("occupied", MediaTypeRegistry.TEXT_PLAIN);
                    System.out.println("changed to ocuppied");
                    stateRegistry.put(client.getRegistrationId(), 100);
                }

                if(yValue == -100 && yValue != stateRegistry.get(client.getRegistrationId())) {
                    stateClient.put("free", MediaTypeRegistry.TEXT_PLAIN);
                    System.out.println("changed to free");
                    stateRegistry.put(client.getRegistrationId(), -100);
                }
            }

            @Override public void onError() {
                System.err.println("Error on setting the observe relation");
            }
        });

        //ObserveResponse cResponse = server.send(client, request, TIMEOUT);

        //Client client = server.getClientRegistry().get(clientEndpoint);

        return response;
    }

}
