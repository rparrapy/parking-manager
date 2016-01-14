package nl.tue.iot;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.Iterator;

import nl.tue.iot.reservation.BillsServlet;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.californium.impl.RegisterResource;
import org.eclipse.leshan.server.impl.SecurityRegistryImpl;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.standalone.LeshanStandalone;
import org.eclipse.leshan.standalone.servlet.ClientServlet;
import org.eclipse.leshan.standalone.servlet.EventServlet;
import org.eclipse.leshan.standalone.servlet.ObjectSpecServlet;
import org.eclipse.leshan.standalone.servlet.SecurityServlet;
import nl.tue.iot.reservation.ReservationServlet;
import org.eclipse.leshan.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by rparra on 5/1/16.
 */
public class ParkingManager extends LeshanStandalone {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanStandalone.class);
    private Server server;
    private LeshanServer lwServer;

    public void start() {
        // Use those ENV variables for specifying the interface to be bound for coap and coaps
        String iface = System.getenv("COAPIFACE");
        String ifaces = System.getenv("COAPSIFACE");

        // Build LWM2M server
        LeshanServerBuilder builder = new LeshanServerBuilder();
        if (iface != null && !iface.isEmpty()) {
            builder.setLocalAddress(iface.substring(0, iface.lastIndexOf(':')),
                    Integer.parseInt(iface.substring(iface.lastIndexOf(':') + 1, iface.length())));
        }
        if (ifaces != null && !ifaces.isEmpty()) {
            builder.setLocalAddressSecure(ifaces.substring(0, ifaces.lastIndexOf(':')),
                    Integer.parseInt(ifaces.substring(ifaces.lastIndexOf(':') + 1, ifaces.length())));
        }

        // Get public and private server key
        PrivateKey privateKey = null;
        PublicKey publicKey = null;
        try {
            // Get point values
            byte[] publicX = Hex
                    .decodeHex("fcc28728c123b155be410fc1c0651da374fc6ebe7f96606e90d927d188894a73".toCharArray());
            byte[] publicY = Hex
                    .decodeHex("d2ffaa73957d76984633fc1cc54d0b763ca0559a9dff9706e9f4557dacc3f52a".toCharArray());
            byte[] privateS = Hex
                    .decodeHex("1dae121ba406802ef07c193c1ee4df91115aabd79c1ed7f4c0ef7ef6a5449400".toCharArray());

            // Get Elliptic Curve Parameter spec for secp256r1
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

            // Create key specs
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);

            // Get keys
            publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            privateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);

            builder.setSecurityRegistry(new SecurityRegistryImpl(privateKey, publicKey));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidParameterSpecException e) {
            LOG.warn("Unable to load RPK.", e);
        }

        lwServer = builder.build();
        lwServer.start();

        // Now prepare and start jetty
        String webPort = System.getenv("PORT");
        if (webPort == null || webPort.isEmpty()) {
            webPort = System.getProperty("PORT");
        }
        if (webPort == null || webPort.isEmpty()) {
            webPort = "8080";
        }
        server = new Server(Integer.valueOf(webPort));
        WebAppContext root = new WebAppContext();
        root.setContextPath("/");
        root.setResourceBase(this.getClass().getClassLoader().getResource("webapp").toExternalForm());
        root.setParentLoaderPriority(true);
        server.setHandler(root);

        // Create Servlet
        EventServlet eventServlet = new EventServlet(lwServer);
        ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        root.addServlet(eventServletHolder, "/event/*");

        ServletHolder clientServletHolder = new ServletHolder(new ClientServlet(lwServer));
        root.addServlet(clientServletHolder, "/api/clients/*");

        ServletHolder securityServletHolder = new ServletHolder(new SecurityServlet(lwServer.getSecurityRegistry()));
        root.addServlet(securityServletHolder, "/api/security/*");

        ServletHolder objectSpecServletHolder = new ServletHolder(new ObjectSpecServlet());
        root.addServlet(objectSpecServletHolder, "/api/objectspecs/*");

        ServletHolder billsServletHolder = new ServletHolder(new BillsServlet(lwServer));
        root.addServlet(billsServletHolder, "/api/bills/*");
        
        //new Servlet for reservation flow
        ServletHolder reservationServletHolder = new ServletHolder(
                new ReservationServlet(lwServer, lwServer.getSecureAddress().getPort()));
        root.addServlet(reservationServletHolder, "/api/reservation/*");

        startSensing();
        // Start jetty
        try {
            server.start();
        } catch (Exception e) {
            LOG.error("jetty error", e);
        }
    }

    private void startSensing() {
        CoapServer coapServer = this.lwServer.getCoapServer();
        // First we hotswap the registration handler
        // define /rd resource
        final RegisterResource rdResource = new RegisterResource(new SensingRegistrationHandler(this.lwServer.getClientRegistry(),
                this.lwServer.getSecurityRegistry()));

        for (Iterator<Resource> iterator = coapServer.getRoot().getChildren().iterator(); iterator.hasNext();) {
            Resource r = iterator.next();
            if(r instanceof RegisterResource) {
                iterator.remove();
            }
        }

        coapServer.add(rdResource);

            //this.lwServer.getCoapServer().add(rdResource);

    }

    public static void main(String[] args) {
        new ParkingManager().start();
    }
}
