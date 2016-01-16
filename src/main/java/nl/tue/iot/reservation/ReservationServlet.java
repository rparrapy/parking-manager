
/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package nl.tue.iot.reservation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.request.exception.RequestFailedException;
import org.eclipse.leshan.core.request.exception.ResourceAccessException;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.standalone.servlet.json.ClientSerializer;
import org.eclipse.leshan.standalone.servlet.json.LwM2mNodeDeserializer;
import org.eclipse.leshan.standalone.servlet.json.LwM2mNodeSerializer;
import org.eclipse.leshan.standalone.servlet.json.ResponseSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import nl.tue.iot.reservation.model.ReservationDao;

/**
 * Service HTTP REST API calls.
 */
public class ReservationServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ReservationServlet.class);

    private static final long TIMEOUT = 5000; // ms

    private static final long serialVersionUID = 1L;

    private final LwM2mServer server;

    private final Gson gson;
    // private List<ReservationObject> reservationList = new ArrayList<ReservationObject>();

    public ReservationServlet(LwM2mServer server, int securePort) {
        this.server = server;

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(Client.class, new ClientSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mResponse.class, new ResponseSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeDeserializer());
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.gson = gsonBuilder.create();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // all registered clients
        if (req.getPathInfo() == null) {
        	
            Collection<Client> clients = server.getClientRegistry().allClients();
            List<ParkingClientObject> reservationList = new ArrayList<ParkingClientObject>();            
            for (Client client : clients) {
            	ParkingClientObject pcObject = new ParkingClientObject();
                String endPoint = client.getEndpoint();

                System.out.println("endpoint :" + endPoint);
                
                String locationTarget = "/6";
                ReadRequest locationRequest = new ReadRequest(locationTarget);
                ReadResponse cLocationResponse = server.send(client, locationRequest, TIMEOUT);
                LwM2mObject locationNode = (LwM2mObject) cLocationResponse.getContent();
                LwM2mObjectInstance location = locationNode.getInstance(0);
                String latitude = (String) location.getResource(0).getValue();
                String longitude = (String) location.getResource(1).getValue();
                pcObject.setLatitude(latitude);
                pcObject.setLongitude(longitude);
                pcObject.setEndPoint(endPoint);

                String spotTarget = "/32700";
                ReadRequest spotRequest = new ReadRequest(spotTarget);
                ReadResponse cSpotResponse = server.send(client, spotRequest, TIMEOUT);
                LwM2mObject spotNode = (LwM2mObject) cSpotResponse.getContent();
                Map<Integer, LwM2mObjectInstance> spotInstances = spotNode.getInstances();
                for (Map.Entry<Integer, LwM2mObjectInstance> entry : spotInstances.entrySet()) {
                    String spotId = (String) entry.getValue().getResource(32800).getValue();
                    String spotState = (String) entry.getValue().getResource(32801).getValue();
                    String vehicleId = (String) entry.getValue().getResource(32802).getValue();
                    Double billingRate = (Double) entry.getValue().getResource(32803).getValue();
                    ParkingSpot ps = new ParkingSpot(spotId, spotState, vehicleId);
                    pcObject.getSpotList().add(ps);
                }
                reservationList.add(pcObject);          
            }
            String json = this.gson.toJson(reservationList);
            // String json = this.gson.toJson(clients.toArray(new Client[] {}));
            resp.setContentType("application/json");
            resp.getOutputStream().write(json.getBytes("UTF-8"));
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        String clientEndpoint = path[0];

        // at least /endpoint/objectId/instanceId
        if (path.length < 3) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
            return;
        }

        try {
            String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);
            Client client = server.getClientRegistry().get(clientEndpoint);
            if (client != null) {
                WriteResponse cResponse = this.writeRequest(client, target, req, resp);
                processDeviceResponse(req, resp, cResponse);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("No registered client with id '%s'", clientEndpoint).flush();
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid request", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().append(e.getMessage()).flush();
        } catch (ResourceAccessException | RequestFailedException e) {
            LOG.warn(String.format("Error accessing resource %s%s.", req.getServletPath(), req.getPathInfo()), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append(e.getMessage()).flush();
        }
    }

    private void processDeviceResponse(HttpServletRequest req, HttpServletResponse resp, LwM2mResponse cResponse)
            throws IOException {
        String response = null;
        if (cResponse == null) {
            LOG.warn(String.format("Request %s%s timed out.", req.getServletPath(), req.getPathInfo()));
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Request timeout").flush();
        } else {
            response = this.gson.toJson(cResponse);
            resp.setContentType("application/json");
            resp.getOutputStream().write(response.getBytes());
            resp.setStatus(HttpServletResponse.SC_OK);
        }

    }

    // TODO refactor the code to remove this method.
    private WriteResponse writeRequest(Client client, String target, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        Map<String, String> parameters = new HashMap<String, String>();
        String contentType = HttpFields.valueParameters(req.getContentType(), parameters);

        if ("text/plain".equals(contentType)) {
            String content = IOUtils.toString(req.getInputStream(), parameters.get("charset"));
            int rscId = Integer.valueOf(target.substring(target.lastIndexOf("/") + 1));
            WriteRequest writeRequest = new WriteRequest(Mode.REPLACE, ContentFormat.TEXT, target,
                    LwM2mSingleResource.newStringResource(rscId, content));
            return server.send(client, writeRequest, TIMEOUT);

        } else if ("application/json".equals(contentType)) {
            String content = IOUtils.toString(req.getInputStream(), parameters.get("charset"));
            JsonObject jsonObject = gson.fromJson(content, JsonObject.class);
            String vehicleId = jsonObject.get("value").getAsString();
            String action = jsonObject.get("action").getAsString();

            // hack to reserve parking spot--------------------------
            String content2 = "";
            String target2 = "";
            String contentColor = "";
            String targetColor = "";
            if ("reserve".equalsIgnoreCase(action)) {
                content2 = "{'id':'32801','value':'reserved'}";
                int i = target.lastIndexOf('/');
                target2 = target.substring(0, i + 1) + "32801";
                contentColor = "{'id':'5527','value':'orange'}";
                targetColor = "/3341/0/5527";
            
	            LwM2mNode node2 = null;
	            LwM2mNode nodeColor = null;
	            try {
	                node2 = gson.fromJson(content2, LwM2mNode.class);
	                nodeColor = gson.fromJson(contentColor, LwM2mNode.class);
	            } catch (JsonSyntaxException e) {
	                throw new IllegalArgumentException("unable to parse json to tlv:" + e.getMessage(), e);
	            }
	
	            server.send(client, new WriteRequest(Mode.REPLACE, null, targetColor, nodeColor), TIMEOUT);
	            server.send(client, new WriteRequest(Mode.REPLACE, null, target2, node2), TIMEOUT);
	            
	            //get spot info
	        	String spotTarget = "/32700";
	            ReadRequest spotRequest = new ReadRequest(spotTarget);
	            ReadResponse cSpotResponse = server.send(client, spotRequest, TIMEOUT);
	            LwM2mObject spotNode = (LwM2mObject) cSpotResponse.getContent();
	            LwM2mObjectInstance spotInstance = spotNode.getInstance(0);
	            String spotId = (String) spotInstance.getResource(32800).getValue();
	            //String spotState = (String) spotInstance.getResource(32801).getValue();	          
	            Double billingRate = (Double) spotInstance.getResource(32803).getValue();
	            
	            //write reservation info to mongodb
	            ReservationDao.writeEventToDatabase(client.getEndpoint(),spotId,vehicleId,billingRate,action);
            }
            // hack ---------------------

            LwM2mNode node = null;
            try {
                node = gson.fromJson(content, LwM2mNode.class);
            } catch (JsonSyntaxException e) {
                throw new IllegalArgumentException("unable to parse json to tlv:" + e.getMessage(), e);
            }
            return server.send(client, new WriteRequest(Mode.REPLACE, null, target, node), TIMEOUT);

        } else {
            throw new IllegalArgumentException(
                    "content type " + req.getContentType() + " not supported for write requests");
        }
    }
    
    public void destroy(){
    	ReservationDao.closeConnection();
    }
            
}
