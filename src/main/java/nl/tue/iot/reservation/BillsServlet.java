package nl.tue.iot.reservation;
import org.eclipse.leshan.server.LwM2mServer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class BillsServlet extends HttpServlet {
    private final LwM2mServer server;

    public BillsServlet(LwM2mServer server) {
        this.server = server;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String parkingSpotId = req.getParameter("parkingSpotId");
        String date = req.getParameter("date"); // format: ISO-8601 yyyy-mm-dd
        String path;

        if (parkingSpotId == null) {
            path = "all-parking-spot-bills.json";
        } else {
            path = "parking-spot-bills.json";
        }


        byte[] sampleResponse = Files.readAllBytes(Paths.get(path));
        resp.setContentType("application/json");
        resp.getOutputStream().write(sampleResponse);
        resp.setStatus(HttpServletResponse.SC_OK);
        return;
    }
}
