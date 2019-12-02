package scorekeep;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;

import java.util.*;
import java.lang.Exception;

public class SessionFactory {
  private final SessionModel model = new SessionModel();
  private final Tracer tracer = OpenTelemetry.getTracerFactory().get(SessionFactory.class.getName());

  public SessionFactory(){
  }

  public Session newSession() {
    String id = Identifiers.random();
    Session session = new Session(id);
    model.saveSession(session);
    return session;
  }

  public Session getSession(String sessionId) throws SessionNotFoundException {
    return model.loadSession(sessionId);
  }

  public List<Session> getSessions() {
    Span getSessions = tracer.spanBuilder("GetSessions").startSpan();
    List<Session> sessions =  model.loadSessions();
    getSessions.end();
    return sessions;
  }
}