package io.accelerate.tracking.app.events;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import io.accelerate.tracking.app.Stoppable;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExternalEventServerThread implements Stoppable {
    private static final int PORT = 41375;
    private Server server;
    private List<ExternalEventListener> notifyListeners;
    private List<ExternalEventListener> stopListeners;

    public ExternalEventServerThread() {
        // Create the server
        QueuedThreadPool threadPool = new QueuedThreadPool(16, 1);
        threadPool.setName("ExEvent");
        server = new Server(threadPool);
        server.setStopTimeout(1000);

        // Add the http connector
        ServerConnector http = new ServerConnector(server);
        http.setHost("localhost");
        http.setPort(PORT);
        server.addConnector(http);

        // Prepare listeners
        notifyListeners = new ArrayList<>();
        stopListeners = new ArrayList<>();

        // Register the servlets
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping(new ServletHolder(new StatusServlet()),
                "/status");
        handler.addServletWithMapping(new ServletHolder(new PostEventServlet(notifyListeners)),
                "/notify");
        handler.addServletWithMapping(new ServletHolder(new PostEventServlet(stopListeners)),
                "/stop");
    }

    public void start() throws Exception {
        server.start();
    }

    @Override
    public boolean isAlive() {
        return server.isRunning();
    }

    @Override
    public void join() throws InterruptedException {
        server.join();
    }

    @Override
    public void signalStop() throws Exception {
        server.stop();
    }

    //~~~~~~~~~ The listeners

    public void addNotifyListener(ExternalEventListener externalEventListener) {
        notifyListeners.add(externalEventListener);
    }

    public void addStopListener(ExternalEventListener externalEventListener) {
        stopListeners.add(externalEventListener);
    }

    //~~~~~~~~~ The commands that are being handled

    private class StatusServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("OK");
        }
    }

    private class PostEventServlet extends HttpServlet {
        private List<ExternalEventListener> listeners;

        PostEventServlet(List<ExternalEventListener> listeners) {
            this.listeners = listeners;
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {
            String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

            try {
                for (ExternalEventListener externalEventListener : listeners) {
                    externalEventListener.onExternalEvent(body.trim());
                }
                resp.setContentType("text/plain");
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println("ACK");
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println(e.getMessage());
            }
        }

    }
}
